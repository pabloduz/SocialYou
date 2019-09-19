package com.eccos.socialyou;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

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

        Log.e("SpotAdapter", "Entrou");

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_event,parent,false);
        ViewHolder viewHolder = new ViewHolder(view);
        context = parent.getContext();
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        Spot item = spotList.get(position);

        Log.e("SpotAdapter", "Entrou");

        if(position == 0){
            holder.header.setText(R.string.my_events);
            holder.header.setPadding(0,0,0,50);
        }

        holder.title.setText(item.getTitle());
        holder.date.setText(item.getDate());


        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(context,"The position is:"+position,Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    public int getItemCount() {
        return spotList.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView header;
        TextView title;
        TextView date;
        CardView cardView;

        public ViewHolder(View itemView)
        {
            super(itemView);
            header =  itemView.findViewById(R.id.header);
            title = itemView.findViewById(R.id.title);
            date = itemView.findViewById(R.id.date);
            cardView = itemView.findViewById(R.id.cv);

            cardView.setRadius(15);

            cardView.setPadding(25, 25, 25, 25);

            cardView.setCardBackgroundColor(Color.BLACK);

            cardView.setMaxCardElevation(30);

            title.setTextColor(Color.WHITE);

            title.setPadding(25,25,25,25);

            date.setTextColor(Color.WHITE);

            date.setPadding(25,25,25,25);


        }

    }
}