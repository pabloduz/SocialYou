package com.eccos.socialyou;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class SpotAdapter extends RecyclerView.Adapter<SpotAdapter.ViewHolder> {

    List<Spot> SpotList;
    Context context;

    public SpotAdapter(List<Spot>SpotList)
    {
        this.SpotList = SpotList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_event,parent,false);
        ViewHolder viewHolder = new ViewHolder(view);
        context = parent.getContext();
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        Spot item = SpotList.get(position);

        if(position == 0){
            holder.header.setText("Statistics for your questions");
            holder.header.setPadding(0,0,0,50);
        }


        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(context,"The position is:"+position,Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    public int getItemCount() {
        return 0;
    }


    public class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView header;
        TextView firstText;
        TextView secondText;
        CardView cardView;

        public ViewHolder(View itemView)
        {
            super(itemView);
            header =  itemView.findViewById(R.id.header);
            firstText = itemView.findViewById(R.id.firstText);
            secondText = itemView.findViewById(R.id.secondText);
            cardView = itemView.findViewById(R.id.cv);

            cardView.setRadius(15);

            cardView.setPadding(25, 25, 25, 25);

            cardView.setCardBackgroundColor(Color.RED);

            cardView.setMaxCardElevation(30);

            cardView.setMaxCardElevation(6);

            firstText.setTextColor(Color.WHITE);

            firstText.setPadding(25,25,25,25);

            secondText.setTextColor(Color.WHITE);

            secondText.setPadding(25,25,25,25);


        }

    }
}