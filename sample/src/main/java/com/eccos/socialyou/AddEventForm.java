package com.eccos.socialyou;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class AddEventForm extends AppCompatActivity {
    private FusedLocationProviderClient fusedLocationClient;

    private static final int MY_WRITE_PERMISSION_REQUEST = 101;

    private static final int PICK_IMAGE_REQUEST = 1;

    private static final String TAG = "AddEventForm";

    private LocationCallback locationCallback;

    private Firebase myFirebaseRef;

    private LocationRequest locationRequestNew;

    private StorageReference storageRef;

    private Uri mImageUri;

    private EditText title; private EditText date; private EditText time; private EditText description;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);
        setContentView(R.layout.add_event_form);

        //Getting references to Firebase
        storageRef = FirebaseStorage.getInstance().getReference();

        title = findViewById(R.id.title); date = findViewById(R.id.date); time = findViewById(R.id.time); description = findViewById(R.id.description);

        date.setInputType(InputType.TYPE_NULL);

        startLocationUpdates();

        setDialogCalendar();

        final Button chooseImage = (Button) findViewById(R.id.choose_image);
        chooseImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_WRITE_PERMISSION_REQUEST);
            }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case MY_WRITE_PERMISSION_REQUEST:

                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission was granted do nothing and carry on
                    openFileChooser();
                }else{
                    showSnackbar(R.string.grant_storage);
                }

                break;
        }
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

    private boolean checkTextFields() {
        if(TextUtils.isEmpty(title.getText())) {
            /***   You can Toast a message here that the title is Empty **/
            title.setError("Title is required!");

        }else if (TextUtils.isEmpty(date.getText())) {
            date.setError("Date is required!");

        }else if (TextUtils.isEmpty(time.getText())) {
            time.setError("Time is required!");

        }else if (TextUtils.isEmpty(description.getText())) {
            description.setError("Description is required!");

        }else {
            return true;
        }

        return false;
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

                                Map mInformation = new HashMap();

                                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                                //Saving all data with FireBase
                                mInformation.put("title", title.getText().toString()); mInformation.put("date", date.getText().toString()); mInformation.put("time", time.getText().toString()); mInformation.put("description", description.getText().toString());
                                Firebase fb = myFirebaseRef.child("events").push();
                                fb.setValue(mInformation);

                                String key= fb.getKey();

                                //Creating a location node with GeoFire
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("/locations");
                                GeoFire geoFire = new GeoFire(ref);

                                geoFire.setLocation(key, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                                    @Override
                                    public void onComplete(String key, DatabaseError error) {
                                        if (error != null) {
                                            Log.e(TAG, "There was an error saving the location to GeoFire: " + error);
                                        } else {
                                            Log.e(TAG, "Location saved on server successfully!");
                                        }
                                    }
                                });

                                //Creating a node for event's attendees
                                myFirebaseRef.child("attendees").child(key).push().setValue(userId);

                                //Uploading the event image with FireBase Storage
                                storeImageFile(key);

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

    private void storeImageFile(String path) {
//        Uri file = mImageUri;
        StorageReference ref = storageRef.child(path);

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), mImageUri);

            Bitmap resized = Bitmap.createScaledBitmap(bitmap, 600, 800, true);

            Uri resizedUri = getImageUri(this, resized);

            ref.putFile(resizedUri)
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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG,100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage,
                "SocialYou - What and Who is Next to Me", "Find more about the app on the PlayStore.");
        return Uri.parse(path); }


    private void setDialogCalendar() {
        final Calendar myCalendar = Calendar.getInstance();

        date.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                new DatePickerDialog(AddEventForm.this, new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear,
                                          int dayOfMonth) {
                        myCalendar.set(Calendar.YEAR, year);
                        myCalendar.set(Calendar.MONTH, monthOfYear);
                        myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateLabel(myCalendar);
                    }

                }, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });


        date.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (date.hasFocus()) {
                    new DatePickerDialog(AddEventForm.this, new DatePickerDialog.OnDateSetListener() {

                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear,
                                              int dayOfMonth) {
                            myCalendar.set(Calendar.YEAR, year);
                            myCalendar.set(Calendar.MONTH, monthOfYear);
                            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                            updateLabel(myCalendar);
                        }

                    }, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();
                }
            }
        });

        time.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                new TimePickerDialog(AddEventForm.this,
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay,
                                                  int minute) {

                                String dateTime = hourOfDay + ":" + String.format(Locale.getDefault(), "%02d", minute);
                                time.setText(dateTime);
                            }


                        }, myCalendar.get(Calendar.HOUR_OF_DAY), myCalendar.get(Calendar.MINUTE), false).show();
            }
        });


        time.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (time.hasFocus()) {
                    new TimePickerDialog(AddEventForm.this,
                            new TimePickerDialog.OnTimeSetListener() {

                                @Override
                                public void onTimeSet(TimePicker view, int hourOfDay,
                                                      int minute) {

                                    String dateTime = hourOfDay + ":" + String.format(Locale.getDefault(), "%02d", minute);
                                    time.setText(dateTime);

                                }
                            }, myCalendar.get(Calendar.HOUR_OF_DAY), myCalendar.get(Calendar.MINUTE), false).show();
                }
            }
        });
    }


    private void updateLabel(Calendar myCalendar) {
        String myFormat = "MM/dd/yyyy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());


        date.setText(sdf.format(myCalendar.getTime()));
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


    private void showSnackbar(int text){
        View contextView = findViewById(android.R.id.content);

        Snackbar.make(contextView, text, Snackbar.LENGTH_LONG)
                .show();
    }
}
