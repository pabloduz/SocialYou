package com.eccos.socialyou

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log

import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.firebase.client.DataSnapshot
import com.firebase.client.Firebase
import com.firebase.client.FirebaseError
import com.firebase.client.ValueEventListener
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQueryDataEventListener
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

class NearbyUsers : AppCompatActivity() {

    private var billingManager: BillingManager? = null
    private val persistActivity = 10
    var users: ArrayList<User>? = null
    private val myLocationPermissionRequest = 101

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nearby_users)

        billingManager= BillingManager(this@NearbyUsers)

        Firebase.setAndroidContext(this)

        setupNavigation()

        users = ArrayList()

        showSnackbar(R.string.nearby_user_message)

        createLocationRequest()
        startLocationUpdates()
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
        val ref = FirebaseDatabase.getInstance().getReference("/locations_")

//        Log.e(tag, "${myLocation.latitude} and ${myLocation.longitude}")

        val geoFire = GeoFire(ref)

        val geoQuery = geoFire.queryAtLocation(GeoLocation(myLocation.latitude, myLocation.longitude), 0.075)

        geoQuery.addGeoQueryDataEventListener(object : GeoQueryDataEventListener {

            override fun onDataEntered(dataSnapshot: com.google.firebase.database.DataSnapshot, location: GeoLocation) {
                val key = dataSnapshot.key

                showSpots(key)
            }


            override fun onDataExited(dataSnapshot: com.google.firebase.database.DataSnapshot) {
                // ...
            }

            override fun onDataMoved(dataSnapshot: com.google.firebase.database.DataSnapshot, location: GeoLocation) {
                // ...
            }


            override fun onDataChanged(dataSnapshot: com.google.firebase.database.DataSnapshot, location: GeoLocation) {

            }

            override fun onGeoQueryReady() {
                setupAdapter()
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

            val ref = myFirebaseRef.child("users").child(key)

            ref.addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    val key = dataSnapshot.key

                    Log.e(tag, key)

                    Log.e(tag, "Create card view for")

                    val data = dataSnapshot.value as Map<*, *>

                    val name = data["name"] as String
                    val profile = data["profile"] as String
                    val url = data["url"] as String

                    val spot = User(key, name, profile, url)

                    users!!.add(spot)

                    Log.e(tag, "User added")
                }

                override fun onCancelled(firebaseError: FirebaseError) {

                }

            })

        } catch (ex: Exception) {

            Log.e(tag, "FireBase exception: " + ex.message)
        }
    }


    private fun setupAdapter() {
        val handler = Handler()
        handler.postDelayed({
            val progressBar = findViewById<ProgressBar>(R.id.progressBar)
            progressBar.visibility = View.GONE

            //Do something after X ms
            if (users!!.isEmpty()) {
                Log.e(tag, "Empty")
                val spot = User("1", getString(R.string.no_user), "", "")

                users!!.add(spot)
            }

            var userAdapter = UserAdapter(this, users!!)

            var recyclerView = findViewById<RecyclerView>(R.id.rv)
            recyclerView.layoutManager = LinearLayoutManager(applicationContext)
            recyclerView.itemAnimator = DefaultItemAnimator()
            recyclerView.adapter = userAdapter
        }, 1250)
    }

    private fun setWindow() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    private fun setupNavigation() {
        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // DrawerLayout
        val drawerLayout = findViewById<DrawerLayout>(R.id.linearLayout)
        val actionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_drawer, R.string.close_drawer)
        actionBarDrawerToggle.syncState()
        drawerLayout.addDrawerListener(actionBarDrawerToggle)

        // NavigationView
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home_page -> home()
                R.id.my_events -> myEvents()
                R.id.add_event -> addEvent()
                R.id.buy_premium -> buyPremium()
                R.id.rate -> rate()
                R.id.share -> share()
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun share() {
        val appPackageName = packageName // package name of the app

        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val shareBody = "Being Social Made Easy!\nI recommend you this app so you will find what and who is next to you:\n\nhttps://play.google.com/store/apps/details?id=$appPackageName"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "SocialYou")
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, "Share via"))    }

    private fun rate() {
        val appPackageName = packageName // getPackageName() from Context or Activity object

        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
        } catch (ex: android.content.ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
        }
    }

    private fun buyPremium() {
        Log.e(tag, MainActivity.skuDetails.toString())
        billingManager!!.initiatePurchaseFlow(MainActivity.skuDetails)
    }


    private fun home() {
        val myIntent = Intent(this@NearbyUsers, MainActivity::class.java)
        startActivity(myIntent)
        finish()
    }


    private fun myEvents() {
        val myIntent = Intent(this@NearbyUsers, MyEvents::class.java)
        startActivity(myIntent)
        finish()
    }

    private fun addEvent() {
        val myIntent = Intent(this@NearbyUsers, AddEventForm::class.java)
        startActivityForResult(myIntent, persistActivity)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Check which request we're responding to
        if (requestCode == persistActivity) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                Log.e(tag, "result code ok called!")

            }else{
                Log.e(tag, "result code not ok called!")
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        setWindow()
    }

    public override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient?.let{ it.removeLocationUpdates(locationCallback)}
    }

    override fun onBackPressed() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.linearLayout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers()
        } else {
            persistMainActivity()
            super.onBackPressed()
        }
    }

    private fun persistMainActivity() {
        var intent= Intent()
        setResult(Activity.RESULT_OK, intent)
    }

    private fun showSnackbar(text: Int) {
        val contextView = findViewById<View>(android.R.id.content)

        Snackbar.make(contextView, text, Snackbar.LENGTH_LONG)
                .show()
    }

    companion object {
        private val tag = "NearbyUsers"
    }
}
