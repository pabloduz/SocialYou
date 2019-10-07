package com.eccos.socialyou

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.firebase.client.Firebase
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import android.app.Activity




class SpotAdapter(private var mContext: Context, private var mData: List<Spot>) : RecyclerView.Adapter<SpotAdapter.ViewHolder>() {
    private var option: RequestOptions


    init {

        // Request option for Glide
        option = RequestOptions().centerCrop().placeholder(R.drawable.loading_shape).error(R.drawable.loading_shape)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        mContext = parent.context

        val view = LayoutInflater.from(mContext).inflate(R.layout.list_event, parent, false)
        val viewHolder = ViewHolder(view)

        // Request option for Glide
        option = RequestOptions().centerCrop().placeholder(R.drawable.loading_shape).error(R.drawable.loading_shape)
        viewHolder.view_container.setOnClickListener {
            val i = Intent(mContext, ShowEvent::class.java)
            i.putExtra("key", mData[viewHolder.adapterPosition].key)
            i.putExtra("title", mData[viewHolder.adapterPosition].title)
            i.putExtra("date", mData[viewHolder.adapterPosition].date)
            i.putExtra("time", mData[viewHolder.adapterPosition].time)
            i.putExtra("location", mData[viewHolder.adapterPosition].location)
            i.putExtra("description", mData[viewHolder.adapterPosition].description)
            i.putExtra("url", mData[viewHolder.adapterPosition].url)

            mContext.startActivity(i)
        }

        view.setOnLongClickListener{
//            Toast.makeText(mContext, "Long click detected", Toast.LENGTH_SHORT).show()
            Snackbar.make(view, R.string.confirm, Snackbar.LENGTH_LONG).setAction(R.string.ok) {
                //Remove meeting for the user

                val key= mData[viewHolder.adapterPosition].key

                val userId = FirebaseAuth.getInstance().currentUser!!.uid

                //Get the DataSnapshot key
                val myFirebaseRef = Firebase("https://socialyou-be6cf.firebaseio.com/")

                myFirebaseRef.child("attendees").child(key).child(userId).removeValue()
                myFirebaseRef.child("users").child(userId).child("events").child(key).removeValue()

                val myIntent = Intent(mContext, MyEvents::class.java)
                mContext.startActivity(myIntent)
                (mContext as Activity).finish()

            }.show()

            true
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (_, _, title, date, time, _, _, url) = mData[position]

        // String date= mContext.getString(R.string.date); String time= mContext.getString(R.string.time); String concat= date + ": " + item.getDate() + "  " + time + ": " + item.getTime();

        val length = title.length

        if (length > 60) {
            val shortTitle = title.substring(0, 57) + "..."
            holder.title.text = shortTitle
        } else {
            holder.title.text = title
        }

        holder.date.text = date
        holder.time.text = time
        Glide.with(mContext).load(url).apply(option).into(holder.img_thumbnail)
    }

    override fun getItemCount(): Int {
        return mData.size
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var title: TextView
        internal var date: TextView
        internal var time: TextView
        internal var location: TextView? = null
        internal var description: TextView? = null
        internal var img_thumbnail: ImageView
        internal var view_container: LinearLayout

        init {
            title = itemView.findViewById(R.id.title)
            date = itemView.findViewById(R.id.date)
            time = itemView.findViewById(R.id.time)
            img_thumbnail = itemView.findViewById(R.id.thumbnail)
            view_container = itemView.findViewById(R.id.container)

        }

    }
}