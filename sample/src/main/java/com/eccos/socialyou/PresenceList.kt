package com.eccos.socialyou

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.client.DataSnapshot
import com.firebase.client.Firebase
import com.firebase.client.FirebaseError
import com.firebase.client.ValueEventListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import java.util.ArrayList

class PresenceList : AppCompatActivity() {

    var users: ArrayList<User>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.presence_list)

        Firebase.setAndroidContext(this)

        users = ArrayList()

        val key = intent.extras!!.getString("key")
        showSpots(key)
    }


    fun showSpots(key : String) {
        //Get the DataSnapshot key
        val myFirebaseRef = Firebase("https://socialyou-be6cf.firebaseio.com/")

        val ref = myFirebaseRef.child("attendees").child(key)

        ref.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (ds in dataSnapshot.children) {
                    val key = ds.key

                    Log.e(tag, "Key: $key")

                    Log.e(tag, "Create card view for")

                    val refEvent = myFirebaseRef.child("users").child(key)
                    refEvent.addListenerForSingleValueEvent(object : ValueEventListener {


                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val key = dataSnapshot.key

                            Log.e(tag, "Create card view for")

                            val data = dataSnapshot.value as Map<*, *>

                            val name = data["name"] as String
                            val profile = data["profile"] as String
                            val url = data["url"] as String

                            val spot = User(key, name, profile, url)

                            users!!.add(spot)

                            Log.e(tag, "User added")
                        }

                        override fun onCancelled(firebaseError: FirebaseError) {}
                    })
                }

                setupAdapter()
            }

            override fun onCancelled(firebaseError: FirebaseError) {}
        })
    }

    private fun setupAdapter() {
        val handler = Handler()
        handler.postDelayed({
            val progressBar = findViewById<ProgressBar>(R.id.progressBar)
            progressBar.visibility = View.GONE

            //Do something after X ms
            var userAdapter = UserAdapter(this, users)

            var recyclerView = findViewById<RecyclerView>(R.id.rv)
            recyclerView.layoutManager = LinearLayoutManager(applicationContext)
            recyclerView.itemAnimator = DefaultItemAnimator()
            recyclerView.adapter = userAdapter
        }, 2000)
    }

    private fun setWindow() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    override fun onResume() {
        super.onResume()

        setWindow()
    }


    private fun showSnackbar(text: Int) {
        val contextView = findViewById<View>(android.R.id.content)

        Snackbar.make(contextView, text, Snackbar.LENGTH_LONG)
                .show()
    }

    companion object {
        private var pref: SharedPreferences? = null
        private val tag = "NearbyUsers"
    }
}
