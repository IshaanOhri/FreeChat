package com.ishaan.wifip2p;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private ArrayList<String> messages;
    private ArrayList<Integer> identifier;

    public RecyclerViewAdapter(ArrayList<String> messages, ArrayList<Integer> identifier) {
        this.messages = messages;
        this.identifier = identifier;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.individual_item,parent,false);
        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        if (identifier.get(position) == 0)
        {
            //Sent Message
            holder.receivedTextView.setVisibility(View.GONE);
            holder.sentTextView.setVisibility(View.VISIBLE);
            holder.sentTextView.setText(messages.get(position));
        }
        else if(identifier.get(position) == 1)
        {
            //Received Message
            holder.receivedTextView.setVisibility(View.VISIBLE);
            holder.sentTextView.setVisibility(View.GONE);
            holder.receivedTextView.setText(messages.get(position));
        }

    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView receivedTextView, sentTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            receivedTextView = itemView.findViewById(R.id.receivedTextView);
            sentTextView = itemView.findViewById(R.id.sentTextView);

        }
    }
}
