package com.meteuapp.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.meteuapp.models.Range;
import com.meteuapp.R;

import java.util.ArrayList;
import java.util.List;

public class RangeAdapter extends RecyclerView.Adapter<RangeAdapter.VH> {
    private List<Range> items = new ArrayList<>();
    private OnItemClick click;
    private int selected = -1;

    public interface OnItemClick { void onClick(int position, Range r); }
    public void setOnItemClick(OnItemClick c) { this.click = c; }

    public void setItems(List<Range> list) { items.clear(); if (list != null) items.addAll(list); notifyDataSetChanged(); }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_range, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Range r = items.get(position);
        holder.tvParam.setText(r.getParameter() == null ? "" : r.getParameter());
        holder.tvMin.setText(r.getMin() == null ? "" : String.valueOf(r.getMin()));
        holder.tvMax.setText(r.getMax() == null ? "" : String.valueOf(r.getMax()));
        holder.itemView.setOnClickListener(v -> {
            int old = selected;
            selected = position;
            notifyItemChanged(old);
            notifyItemChanged(selected);
            if (click != null) click.onClick(position, r);
        });
        // visual selected state using adaptive color resource
        int color = android.graphics.Color.TRANSPARENT;
        if (position == selected) {
            try {
                color = androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), com.meteuapp.R.color.selected_item_bg);
            } catch (Exception ignored) {}
        }
        holder.itemView.setBackgroundColor(color);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvParam, tvMin, tvMax;
        VH(@NonNull View itemView) {
            super(itemView);
            tvParam = itemView.findViewById(R.id.tv_parameter);
            tvMin = itemView.findViewById(R.id.tv_min);
            tvMax = itemView.findViewById(R.id.tv_max);
        }
    }
}
