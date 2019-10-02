package com.eccos.socialyou

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions


class UserAdapter(private var mContext: Context, private var mData: ArrayList<User>) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {
    private var option: RequestOptions
    private var billingManager: BillingManager? = null


    init {
        // Request option for Glide
        option = RequestOptions().centerCrop().placeholder(R.drawable.loading_shape).error(R.drawable.loading_shape)
        billingManager= BillingManager(MainActivity.instance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        mContext = parent.context

        val view = LayoutInflater.from(mContext).inflate(R.layout.list_user, parent, false)
        val viewHolder = ViewHolder(view)

        // Request option for Glide
        option = RequestOptions().centerCrop().placeholder(R.drawable.loading_shape).error(R.drawable.loading_shape)
        viewHolder.view_container.setOnClickListener {
            val name = mData[viewHolder.adapterPosition].name

            var unlock= mContext.resources.getString(R.string.unlock)

            if (name == unlock){

                billingManager!!.initiatePurchaseFlow(MainActivity.skuDetails)

            } else {
                val url = "http://" + mData[viewHolder.adapterPosition].profile

                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                mContext.startActivity(browserIntent)
            }
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (_, title, profile, url) = mData[position]

        // String date= mContext.getString(R.string.date); String time= mContext.getString(R.string.time); String concat= date + ": " + item.getDate() + "  " + time + ": " + item.getTime();

        val length = title.length

        if (length > 60) {
            val shortTitle = title.substring(0, 57) + "..."
            holder.name.text = shortTitle
        } else {
            holder.name.text = title
        }

        holder.profile.text = profile
        Glide.with(mContext).load(url).apply(option).into(holder.img_thumbnail)
    }

    override fun getItemCount(): Int {
        return mData.size
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var name: TextView
        internal var profile: TextView
        internal var img_thumbnail: ImageView
        internal var view_container: LinearLayout

        init {
            name = itemView.findViewById(R.id.name)
            profile = itemView.findViewById(R.id.profile)
            img_thumbnail = itemView.findViewById(R.id.thumbnail)
            view_container = itemView.findViewById(R.id.container)

        }

    }
}