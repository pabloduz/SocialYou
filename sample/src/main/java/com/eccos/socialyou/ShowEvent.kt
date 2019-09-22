package com.eccos.socialyou

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.snackbar.Snackbar

class ShowEvent : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.show_event)

        var mToolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(mToolbar)

        // Recieve data
        val key = intent.extras!!.getString("key")
        val title = intent.extras!!.getString("title")
        val date = intent.extras!!.getString("date")
        val time = intent.extras!!.getString("time")
        var location = intent.extras!!.getString("location")
        val description = intent.extras!!.getString("description")
        val url = intent.extras!!.getString("url")

        supportActionBar!!.title = title

        val vDate = findViewById<TextView>(R.id.aa_date)
        val vTime = findViewById<TextView>(R.id.aa_time)
        val vLocation = findViewById<TextView>(R.id.aa_location)
        val vDescription = findViewById<TextView>(R.id.aa_description)
        val vUrl = findViewById<ImageView>(R.id.aa_thumbnail)


        val length = location.length

        if (length > 35) {
            var shortLocation = location.substring(0, 32) + "..."
            vLocation.text= shortLocation
        }else{
            vLocation.text = location
        }


        // setting values to each view

        vDate.text = date
        vTime.text = time
        vDescription.text = description

        val requestOptions = RequestOptions().centerCrop().placeholder(R.drawable.loading_shape).error(R.drawable.loading_shape)


        // set image using Glide
        Glide.with(this).load(url).apply(requestOptions).into(vUrl)


        mToolbar.setOnClickListener{
            val contextView = this@ShowEvent.findViewById<View>(android.R.id.content)

            val snackBar= Snackbar.make(contextView, title, Snackbar.LENGTH_LONG)
            val snackBarView = snackBar.view

            val textView = snackBarView.findViewById(com.google.android.material.R.id.snackbar_text) as TextView
            textView.maxLines = 5

            snackBar.show()
        }


        vLocation.setOnClickListener{
            val contextView = this@ShowEvent.findViewById<View>(android.R.id.content)

            val snackBar= Snackbar.make(contextView, location, Snackbar.LENGTH_LONG)
            val snackBarView = snackBar.view

            val textView = snackBarView.findViewById(com.google.android.material.R.id.snackbar_text) as TextView
            textView.maxLines = 5

            snackBar.show()
        }
    }
}
