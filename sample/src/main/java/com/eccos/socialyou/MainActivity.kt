package com.eccos.socialyou

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.navigation.NavigationView
import com.eccos.socialyou.cardstackview.*
import com.firebase.client.Firebase
import com.firebase.client.FirebaseError
import com.firebase.client.ValueEventListener
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQueryDataEventListener
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.my_events.view.*
import java.util.*

class MainActivity : AppCompatActivity(), CardStackListener {

    private val drawerLayout by lazy { findViewById<DrawerLayout>(R.id.drawer_layout) }
    private val cardStackView by lazy { findViewById<CardStackView>(R.id.card_stack_view) }
    private val manager by lazy { CardStackLayoutManager(this, this) }
    private val adapter by lazy { CardStackAdapter(createSpots()) }
    private val myLocationPermissionRequest = 101
    private val eventCreated = 10
    private val tag = "MainActivity"
    private var firstSpot: Boolean = true

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest? = null

    private var spotsSwiped = ArrayList<String>()
    private var arrayList = ArrayList<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.setAndroidContext(this)
        setupSpotsSwiped()

        setContentView(R.layout.activity_main)
        setupNavigation()
        setupCardStackView()
        setupButton()
    }

    private fun setWindow() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }


    private fun setupSpotsSwiped() {
        var pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        //Start with a value to avoid null pointer exception when no events swiped
        arrayList.add("null")
        val list = Gson().toJson(arrayList)
        val json = pref.getString("spotsSwiped", list)

        val collectionType = object : TypeToken<ArrayList<String>>() {}.type
        spotsSwiped = Gson().fromJson(json, collectionType)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            myLocationPermissionRequest ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates()

                    startService(Intent(this, LocationService::class.java))
                } else {
                    Toast.makeText(this, R.string.grant_location, Toast.LENGTH_LONG).show()
                    finish()
                }
        }
    }

    private fun createLocationRequest() {
        var locationRequestLocal = LocationRequest.create()
        locationRequestLocal.smallestDisplacement = 5f
        locationRequestLocal.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequestLocal)

        locationRequest = locationRequestLocal

        val client = LocationServices.getSettingsClient(this)
        client.checkLocationSettings(builder.build())
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            var fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    if (locationResult == null) {
                        return
                    }

                    for (location in locationResult.locations) {
                        // Update UI with location data
                        val myLocation = LatLng(location.latitude, location.longitude)

                        Log.e(tag, "startLocationUpdate CALLED")

                        getSpots(myLocation)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback, null/* Looper */)

        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), myLocationPermissionRequest)
            }
        }
    }

    private fun getSpots(myLocation: LatLng) {
        val ref = FirebaseDatabase.getInstance().getReference("/locations")

//        Log.e(tag, "${myLocation.latitude} and ${myLocation.longitude}")

        val geoFire = GeoFire(ref)

        val geoQuery = geoFire.queryAtLocation(GeoLocation(myLocation.latitude, myLocation.longitude), 5.00)

        geoQuery.addGeoQueryDataEventListener(object : GeoQueryDataEventListener {

            override fun onDataEntered(dataSnapshot: DataSnapshot, location: GeoLocation) {
                val key = dataSnapshot.key

                if(!spotsSwiped.contains(key)) {
                    Log.e(tag, "Event found near you.")

                    if(firstSpot){
                        createProgressBar()
                        
                        firstSpot= false
                    }
                    
                    showSpots(key)
                }
            }

            override fun onDataExited(dataSnapshot: DataSnapshot) {
                // ...
            }

            override fun onDataMoved(dataSnapshot: DataSnapshot, location: GeoLocation) {
                // ...
            }


            override fun onDataChanged(dataSnapshot: DataSnapshot, location: GeoLocation) {

            }

            override fun onGeoQueryReady() {
                // ...
            }

            override fun onGeoQueryError(error: DatabaseError) {
                Log.e(tag, "$error")
            }

        })
    }

    fun showSpots(key: String?) {
        try {
            //Get the DataSnapshot key
            val myFirebaseRef = Firebase("https://socialyou-be6cf.firebaseio.com/")
            val ref = myFirebaseRef.child("events").child(key!!)

            ref.addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(dataSnapshot: com.firebase.client.DataSnapshot) {

                    val data = dataSnapshot.value as Map<*, *>

                    val title = data["title"] as String
                    val date = data["date"] as String
                    val time = data["time"] as String
                    val description = data["description"] as String


                    var storageRef = FirebaseStorage.getInstance().reference


                    storageRef.child(key).downloadUrl.addOnSuccessListener {
                        // Got the download URL
                        var url= it.toString()

                        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
                        progressBar.visibility = View.GONE


                        addLast(1, key, title, date, time, description, url)

                    }.addOnFailureListener {
                        // Handle any errors
                    }
                }

                override fun onCancelled(firebaseError: FirebaseError) {}
            })

        } catch (ex: Exception) {

            Log.e(tag, "FireBase exception: " + ex.message)
        }

    }

    private fun createProgressBar() {
        // Create progressBar dynamically...
        val progressBar = ProgressBar(this)
        progressBar.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        progressBar.setPadding(0,0,0,175)

        progressBar.id= R.id.progressBar
        val layout = findViewById<LinearLayout>(R.id.container)

        // Add ProgressBar to LinearLayout
        layout.gravity = Gravity.CENTER
        layout.addView(progressBar)
    }

    override fun onResume() {
        super.onResume()

        setWindow()

        createLocationRequest()
        startLocationUpdates()
    }

    public override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient?.let{ it.removeLocationUpdates(locationCallback)}
    }


    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCardDragging(direction: Direction, ratio: Float) {
        Log.d("CardStackView", "onCardDragging: d = ${direction.name}, r = $ratio")
    }

    override fun onCardSwiped(direction: Direction) {
        Log.d("CardStackView", "onCardSwiped: p = ${manager.topPosition}, d = $direction")

        val key= adapter.getSpotId(manager.topPosition - 1)

        var stringDirection= direction.toString()

        if(stringDirection.equals("Right")){

            insertAttendee(key)
        }

        //addSpotSwiped(key)

    }

    private fun insertAttendee(key: String) {
        try {
            val userId = FirebaseAuth.getInstance().currentUser!!.uid

            //Get the DataSnapshot key
            val myFirebaseRef = Firebase("https://socialyou-be6cf.firebaseio.com/")
            myFirebaseRef.child("attendees").child(key!!).child(userId).setValue(true)

        } catch (ex: Exception) {

            Log.e(tag, "FireBase exception: " + ex.message)
        }

    }


    private fun addSpotSwiped(key: String) {
        Log.d("CardStackView", "onCardSwiped: key = $key")
        spotsSwiped.remove("null")
        spotsSwiped.add(key)

        var pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val json = Gson().toJson(spotsSwiped)
        pref.edit().putString("spotsSwiped", json).commit()

    }



    override fun onCardRewound() {
        Log.d("CardStackView", "onCardRewound: ${manager.topPosition}")
    }

    override fun onCardCanceled() {
        Log.d("CardStackView", "onCardCanceled: ${manager.topPosition}")
    }

    override fun onCardAppeared(view: View, position: Int) {
        val textView = view.findViewById<TextView>(R.id.item_title)
        Log.d("CardStackView", "onCardAppeared: ($position) ${textView.text}")
    }

    override fun onCardDisappeared(view: View, position: Int) {
        val textView = view.findViewById<TextView>(R.id.item_title)
        Log.d("CardStackView", "onCardDisappeared: ($position) ${textView.text}")
    }

    private fun setupNavigation() {
        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // DrawerLayout
        val actionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_drawer, R.string.close_drawer)
        actionBarDrawerToggle.syncState()
        drawerLayout.addDrawerListener(actionBarDrawerToggle)

        // NavigationView
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home_page -> home()
                R.id.add_event -> addEvent()
                R.id.my_events -> myEvents()

            }
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun setupCardStackView() {
        initialize()
    }

    private fun setupButton() {
        val skip = findViewById<View>(R.id.skip_button)
        skip.setOnClickListener {
            val setting = SwipeAnimationSetting.Builder()
                    .setDirection(Direction.Left)
                    .setDuration(Duration.Normal.duration)
                    .setInterpolator(AccelerateInterpolator())
                    .build()
            manager.setSwipeAnimationSetting(setting)
            cardStackView.swipe()
        }

        val rewind = findViewById<View>(R.id.rewind_button)
        rewind.setOnClickListener {
            val setting = RewindAnimationSetting.Builder()
                    .setDirection(Direction.Bottom)
                    .setDuration(Duration.Normal.duration)
                    .setInterpolator(DecelerateInterpolator())
                    .build()
            manager.setRewindAnimationSetting(setting)
            cardStackView.rewind()
        }

        val like = findViewById<View>(R.id.like_button)
        like.setOnClickListener {
            val setting = SwipeAnimationSetting.Builder()
                    .setDirection(Direction.Right)
                    .setDuration(Duration.Normal.duration)
                    .setInterpolator(AccelerateInterpolator())
                    .build()
            manager.setSwipeAnimationSetting(setting)
            cardStackView.swipe()
        }
    }

    private fun initialize() {
        manager.setStackFrom(StackFrom.None)
        manager.setVisibleCount(3)
        manager.setTranslationInterval(8.0f)
        manager.setScaleInterval(0.95f)
        manager.setSwipeThreshold(0.3f)
        manager.setMaxDegree(20.0f)
        manager.setDirections(Direction.HORIZONTAL)
        manager.setCanScrollHorizontal(true)
        manager.setCanScrollVertical(true)
        manager.setSwipeableMethod(SwipeableMethod.AutomaticAndManual)
        manager.setOverlayInterpolator(LinearInterpolator())
        cardStackView.layoutManager = manager
        cardStackView.adapter = adapter
        cardStackView.itemAnimator.apply {
            if (this is DefaultItemAnimator) {
                supportsChangeAnimations = false
            }
        }
    }

    private fun home() {

    }

    private fun addEvent() {
        val myIntent = Intent(this@MainActivity, AddEventForm::class.java)
        startActivityForResult(myIntent, eventCreated)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Check which request we're responding to
        if (requestCode == eventCreated) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                Log.e(tag, "onActivityResult called!")

                finish()
            }
        }
    }

    private fun myEvents() {
        val myIntent = Intent(this@MainActivity, MyEvents::class.java)
        startActivity(myIntent)
        finish()
    }

    private fun addLast(size: Int, key: String, title: String, date: String, time: String, description: String, url: String) {
        val old = adapter.getSpots()
        val new = mutableListOf<Spot>().apply {
            addAll(old)
            addAll(List(size) { createSpot(key, title, date, time, description, url) })
        }
        val callback = SpotDiffCallback(old, new)
        val result = DiffUtil.calculateDiff(callback)
        adapter.setSpots(new)
        result.dispatchUpdatesTo(adapter)
    }

    private fun createSpot(key: String, title: String, date: String, time: String, description: String, url: String): Spot {
        return Spot(
                key= key,
                title = title,
                date = date,
                time = time,
                description = description,
                url = url
        )
    }

    private fun createSpots(): List<Spot> {
        val spots = ArrayList<Spot>()
        return spots
    }
}