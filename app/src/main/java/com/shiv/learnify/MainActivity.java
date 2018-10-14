package com.shiv.learnify;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.qhutch.bottomsheetlayout.BottomSheetLayout;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback
{

    public ArrayList<Marker> markersList;
    Student currentStudent;
    //bottom sheet fields
    ImageView profilePic;
    TextView studentName;
    TextView courseName;
    TextView beaconTitle;
    TextView descriptionText;
    boolean firstRefresh = true;
    private GoogleMap map;
    private double latitude;
    private double longitude;
    private ImageButton currentLocationButton;
    private BottomSheetLayout bottomSheet;
    private Switch beaconSwitch;
    private ConstraintLayout beaconLayout;
    private TextView beaconStatus;
    private ArrayList<Beacon> beaconsList;
    private Marker currentMarker;
    private String beaconKey;
    private String uid;
    private String currentBeaconCourse;
    private ListView coursesList;
    private Button addCourseButton;
    private ImageView courseToggle;
    private boolean coursesExpanded = false;
    private TextInputEditText searchInput;
    private String selectedBeaconCourse; // course which is selected from the course list of the student
    // which is used to filter the beacons for the map

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        currentLocationButton = findViewById(R.id.currentLocation);
        bottomSheet = findViewById(R.id.bottomSheetLayout);
        beaconSwitch = findViewById(R.id.beaconSwitch);
        beaconLayout = findViewById(R.id.beaconLayout);
        beaconStatus = findViewById(R.id.beaconStatus);
        coursesList = findViewById(R.id.coursesList);
        addCourseButton = findViewById(R.id.addCourseButton);
        courseToggle = findViewById(R.id.courseToggleProfilePic);
        searchInput = findViewById(R.id.searchInput);
        markersList = new ArrayList<>();
        beaconsList = new ArrayList<>();

        uid = getIntent().getExtras().getString("uid");

        addCourseButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(!searchInput.getText().toString().equals(""))
                {
                    if(!currentStudent.courses.contains(searchInput.getText().toString()))
                    {
                        currentStudent.courses.add(searchInput.getText().toString());
                        coursesList.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1,
                                android.R.id.text1, currentStudent.courses));
                        DatabaseReference dr = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);
                        dr.setValue(currentStudent);
                        searchInput.setText("");
                    }
                }
            }
        });

        bottomSheet.setVisibility(View.GONE);
        currentLocationButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                bottomSheet.collapse();
                setMarker();
            }
        });

        profilePic = findViewById(R.id.profilePic);
        studentName = findViewById(R.id.studentName);
        courseName = findViewById(R.id.courseName);
        beaconTitle = findViewById(R.id.beaconTitle);
        descriptionText = findViewById(R.id.descriptionText);


        courseToggle.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view)
            {
                if(coursesExpanded)
                {
                    courseListCollapse();
                }
                else
                {
                    courseListExpand();
                }
            }
        });

        //getting the student from database using the uid

        DatabaseReference studentReference = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);
        studentReference.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                currentStudent = dataSnapshot.getValue(Student.class);
                coursesList.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, android.R.id.text1, currentStudent.courses));
                System.out.println(currentStudent);
                StorageReference sr = FirebaseStorage.getInstance().getReference();
                sr.child("images/" + currentStudent.email).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Picasso.get()
                                .load(uri.toString())
                                .fit()
                                .centerCrop()
                                .into(courseToggle);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        profilePic.setImageResource(R.mipmap.ic_launcher_round);

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });



        beaconLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view)
            {
                beaconSwitch.toggle();
            }
        });

        beaconSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked)
            {
                if(isChecked)
                {

                    LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
                    View promptsView = layoutInflater.inflate(R.layout.course_values_input_dialog, null);
                    final TextInputEditText title = promptsView.findViewById(R.id.titleInput);
                    title.setHint("Title");
                    final TextInputEditText description = promptsView.findViewById(R.id.descriptionInput);
                    description.setHint("Description");
                    final Spinner spinner = promptsView.findViewById(R.id.courseSpinner);

                    spinner.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, currentStudent.courses));
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);

                    alertDialogBuilder
                            .setView(promptsView)
                            .setCancelable(false)
                            .setPositiveButton("OK",
                                    new DialogInterface.OnClickListener()
                                    {
                                        public void onClick(DialogInterface dialog, int id)
                                        {
                                            //Click ok
                                            beaconLayout.setBackground(getDrawable(R.drawable.beacon_stroke_green));
                                            beaconStatus.setText("On");

                                            closeBottomSheet();
                                            getLocation();

                                            currentBeaconCourse = spinner.getSelectedItem().toString();
                                            selectedBeaconCourse = null;    //because when we start a beacon, we should remove all filters

                                            String titleString = title.getText().toString();
                                            String descString = description.getText().toString();

                                            Beacon beacon = new Beacon(currentStudent, currentBeaconCourse,
                                                    new CustomLatLng(latitude, longitude), titleString, descString);

                                            beaconsList.add(beacon);
                                            LatLng place = new LatLng(latitude, longitude);
                                            currentMarker = map.addMarker(new MarkerOptions().position(place).title("Your Beacon"));
                                            markersList.add(currentMarker);

                                            DatabaseReference dr = FirebaseDatabase.getInstance().getReference();
                                            beaconKey = dr.push().getKey();

                                            dr.child("universities").child("michigan").child(beacon.course).child(beaconKey).setValue(beacon);
                                        }
                                    });
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
                else
                {
                    beaconLayout.setBackground(getDrawable(R.drawable.beacon_stroke_red));
                    beaconStatus.setText("Off");

                    DatabaseReference dr = FirebaseDatabase.getInstance().getReference();

                    dr.child("universities").child("michigan").child(currentBeaconCourse).child(beaconKey).removeValue();
                    currentMarker.remove();
                    markersList.remove(currentMarker);
                    currentMarker = null;
                    closeBottomSheet();
                }
            }
        });

        getLocation();

        //Database reference for listening to change in courses for particular uni only
        DatabaseReference courseReference = FirebaseDatabase.getInstance().getReference().child("universities")
                .child("michigan");

        ValueEventListener postListner = new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                Log.d("check", "shiv");
                beaconsList.clear();
                for(DataSnapshot snapshot : dataSnapshot.getChildren())
                {
                    for(DataSnapshot courseBeacon : snapshot.getChildren())
                    {
                        Beacon b = courseBeacon.getValue(Beacon.class);
                        beaconsList.add(b);
                    }
                }


                showBeacons();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        };


        coursesList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                selectedBeaconCourse = currentStudent.courses.get(i);
                searchInput.setText(selectedBeaconCourse);
                courseListCollapse();
                showBeacons();
            }
        });

        coursesList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                showConfirmationPrompt("Confirm?",
                        "Do you really want to delete " + currentStudent.courses.get(i) +
                                " from your courses?", true, i);
                return false;
            }
        });

        //linking the listener to reference
        courseReference.addValueEventListener(postListner);
    }

    /**
     * Shows a yes/no prompt based on preferences
     *
     * @param title,       dialog title
     * @param description, dialog description
     * @param cancelable,  doh
     */
    public void showConfirmationPrompt(String title, String description, boolean cancelable, final int i)
    {
        //Creating alert dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        if(title != null)
        {
            alertDialogBuilder.setTitle(title);
        }
        alertDialogBuilder.setMessage(description);
        alertDialogBuilder.setCancelable(cancelable);
        alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                currentStudent.courses.remove(i);
                DatabaseReference dr = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);
                dr.setValue(currentStudent);
                coursesList.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, android.R.id.text1, currentStudent.courses));

            }
        });
        alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int id)
            {

            }
        });
        if(cancelable)
        {
            alertDialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener()
            {
                @Override
                public void onCancel(DialogInterface dialogInterface)
                {
                }
            });
        }
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    void courseListExpand()
    {
        coursesList.setVisibility(View.VISIBLE);
        addCourseButton.setVisibility(View.VISIBLE);
        searchInput.setHint("Add new Course");
        coursesExpanded = true;
    }

    void courseListCollapse()
    {
        coursesList.setVisibility(View.GONE);
        addCourseButton.setVisibility(View.GONE);
        searchInput.setHint("Search for Courses");
        coursesExpanded = false;
    }

    /**
     * show beacons method
     * called everytime when there is a change in list of beacons in firebase
     */
    private void showBeacons()
    {
        for(int i = 0; i < markersList.size(); i++)
        {
            markersList.get(i).remove();
        }
        markersList.clear();
        if(selectedBeaconCourse == null)
        {
            for(int i = 0; i < beaconsList.size(); i++)
            {
                LatLng latLng = new LatLng(beaconsList.get(i).location.latitude, beaconsList.get(i).location.longitude);
                markersList.add(map.addMarker(new MarkerOptions().position(latLng).title(beaconsList.get(i).course)));
            }
        }
        else
        {
            for(int i = 0; i < beaconsList.size(); i++)
            {
                if(selectedBeaconCourse.equals(beaconsList.get(i).course))
                {
                    LatLng latLng = new LatLng(beaconsList.get(i).location.latitude, beaconsList.get(i).location.longitude);
                    markersList.add(map.addMarker(new MarkerOptions().position(latLng).title(beaconsList.get(i).course)));
                }
            }
        }

    }

    /**
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        map = googleMap;
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener()
        {
            @Override
            public void onMapClick(LatLng latLng)
            {
                closeBottomSheet();

                System.out.println("map click");
            }
        });
        setMarker();

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener()
        {
            @Override
            public boolean onMarkerClick(Marker marker)
            {

                Beacon beacon = null;
                LatLng coor = marker.getPosition();
                for(int i = 0; i < beaconsList.size(); i++)
                {
                    if(coor.latitude == beaconsList.get(i).location.latitude && coor.longitude == beaconsList.get(i).location.longitude)
                    {
                        beacon = beaconsList.get(i);
                        break;
                    }

                }
                System.out.println(beacon.toString());

                populateBeaconBottomSheet(beacon);

                openBottomSheet();

                return false;
            }
        });
    }

    /**
     * Displays the beacon values in the bottom sheet when beacon is selected
     *
     * @param beacon, beacon selected
     */
    void populateBeaconBottomSheet(Beacon beacon) {
        StorageReference sr = FirebaseStorage.getInstance().getReference();
        sr.child("images/" + beacon.student.email).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Picasso.get()
                        .load(uri.toString())
                        .fit()
                        .centerCrop()
                        .into(profilePic);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                profilePic.setImageResource(R.mipmap.ic_launcher_round);

            }
        });
        //TODO: set photo in profile pic

        studentName.setText(beacon.student.name);
        courseName.setText(beacon.course);
        beaconTitle.setText(beacon.title);
        descriptionText.setText(beacon.description);
    }

    /**
     * Gets location of the user, taking care of all the permissions
     */
    void getLocation()
    {
        while(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
        while(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }

        LocationManager manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener()
        {
            @Override
            public void onStatusChanged(String s, int i, Bundle bundle)
            {

            }

            @Override
            public void onProviderEnabled(String s)
            {

            }

            @Override
            public void onProviderDisabled(String s)
            {

            }

            @Override
            public void onLocationChanged(Location location)
            {
                // Previously mock location is cleared.
                // getLastKnownLocation(LocationManager.GPS_PROVIDER); will not return mock location.

                latitude = location.getLatitude();
                longitude = location.getLongitude();

                if(firstRefresh)
                {
                    setMarker();
                    firstRefresh = false;
                }
            }

        };
        //TODO: REMOVE network provider for better emulator location, but include when installing in phone
//        manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    void closeBottomSheet()
    {
        bottomSheet.setVisibility(View.GONE);
        bottomSheet.animate().scaleY(0);
        bottomSheet.collapse();
    }

    void openBottomSheet()
    {
        bottomSheet.collapse();
        bottomSheet.animate().scaleY(1);
        bottomSheet.setVisibility(View.VISIBLE);
    }

    /**
     * OnClick for R.id.currentLocationButton button
     */
    public void setMarker()
    {
        try
        {
            LatLng place = new LatLng(latitude, longitude);
//            map.addMarker(new MarkerOptions().position(place).title("Current Location"));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(place, 15));
        }
        catch(NullPointerException e)
        {
            Log.i("MapView", "Map not ready yet");
        }
    }

    @Override
    public void onBackPressed()
    {
        if(bottomSheet.isExpended())
        {
            bottomSheet.collapse();
        }
        else if(coursesExpanded)
        {
            courseListCollapse();
        }
        else
        {
            //TODO: Implement proper back handling
            super.onBackPressed();
        }
    }
}
