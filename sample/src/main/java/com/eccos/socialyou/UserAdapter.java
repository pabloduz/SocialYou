package com.eccos.socialyou;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;


public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    Context mContext;
    RequestOptions option;
    List<User> mData ;


    public UserAdapter(Context mContext, List<User> mData) {
        this.mContext = mContext;
        this.mData = mData;

        // Request option for Glide
        option = new RequestOptions().centerCrop().placeholder(R.drawable.loading_shape).error(R.drawable.loading_shape);

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        mContext = parent.getContext();

        View view = LayoutInflater.from(mContext).inflate(R.layout.list_user,parent,false);
        final ViewHolder viewHolder = new ViewHolder(view);

        // Request option for Glide
        option = new RequestOptions().centerCrop().placeholder(R.drawable.loading_shape).error(R.drawable.loading_shape);
        viewHolder.view_container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url= "http://"+ mData.get(viewHolder.getAdapterPosition()).getProfile();

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                mContext.startActivity(browserIntent);
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        User item = mData.get(position);

        // String date= mContext.getString(R.string.date); String time= mContext.getString(R.string.time); String concat= date + ": " + item.getDate() + "  " + time + ": " + item.getTime();

        String title= item.getName();
        int length= title.length();

        if(length > 60){
            String shortTitle= title.substring(0,57).concat("...");
            holder.name.setText(shortTitle);
        }else{
            holder.name.setText(title);
        }

        holder.profile.setText(item.getProfile());
        Glide.with(mContext).load(item.getUrl()).apply(option).into(holder.img_thumbnail);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView name;
        TextView profile;
        ImageView img_thumbnail;
        LinearLayout view_container;

        public ViewHolder(View itemView)
        {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            profile = itemView.findViewById(R.id.profile);
            img_thumbnail = itemView.findViewById(R.id.thumbnail);
            view_container = itemView.findViewById(R.id.container);

        }

    }
}