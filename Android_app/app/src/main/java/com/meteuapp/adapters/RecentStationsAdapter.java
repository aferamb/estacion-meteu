package com.meteuapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.meteuapp.R;
import com.meteuapp.models.Station;

import java.util.ArrayList;
import java.util.List;

public class RecentStationsAdapter extends RecyclerView.Adapter<RecentStationsAdapter.VH> {

    private List<Station> items = new ArrayList<>();

    public void setItems(List<Station> list) { items.clear(); if (list != null) items.addAll(list); notifyDataSetChanged(); }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stations_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Station s = items.get(position);
        holder.colLeader.setText(s.getSensorId());
        holder.colLastSeen.setText(s.getLastSeen());
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView colLeader, colLastSeen;
        VH(@NonNull View itemView) {
            super(itemView);
            colLeader = itemView.findViewById(R.id.col_station_leader);
            colLastSeen = itemView.findViewById(R.id.col_last_seen);
        }
    }
}
