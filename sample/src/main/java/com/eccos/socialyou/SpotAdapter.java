package com.eccos.socialyou;

import android.content.Context;
import android.graphics.Color;
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


public class SpotAdapter extends RecyclerView.Adapter<SpotAdapter.ViewHolder> {

    List<Spot> spotList;
    Context context;


    public SpotAdapter(List<Spot>spList)
    {
        this.spotList = spList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {


        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_event,parent,false);
        ViewHolder viewHolder = new ViewHolder(view);

        context = parent.getContext();
        // Request option for Glide
        //option = new RequestOptions().centerCrop().placeholder(R.drawable.loading_shape).error(R.drawable.loading_shape); apply(option);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        Spot item = spotList.get(position);

        // String date= context.getString(R.string.date); String time= context.getString(R.string.time); String concat= date + ": " + item.getDate() + "  " + time + ": " + item.getTime();

        String title= item.getTitle();
        int length= title.length();


        if(length > 60){
            title= title.substring(0,57).concat("...");
        }

        holder.title.setText(title);
        holder.date.setText(item.getDate());
        holder.time.setText(item.getTime());
        Glide.with(context).load(item.getUrl()).into(holder.img_thumbnail);
    }

    @Override
    public int getItemCount() {
        return spotList.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView title;
        TextView date;
        TextView time;
        ImageView img_thumbnail;
        LinearLayout view_container;

        public ViewHolder(View itemView)
        {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            date = itemView.findViewById(R.id.date);
            time = itemView.findViewById(R.id.time);
            img_thumbnail = itemView.findViewById(R.id.thumbnail);
            view_container = itemView.findViewById(R.id.container);

        }

    }
}