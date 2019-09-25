package com.eccos.socialyou


import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log

import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.firebase.client.DataSnapshot
import com.firebase.client.Firebase
import com.firebase.client.FirebaseError
import com.firebase.client.ValueEventListener
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage

import java.util.ArrayList


class MyEvents : AppCompatActivity() {

    private val persistActivity: Int = 101
    var spots: ArrayList<Spot>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.my_events)

        setupNavigation()

        spots = ArrayList()

        pref = PreferenceManager.getDefaultSharedPreferences(this)

        val myFirebaseRef = Firebase("https://socialyou-be6cf.firebaseio.com/")

        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        val ref = myFirebaseRef.child("users").child(userId).child("events")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (ds in dataSnapshot.children) {
                    val key = ds.key

                    Log.e(tag, "Create card view for")

                    val refEvent = myFirebaseRef.child("events").child(key)
                    refEvent.addListenerForSingleValueEvent(object : ValueEventListener {

                        override fun onDataChange(snap: DataSnapshot) {
                            val key = snap.key

                            val data = snap.value as Map<*, *>

                            val title = data["title"] as String?
                            val date = data["date"] as String?
                            val time = data["time"] as String?
                            val location = data["location"] as String?
                            val description = data["description"] as String?

                            val storageRef = FirebaseStorage.getInstance().reference

                            storageRef.child(key).downloadUrl.addOnSuccessListener { uri ->
                                // Got the download URL for 'users/me/profile.png'
                                val url = uri.toString()

                                val spot = Spot(1, key, title!!, date!!, time!!, location!!, description!!, url)

                                spots!!.add(spot)

                                Log.e(tag, "Spot added")
                            }.addOnFailureListener {
                                // Handle any errors
                            }
                        }

                        override fun onCancelled(firebaseError: FirebaseError) {}
                    })
                }

                setupAdapter()
            }

            override fun onCancelled(firebaseError: FirebaseError) {

            }
        })
    }

    private fun setupAdapter() {
        val handler = Handler()
        handler.postDelayed({
            val progressBar = findViewById<ProgressBar>(R.id.progressBar)
            progressBar.visibility = View.GONE

            //Do something after X ms
            if (spots!!.isEmpty()) {
                Log.e(tag, "Empty")
                val spot = Spot(1, "1", getString(R.string.no_event), "", "", "", "", "")

                spots!!.add(spot)
            }

            var spotAdapter = SpotAdapter(this, spots)

            var recyclerView = findViewById<RecyclerView>(R.id.rv)
            recyclerView.layoutManager = LinearLayoutManager(applicationContext)
            recyclerView.itemAnimator = DefaultItemAnimator()
            recyclerView.adapter = spotAdapter
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
                R.id.add_event -> addEvent()
                R.id.nearby_users -> nearbyUsers()

            }
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun home() {
        val myIntent = Intent(this@MyEvents, MainActivity::class.java)
        startActivity(myIntent)
        finish()
    }

    private fun nearbyUsers() {
        val myIntent = Intent(this@MyEvents, NearbyUsers::class.java)
        startActivity(myIntent)
        finish()
    }

    private fun addEvent() {
        val myIntent = Intent(this@MyEvents, AddEventForm::class.java)
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

    companion object {
        private var pref: SharedPreferences? = null
        private val tag = "MyEvents"
    }
}
