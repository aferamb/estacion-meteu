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

public class AlarmTableAdapter extends RecyclerView.Adapter<AlarmTableAdapter.VH> {
    private List<Alarm> items = new ArrayList<>();

    public void setItems(List<Alarm> list) { items.clear(); if (list != null) items.addAll(list); notifyDataSetChanged(); }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alarm_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Alarm a = items.get(position);
        holder.colId.setText(a.getId() == null ? "" : String.valueOf(a.getId()));
        holder.colSensor.setText(a.getSensorId() == null ? "" : a.getSensorId());
        holder.colParam.setText(a.getParameter() == null ? "" : a.getParameter());
        holder.colValue.setText(a.getTriggeredValue() == null ? "" : String.valueOf(a.getTriggeredValue()));
        holder.colTriggered.setText(a.getTriggeredAt() == null ? "" : a.getTriggeredAt());
        holder.colResolved.setText(a.getResolvedAt() == null ? "" : a.getResolvedAt());
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView colId, colSensor, colParam, colValue, colTriggered, colResolved;
        VH(@NonNull View itemView) {
            super(itemView);
            colId = itemView.findViewById(R.id.col_id);
            colSensor = itemView.findViewById(R.id.col_sensor);
            colParam = itemView.findViewById(R.id.col_parameter);
            colValue = itemView.findViewById(R.id.col_value);
            colTriggered = itemView.findViewById(R.id.col_triggered);
            colResolved = itemView.findViewById(R.id.col_resolved);
        }
    }
}
