package com.eccos.socialyou;


import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Map;


public class MyEvents extends AppCompatActivity {
    private static SharedPreferences pref;
    private static final String tag = "MyEvents";

    RecyclerView recyclerView;
    SpotAdapter spotAdapter;
    ArrayList<Spot> spots;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_events);

        spots= new ArrayList<>();

        pref= PreferenceManager.getDefaultSharedPreferences(this);

        final Firebase myFirebaseRef = new Firebase("https://socialyou-be6cf.firebaseio.com/");

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        final Firebase ref = myFirebaseRef.child("users").child(userId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    String key = ds.getKey();

                    Log.e(tag, "Create card view for");

                    final Firebase refEvent = myFirebaseRef.child("events").child(key);
                    refEvent.addListenerForSingleValueEvent(new ValueEventListener() {

                        @Override
                        public void onDataChange(DataSnapshot snap) {
                            final String key= snap.getKey();

                            Map data = (Map) snap.getValue();

                            final String title= (String) data.get("title");
                            final String date= (String) data.get("date");
                            final String time= (String) data.get("time");
                            final String description= (String) data.get("description");

                            StorageReference storageRef = FirebaseStorage.getInstance().getReference();


                            storageRef.child(key).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    // Got the download URL for 'users/me/profile.png'
                                    String url = uri.toString();

                                    Spot spot = new Spot(1, key, title, date, time, description, url);

                                    spots.add(spot);

                                    Log.e(tag, "Spot added");
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle any errors
                                }
                            });
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {
                        }
                    });
                }



                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ProgressBar progressBar= findViewById(R.id.progressBar);
                        progressBar.setVisibility(View.GONE);

                        //Do something after X ms
                        if (spots.isEmpty()) {
                            Log.e(tag, "Empty");
                            Spot spot = new Spot(1, "1", getString(R.string.no_event), "", "", "", "");

                            spots.add(spot);
                        }

                        spotAdapter = new SpotAdapter(spots);

                        recyclerView = findViewById(R.id.rv);
                        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                        recyclerView.setItemAnimator(new DefaultItemAnimator());
                        recyclerView.setAdapter(spotAdapter);

                }
                }, 1000);

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }

        });

        // Toasts the test ad message on the screen. Remove this after defining your own ad unit ID.
        // Toast.makeText(this, TOAST_TEXT, Toast.LENGTH_LONG).show();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
