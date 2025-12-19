package com.meteuapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import com.meteuapp.models.Station;

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.VH> {
    private List<Station> items = new ArrayList<>();

    public void setItems(List<Station> list) {
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
        Station s = items.get(position);
        holder.tv1.setText(s.getSensorId() == null ? "-" : s.getSensorId());
        holder.tv2.setText(s.getLastSeen() == null ? "" : s.getLastSeen());
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv1, tv2;
        VH(@NonNull View itemView) { super(itemView); tv1 = itemView.findViewById(android.R.id.text1); tv2 = itemView.findViewById(android.R.id.text2); }
    }
}
