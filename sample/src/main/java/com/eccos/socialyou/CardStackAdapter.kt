package com.eccos.socialyou

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar

class CardStackAdapter(
        private var spots: List<Spot> = emptyList()) : RecyclerView.Adapter<CardStackAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(inflater.inflate(R.layout.item_spot, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val spot = spots[position]


        holder.title.text = spot.title
        holder.date.text = spot.date
        holder.time.text = spot.time

        Glide.with(holder.image)
                .load(spot.url)
                .into(holder.image)
        holder.itemView.setOnClickListener { v ->
            var snackBar= Snackbar.make(v, spot.description, Snackbar.LENGTH_LONG)

            val snackBarView = snackBar.view

            val textView = snackBarView.findViewById(com.google.android.material.R.id.snackbar_text) as TextView
            textView.maxLines = 38

            snackBar.show()
        }
    }

    override fun getItemCount(): Int {
        return spots.size
    }

    fun getSpotId(position : Int): String {
        val spot = spots[position]
        return spot.key
    }

    fun setSpots(spots: List<Spot>) {
        this.spots = spots
    }

    fun getSpots(): List<Spot> {
        return spots
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.item_title)
        var date: TextView = view.findViewById(R.id.item_date)
        var time: TextView = view.findViewById(R.id.item_time)
        var image: ImageView = view.findViewById(R.id.item_image)
    }

}
