package com.meteuapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SimpleStringAdapter extends RecyclerView.Adapter<SimpleStringAdapter.VH> {
    private List<String> items = new ArrayList<>();
    private OnItemLongClick longClick;
    private OnItemClick click;
    private int selected = -1;

    public interface OnItemLongClick { void onLongClick(int position, String value); }
    public interface OnItemClick { void onClick(int position, String value); }

    public void setOnItemLongClick(OnItemLongClick l) { this.longClick = l; }
    public void setOnItemClick(OnItemClick c) { this.click = c; }

    public void setItems(List<String> list) { items.clear(); if (list != null) items.addAll(list); notifyDataSetChanged(); }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String s = items.get(position);
        holder.tv.setText(s == null ? "" : s);
        holder.itemView.setOnLongClickListener(v -> {
            if (longClick != null) longClick.onLongClick(position, s);
            return true;
        });
        holder.itemView.setOnClickListener(v -> {
            int old = selected;
            selected = position;
            notifyItemChanged(old);
            notifyItemChanged(selected);
            if (click != null) click.onClick(position, s);
        });
        // visual selected state
        int bg = android.graphics.Color.TRANSPARENT;
        if (position == selected) {
            try { bg = androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), com.meteuapp.R.color.selected_item_bg); } catch (Exception ignored) {}
        }
        holder.itemView.setBackgroundColor(bg);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder { TextView tv; VH(@NonNull View itemView){ super(itemView); tv = itemView.findViewById(android.R.id.text1); } }
}
