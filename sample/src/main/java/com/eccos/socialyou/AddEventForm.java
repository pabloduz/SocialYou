package com.eccos.socialyou;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.firebase.client.Firebase;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.Map;

import static android.os.SystemClock.sleep;

public class AddEventForm extends AppCompatActivity {
    private FusedLocationProviderClient fusedLocationClient;

    private static final String TAG = "AddNetForm";

    private LocationCallback locationCallback;

    private Firebase myFirebaseRef;

    private LocationRequest locationRequestNew;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);
        setContentView(R.layout.add_event_form);

        final EditText title = (EditText) findViewById(R.id.title);
        final EditText date = (EditText) findViewById(R.id.date);
        final EditText description = (EditText) findViewById(R.id.description);

        startLocationUpdates();

        final Button button = (Button) findViewById(R.id.send_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if( TextUtils.isEmpty(title.getText())) {
                    /**
                     *   You can Toast a message here that the title is Empty
                     **/

                    title.setError("Title is required!");

                }else if (TextUtils.isEmpty(description.getText())) {
                    description.setError("Description is required!");

                }else if (TextUtils.isEmpty(date.getText())) {
                    date.setError("Date is required!");

                }else {
                    try {
                        fusedLocationClient.getLastLocation()
                                .addOnSuccessListener(AddEventForm.this, new OnSuccessListener<Location>() {
                                    @Override
                                    public void onSuccess(final Location location) {
                                        // Got last known location. In some rare situations this can be null.

                                        if (location != null) {
                                            //final LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());

                                            Log.e("Cord", "Latitude: " + location.getLatitude() + " Longitude: " + location.getLongitude());

                                            //Getting reference to Firebase
                                            myFirebaseRef = new Firebase("https://socialyou-be6cf.firebaseio.com/");

                                            //Saving all info into the Firebase
                                            Map mInformation = new HashMap();

                                            String id = myFirebaseRef.getAuth().getUid();
                                            mInformation.put("user", id);

                                            mInformation.put("title", title.getText().toString());
                                            mInformation.put("description", description.getText().toString());
                                            mInformation.put("price", date.getText().toString());

                                            Firebase fb = myFirebaseRef.child("networks").push();
                                            fb.setValue(mInformation);

                                            String key= fb.getKey();

                                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("/locations");
                                            GeoFire geoFire = new GeoFire(ref);

                                            geoFire.setLocation(key, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                                                @Override
                                                public void onComplete(String key, DatabaseError error) {
                                                    if (error != null) {
                                                        System.err.println("There was an error saving the location to GeoFire: " + error);
                                                    } else {
                                                        System.out.println("Location saved on server successfully!");
                                                    }
                                                }
                                            });


//                                            sleep(150);
//
//                                            Intent myIntent = new Intent(AddNetForm.this, MainActivity.class);
//                                            startActivity(myIntent);

                                        } else {
                                            Log.e(TAG, "Location is null.");

                                            View contextView = AddEventForm.this.findViewById(android.R.id.content);

                                            Snackbar.make(contextView, R.string.location_null, Snackbar.LENGTH_LONG)
                                                    .show();
                                        }
                                    }
                                });

                    } catch (SecurityException ex) {
                        Log.e(TAG, "Requires location permission.");
                    }
                }
            }
        });
    }



    public void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(AddEventForm.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(AddEventForm.this);

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }
                    for (Location location : locationResult.getLocations()) {
                        // Update UI with location data

                    }
                }

                ;
            };

            locationRequestNew = LocationRequest.create();

            locationRequestNew.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequestNew.setFastestInterval(2500);
            locationRequestNew.setInterval(5000);


            fusedLocationClient.requestLocationUpdates(locationRequestNew,
                    locationCallback,
                    null /* Looper */);

        }
    }

    /**
     * Called after the start and in between pauses and running.
     */
    @Override
    public void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    /**
     * Executed when the process is running on the background.
     */
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
