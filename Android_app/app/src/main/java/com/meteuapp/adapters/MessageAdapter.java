package com.meteuapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.meteuapp.R;

import java.util.ArrayList;
import java.util.List;

import com.meteuapp.models.SensorMessage;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {

    private List<SensorMessage> items = new ArrayList<>();

    public void setItems(List<SensorMessage> list) {
        items.clear(); if (list != null) items.addAll(list); notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        SensorMessage m = items.get(position);
        String title = m.getSensorIdSafe();
        StringBuilder subtitle = new StringBuilder();
        subtitle.append(m.getRecordedAtSafe());
        if (!Double.isNaN(m.getTempSafe())) subtitle.append(" t:").append(m.getTemp());
        holder.tv1.setText(title);
        holder.tv2.setText(subtitle.toString());
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv1, tv2;
        VH(@NonNull View itemView) {
            super(itemView);
            tv1 = itemView.findViewById(android.R.id.text1);
            tv2 = itemView.findViewById(android.R.id.text2);
        }
    }
}
