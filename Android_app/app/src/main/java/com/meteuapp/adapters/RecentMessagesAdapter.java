package com.meteuapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.meteuapp.R;
import com.meteuapp.models.SensorMessage;

import java.util.ArrayList;
import java.util.List;

public class RecentMessagesAdapter extends RecyclerView.Adapter<RecentMessagesAdapter.VH> {

    private List<SensorMessage> items = new ArrayList<>();

    public void setItems(List<SensorMessage> list) { items.clear(); if (list != null) items.addAll(list); notifyDataSetChanged(); }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_messages_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        SensorMessage m = items.get(position);
        holder.colSensorId.setText(m.getSensorIdSafe());
        holder.colSensorType.setText(m.getSensorTypeSafe());
        holder.colStreetId.setText(m.getStreetIdSafe());
        holder.colTimestamp.setText(m.getRecordedAtSafe());
        holder.colLat.setText(String.valueOf(m.getLatitudeSafe()));
        holder.colLong.setText(String.valueOf(m.getLongitudeSafe()));
        holder.colAlt.setText(String.valueOf(m.getAltitudeSafe()));
        holder.colDistrict.setText(m.getDistrictSafe());
        holder.colNeighborhood.setText(m.getNeighborhoodSafe());
        holder.colTemp.setText(String.valueOf(m.getTempSafe()));
        holder.colHumid.setText(String.valueOf(m.getHumidSafe()));
        holder.colAqi.setText(String.valueOf(m.getAqiSafe()));
        holder.colLux.setText(String.valueOf(m.getLuxSafe()));
        holder.colSoundDb.setText(String.valueOf(m.getSoundDbSafe()));
        holder.colAtmHpa.setText(String.valueOf(m.getAtmHpaSafe()));
        holder.colUvIndex.setText(String.valueOf(m.getUvIndexSafe()));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView colSensorId, colSensorType, colStreetId, colTimestamp, colLat, colLong, colAlt, colDistrict, colNeighborhood,
                colTemp, colHumid, colAqi, colLux, colSoundDb, colAtmHpa, colUvIndex;
        VH(@NonNull View itemView) {
            super(itemView);
            colSensorId = itemView.findViewById(R.id.col_sensor_id);
            colSensorType = itemView.findViewById(R.id.col_sensor_type);
            colStreetId = itemView.findViewById(R.id.col_street_id);
            colTimestamp = itemView.findViewById(R.id.col_timestamp);
            colLat = itemView.findViewById(R.id.col_lat);
            colLong = itemView.findViewById(R.id.col_long);
            colAlt = itemView.findViewById(R.id.col_alt);
            colDistrict = itemView.findViewById(R.id.col_district);
            colNeighborhood = itemView.findViewById(R.id.col_neighborhood);
            colTemp = itemView.findViewById(R.id.col_temp);
            colHumid = itemView.findViewById(R.id.col_humid);
            colAqi = itemView.findViewById(R.id.col_aqi);
            colLux = itemView.findViewById(R.id.col_lux);
            colSoundDb = itemView.findViewById(R.id.col_sound_db);
            colAtmHpa = itemView.findViewById(R.id.col_atmhpa);
            colUvIndex = itemView.findViewById(R.id.col_uv_index);
        }
    }
}
