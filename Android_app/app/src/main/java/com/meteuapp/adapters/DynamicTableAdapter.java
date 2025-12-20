package com.meteuapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.meteuapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DynamicTableAdapter extends RecyclerView.Adapter<DynamicTableAdapter.VH> {

    private List<String> columns = new ArrayList<>();
    private List<Map<String, Object>> rows = new ArrayList<>();

    public void setColumns(List<String> cols) { this.columns.clear(); if (cols != null) this.columns.addAll(cols); notifyDataSetChanged(); }
    public void setRows(List<Map<String, Object>> r) { this.rows.clear(); if (r != null) this.rows.addAll(r); notifyDataSetChanged(); }
    public void addRow(Map<String,Object> row) { this.rows.add(0, row); notifyItemInserted(0); }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dynamic_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Map<String,Object> row = rows.get(position);
        holder.container.removeAllViews();
        for (String col : columns) {
            TextView tv = new TextView(holder.container.getContext());
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            Object v = row.get(col);
            tv.setText(v == null ? "" : String.valueOf(v));
            // set consistent min width and padding so columns align with header
            int minDp = 120;
            int minPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minDp, holder.container.getResources().getDisplayMetrics());
            tv.setMinWidth(minPx);
            int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, holder.container.getResources().getDisplayMetrics());
            tv.setPadding(pad, 0, pad, 0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(pad, 0, 0, 0);
            tv.setLayoutParams(lp);
            holder.container.addView(tv);
        }
    }

    @Override
    public int getItemCount() { return rows.size(); }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout container;
        VH(@NonNull View itemView) {
            super(itemView);
                container = itemView.findViewById(R.id.container_dynamic);
        }
    }
}
