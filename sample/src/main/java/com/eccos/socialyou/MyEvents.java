package com.eccos.socialyou;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
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

import static android.os.SystemClock.sleep;

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

        final Firebase ref = myFirebaseRef.child("attendees");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Map data = (Map) ds.getValue();

                    String key = ds.getKey();

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    Log.e(tag, "User: " + userId);


                    if(ds.child(userId).exists()){
                        Log.e(tag, "Create card view");

                        final Firebase refEvent = myFirebaseRef.child("events").child(key);
                        refEvent.addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(DataSnapshot snap) {
                                final String key= snap.getKey();

                                Log.e(tag, "Event:" + key);
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
                }


                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //Do something after X ms
                        if (spots.isEmpty()){
                            Log.e(tag, "Empty");
                            createCardView();


                        }else{
                            Log.e(tag, "Not empty");

                            spotAdapter = new SpotAdapter(spots);

                            recyclerView = findViewById(R.id.rv);
                            recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                            recyclerView.setItemAnimator(new DefaultItemAnimator());
                            recyclerView.setAdapter(spotAdapter);
                        }
                    }
                }, 1500);
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



    public void createCardView(){

        CardView cardview = new CardView(this);

        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.WRAP_CONTENT
        );


        cardview.setLayoutParams(layoutParams);

        cardview.setRadius(15);

        cardview.setPadding(25, 25, 25, 25);

        cardview.setCardBackgroundColor(Color.BLACK);

        cardview.setMaxCardElevation(30);

        TextView textview = new TextView(this);

        textview.setLayoutParams(layoutParams);

        textview.setText(R.string.no_event);

        textview.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);

        textview.setTextColor(Color.WHITE);

        textview.setPadding(25,25,25,25);

        textview.setGravity(Gravity.CENTER);

        cardview.addView(textview);

        setContentView(R.layout.list_empty);

        LinearLayout linearLayout = findViewById(R.id.lLayout);

        linearLayout.addView(cardview);

    }
}
