package com.meteuapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.meteuapp.R;
import com.meteuapp.models.Alarm;

import java.util.ArrayList;
import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.VH> {
    private List<Alarm> items = new ArrayList<>();

    public void setItems(List<Alarm> list) { items.clear(); if (list != null) items.addAll(list); notifyDataSetChanged(); }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Alarm a = items.get(position);
        holder.tv1.setText(a.getSensorId() + " | " + a.getParameter());
        holder.tv2.setText(a.getTriggeredAt() + " active:" + a.getActive());
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv1, tv2;
        VH(@NonNull View itemView) { super(itemView); tv1 = itemView.findViewById(android.R.id.text1); tv2 = itemView.findViewById(android.R.id.text2); }
    }
}
