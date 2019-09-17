package com.eccos.socialyou;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

import static android.os.SystemClock.sleep;

public class AddEventForm extends AppCompatActivity {
    private FusedLocationProviderClient fusedLocationClient;

    private static final int PICK_IMAGE_REQUEST = 1;

    private static final String TAG = "AddEventForm";

    private LocationCallback locationCallback;

    private Firebase myFirebaseRef;

    private LocationRequest locationRequestNew;

    private StorageReference storageRef;

    private Uri mImageUri;

    private EditText title; private EditText date; private EditText description;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);
        setContentView(R.layout.add_event_form);

        //Getting references to Firebase
        storageRef = FirebaseStorage.getInstance().getReference();

        title = findViewById(R.id.title);
        date = findViewById(R.id.date);
        description = findViewById(R.id.description);

        startLocationUpdates();


        final Button chooseImage = (Button) findViewById(R.id.choose_image);
        chooseImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openFileChooser();
            }
        });


        final Button button = (Button) findViewById(R.id.send_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (checkTextFields()) {

                    if (mImageUri != null) {
                        saveToFirebase();
                    } else {
                        showSnackbar(R.string.select_image);
                    }

                }
            }
        });
    }

    private void saveToFirebase() {
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

                                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                mInformation.put("user", userId);

                                String strTitle= title.getText().toString(); String strDate= date.getText().toString(); String strDescription= description.getText().toString();

                                mInformation.put("title", strTitle); mInformation.put("date", strDate); mInformation.put("description", strDescription);

                                String path= strDate + ":" + strTitle + ":" + userId;
                                storeImageFile(path);

                                Firebase fb = myFirebaseRef.child("events").push();
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


//                                sleep(150);
//
//                                Intent myIntent = new Intent(AddNetForm.this, MainActivity.class);
//                                startActivity(myIntent);

                            } else {
                                Log.e(TAG, "Location is null.");

                                View contextView = AddEventForm.this.findViewById(android.R.id.content);

                                Snackbar.make(contextView, R.string.location_null, Snackbar.LENGTH_LONG)
                                        .show();
                            }
                        }
                    });

        } catch (Exception ex) {
            Log.e(TAG, "Exception: " + ex);
        }
    }



    private boolean checkTextFields() {
        if(TextUtils.isEmpty(title.getText())) {
            /***   You can Toast a message here that the title is Empty **/
            title.setError("Title is required!");

        }else if (TextUtils.isEmpty(date.getText())) {
            date.setError("Date is required!");

        }else if (TextUtils.isEmpty(description.getText())) {
            description.setError("Description is required!");

        }else {
            return true;
        }

        return false;
    }


    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            mImageUri = data.getData();

            showSnackbar(R.string.image_selected);

        }
    }

    private void storeImageFile(String path) {
        Uri file = mImageUri;
        StorageReference riversRef = storageRef.child(path);

        riversRef.putFile(file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        Uri downloadUrl = taskSnapshot.getUploadSessionUri();
                        Log.e(TAG, "" + downloadUrl);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        // ...
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
                }
            };

            locationRequestNew = LocationRequest.create();

            locationRequestNew.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequestNew.setFastestInterval(2500);
            locationRequestNew.setInterval(10000);


            fusedLocationClient.requestLocationUpdates(locationRequestNew,
                    locationCallback,
                    null /* Looper */);
        }
    }

    private void showSnackbar(int text){
        View contextView = findViewById(android.R.id.content);

        Snackbar.make(contextView, text, Snackbar.LENGTH_LONG)
                .show();
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
        if(fusedLocationClient != null){
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
