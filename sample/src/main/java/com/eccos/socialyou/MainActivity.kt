package com.eccos.socialyou

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import com.android.billingclient.api.SkuDetails
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.navigation.NavigationView
import com.eccos.socialyou.cardstackview.*
import com.firebase.client.Firebase
import com.firebase.client.FirebaseError
import com.firebase.client.ValueEventListener
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQueryDataEventListener
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.Normalizer
import java.util.*

class MainActivity : AppCompatActivity(), CardStackListener {

    private var billingManager: BillingManager? = null
    private val drawerLayout by lazy { findViewById<DrawerLayout>(R.id.drawer_layout) }
    private val cardStackView by lazy { findViewById<CardStackView>(R.id.card_stack_view) }
    private val manager by lazy { CardStackLayoutManager(this, this) }
    private val adapter by lazy { CardStackAdapter(createSpots()) }
    private val myLocationPermissionRequest = 101
    private val persistActivity = 10
    private val tag = "MainActivity"
    private var firstSpot: Boolean = true

    private var example = "-LpcZ9nUG6ecI5RkyTpQ"
    private var context: Context? = null



    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest? = null

    private var spotsSwiped = ArrayList<String>()
    private var arrayList = ArrayList<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context= this

        billingManager= BillingManager(this@MainActivity)

        Firebase.setAndroidContext(this)
        setupSpotsSwiped()

        setContentView(R.layout.activity_main)
        setupNavigation()
        setupCardStackView()
        setupButton()

        showExampleSpot()
    }


    private fun showExampleSpot() {
        if(!spotsSwiped.contains(example)) {
            createProgressBar()

            try {
                //Get the DataSnapshot key
                val myFirebaseRef = Firebase("https://socialyou-be6cf.firebaseio.com/")
                val ref = myFirebaseRef.child("events").child(example)

                ref.addListenerForSingleValueEvent(object : ValueEventListener {

                    override fun onDataChange(dataSnapshot: com.firebase.client.DataSnapshot) {

                        val data = dataSnapshot.value as Map<*, *>

                        val title = data["title"] as String
                        val date = data["date"] as String
                        val time = data["time"] as String
                        val location = data["location"] as String
                        val description = data["description"] as String


                        var storageRef = FirebaseStorage.getInstance().reference


                        storageRef.child(example).downloadUrl.addOnSuccessListener {
                            // Got the download URL
                            var url = it.toString()

                            val progressBar = findViewById<ProgressBar>(R.id.progressBar)
                            progressBar.visibility = View.GONE

                            addLast(1, example, title, date, time, location, description, url)

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
    }

    private fun showPopup() {
        if(!spotsSwiped.contains(example)) {
            val userId = FirebaseAuth.getInstance().currentUser!!.uid

            val myFirebaseRef = Firebase("https://socialyou-be6cf.firebaseio.com/")
            val ref = myFirebaseRef.child("users").child(userId)

            ref.addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(dataSnapshot: com.firebase.client.DataSnapshot) {

                    val data = dataSnapshot.value as Map<*, *>

                    val name = data["name"] as String
                    val url = data["url"] as String

                    var myDialog = Dialog(context!!)
                    myDialog.setContentView(R.layout.custom_popup)

                    var vProfile= myDialog.findViewById<TextView>(R.id.profile)
                    var vImage= myDialog.findViewById<ImageView>(R.id.image)

                    //Converting to a standardized string
                    var nameUrl= name!!.replace(" ", "").toLowerCase()
                    nameUrl= stripAccents(nameUrl)
                    
                    vProfile.text = nameUrl

                    val requestOptions = RequestOptions().circleCrop().placeholder(R.drawable.circle)

                    Glide.with(context!!).load(url).apply(requestOptions).into(vImage)

                    var btnNext =  myDialog.findViewById<Button>(R.id.next)
                    btnNext.setOnClickListener {
                        //Getting reference to Firebase
                        var myFirebaseRef = Firebase("https://socialyou-be6cf.firebaseio.com/")

                        val userId = FirebaseAuth.getInstance().currentUser!!.uid

                        var url = "fb.com/${vProfile.text}"

                        myFirebaseRef!!.child("users").child(userId).child("profile").setValue(url)

                        myDialog.dismiss()
                    }

                    myDialog.setOnDismissListener {
                        setWindow()}

                    window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    myDialog.setCancelable(false)
                    myDialog.show()

                }

                override fun onCancelled(firebaseError: FirebaseError) {}
            })
        }
    }

    private fun stripAccents(s: String): String {
        var s = s
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
        s = s.replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
        return s
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

        pref.edit().putString("spotsSwiped", json).apply()

        val collectionType = object : TypeToken<ArrayList<String>>() {}.type
        spotsSwiped = Gson().fromJson(json, collectionType)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            myLocationPermissionRequest ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    showPopupWelcome()

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

                        Log.e(tag, "${myLocation.latitude} and ${myLocation.longitude}")

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
                    val location = data["location"] as String
                    val description = data["description"] as String


                    var storageRef = FirebaseStorage.getInstance().reference


                    storageRef.child(key).downloadUrl.addOnSuccessListener {
                        // Got the download URL
                        var url= it.toString()

                        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
                        progressBar.visibility = View.GONE

                        addLast(1, key, title, date, time, location, description, url)

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

        createLocationRequest()
        startLocationUpdates()

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

        showPopup()

        //It must be after showPopup()
        addSpotSwiped(key)


    }

    private fun insertAttendee(key: String) {
        try {
            val userId = FirebaseAuth.getInstance().currentUser!!.uid

            //Get the DataSnapshot key
            val myFirebaseRef = Firebase("https://socialyou-be6cf.firebaseio.com/")

            myFirebaseRef.child("attendees").child(key!!).child(userId).setValue(true)
            myFirebaseRef!!.child("users").child(userId).child("events").child(key).setValue(true)

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
        pref.edit().putString("spotsSwiped", json).apply()
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
                R.id.my_events -> myEvents()
                R.id.add_event -> addEvent()
                R.id.nearby_users -> nerbyUsers()
                R.id.buy_premium -> buyPremium()
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun buyPremium() {
        Log.e(tag, skuDetails.toString())
        billingManager!!.initiatePurchaseFlow(skuDetails)
    }


    private fun myEvents() {
        val myIntent = Intent(this@MainActivity, MyEvents::class.java)
        startActivityForResult(myIntent, persistActivity)
    }


    private fun addEvent() {
        val myIntent = Intent(this@MainActivity, AddEventForm::class.java)
        startActivityForResult(myIntent, persistActivity)
    }

    private fun nerbyUsers() {
        val myIntent = Intent(this@MainActivity, NearbyUsers::class.java)
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

    private fun addLast(size: Int, key: String, title: String, date: String, time: String, location: String, description: String, url: String) {
        val old = adapter.getSpots()
        val new = mutableListOf<Spot>().apply {
            addAll(old)
            addAll(List(size) { createSpot(key, title, date, time, location, description, url) })
        }
        val callback = SpotDiffCallback(old, new)
        val result = DiffUtil.calculateDiff(callback)
        adapter.setSpots(new)
        result.dispatchUpdatesTo(adapter)
    }

    private fun createSpot(key: String, title: String, date: String, time: String, location: String, description: String, url: String): Spot {
        return Spot(
                key= key,
                title = title,
                date = date,
                time = time,
                location = location,
                description = description,
                url = url
        )
    }

    private fun createSpots(): List<Spot> {
        return ArrayList()
    }

    companion object {
        var freeUser: Boolean = true
        var skuDetails: SkuDetails? = null
    }
}

//private fun showPopupWelcome() {
//    Log.e(tag, "Calling activity: $callingActivity")
//
//    if(callingActivity != null){
//        Log.e(tag, callingActivity.shortClassName)
//
//        if(callingActivity.shortClassName == ".AuthActivity"){
//            var myDialog = Dialog(this)
//            myDialog.setContentView(R.layout.custom_popup_welcome)
//
//            var layout =  myDialog.findViewById<LinearLayout>(R.id.layout)
//            layout.setOnClickListener {
//                //Getting reference to Firebase
//                myDialog.dismiss()
//            }
//            myDialog.setOnDismissListener {
//                setWindow()}
//
//            window.setBackgroundDrawableResource(android.R.color.white)
//            myDialog.show()
//        }
//    }
//}