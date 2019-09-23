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

import java.util.ArrayList


class NearbyUsers : AppCompatActivity() {

    private val eventCreated: Int = 101
    var users: ArrayList<User>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nearby_users)

        setupNavigation()

        users = ArrayList()

        pref = PreferenceManager.getDefaultSharedPreferences(this)

        val myFirebaseRef = Firebase("https://socialyou-be6cf.firebaseio.com/")

        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        val ref = myFirebaseRef.child("users").child(userId)

        ref.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val key = dataSnapshot.key

                Log.e(tag, "Create card view for")

                val data = dataSnapshot.value as Map<*, *>

                val name = data["name"] as String
                val email = data["email"] as String
                val url = data["url"] as String

                val spot = User(key, name, email, url)

                users!!.add(spot)

                Log.e(tag, "User added")


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
            if (users!!.isEmpty()) {
                Log.e(tag, "Empty")
                val spot = User("1", getString(R.string.no_user), "", "")

                users!!.add(spot)
            }

            var spotAdapter = UserAdapter(this, users)

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
                R.id.my_events -> myEvents()
                R.id.add_event -> addEvent()

            }
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun home() {
        //Close the waiting MainActivity
        var intent= Intent()
        setResult(Activity.RESULT_OK, intent)

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

    override fun onResume() {
        super.onResume()
        setWindow()
    }

    override fun onBackPressed() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.linearLayout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private var pref: SharedPreferences? = null
        private val tag = "MyEvents"
    }
}
