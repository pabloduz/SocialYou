package com.eccos.socialyou

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.appbar.CollapsingToolbarLayout

class ShowEvent : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.show_event)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Recieve data

        val key = intent.extras!!.getString("key")
        val title = intent.extras!!.getString("title")
        val date = intent.extras!!.getString("date")
        val time = intent.extras!!.getString("time")
        val location = intent.extras!!.getString("location")
        val description = intent.extras!!.getString("description")
        val url = intent.extras!!.getString("url")

        // ini views
        val collapsingToolbarLayout = findViewById<CollapsingToolbarLayout>(R.id.collapsingtoolbar_id)
        collapsingToolbarLayout.isTitleEnabled = true
        collapsingToolbarLayout.title= "Amazing Event"

        val vTitle = findViewById<TextView>(R.id.aa_title)
        val vDate = findViewById<TextView>(R.id.aa_date)
        val vTime = findViewById<TextView>(R.id.aa_time)
        val vDescription = findViewById<TextView>(R.id.aa_description)
        //        TextView vLocation  = findViewById(R.id.aa_location) ;
        val vUrl = findViewById<ImageView>(R.id.aa_thumbnail)

        // setting values to each view

        vTitle.text = title
        vDate.text = date
        vTime.text = time
        vDescription.text = description
        //        vLocation.setText(location);

        collapsingToolbarLayout.title = title


        val requestOptions = RequestOptions().centerCrop().placeholder(R.drawable.loading_shape).error(R.drawable.loading_shape)


        // set image using Glide
        Glide.with(this).load(url).apply(requestOptions).into(vUrl)
    }
}
