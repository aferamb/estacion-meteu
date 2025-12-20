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
import java.util.Map;

public class QueryReadingAdapter extends RecyclerView.Adapter<QueryReadingAdapter.VH> {
    private List<Map<String, Object>> items = new ArrayList<>();
    private boolean[] visibleColumns;

    public QueryReadingAdapter() {
        visibleColumns = new boolean[30];
        for (int i = 0; i < visibleColumns.length; i++) visibleColumns[i] = true;
    }

    public void setVisibleColumns(boolean[] flags) {
        if (flags == null) return;
        int n = Math.min(flags.length, visibleColumns.length);
        for (int i = 0; i < n; i++) visibleColumns[i] = flags[i];
        notifyDataSetChanged();
    }

    public void setItems(List<Map<String, Object>> list) { items.clear(); if (list != null) items.addAll(list); notifyDataSetChanged(); }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reading_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Map<String, Object> r = items.get(position);
        holder.setText(holder.colSensorId, getString(r, "sensor_id"));
        holder.setText(holder.colSensorType, getString(r, "sensor_type"));
        holder.setText(holder.colStreetId, getString(r, "street_id"));
        holder.setText(holder.colTimestamp, getString(r, "recorded_at"));
        holder.setText(holder.colLat, getNumber(r, "latitude"));
        holder.setText(holder.colLong, getNumber(r, "longitude"));
        holder.setText(holder.colAlt, getNumber(r, "altitude"));
        holder.setText(holder.colDistrict, getString(r, "district"));
        holder.setText(holder.colNeighborhood, getString(r, "neighborhood"));
        holder.setText(holder.colTemp, getNumber(r, "temp"));
        holder.setText(holder.colHumid, getNumber(r, "humid"));
        holder.setText(holder.colAqi, getIntString(r, "aqi"));
        holder.setText(holder.colLux, getNumber(r, "lux"));
        holder.setText(holder.colSoundDb, getNumber(r, "sound_db"));
        holder.setText(holder.colAtmHpa, getNumber(r, "atmhpa"));
        holder.setText(holder.colUvIndex, getNumber(r, "uv_index"));
        holder.setText(holder.colBsecStatus, getIntString(r, "bsec_status"));
        holder.setText(holder.colIaq, getNumber(r, "iaq"));
        holder.setText(holder.colStaticIaq, getNumber(r, "static_iaq"));
        holder.setText(holder.colCo2Eq, getNumber(r, "co2_eq"));
        holder.setText(holder.colBreathVocEq, getNumber(r, "breath_voc_eq"));
        holder.setText(holder.colRawTemperature, getNumber(r, "raw_temperature"));
        holder.setText(holder.colRawHumidity, getNumber(r, "raw_humidity"));
        holder.setText(holder.colPressureHpa, getNumber(r, "pressure_hpa"));
        holder.setText(holder.colGasResistanceOhm, getNumber(r, "gas_resistance_ohm"));
        holder.setText(holder.colGasPercentage, getNumber(r, "gas_percentage"));
        holder.setText(holder.colStabilizationStatus, getNumber(r, "stabilization_status"));
        holder.setText(holder.colRunInStatus, getNumber(r, "run_in_status"));
        holder.setText(holder.colSensorHeatCompTemp, getNumber(r, "sensor_heat_comp_temp"));
        holder.setText(holder.colSensorHeatCompHum, getNumber(r, "sensor_heat_comp_hum"));

        int idx = 0;
        holder.colSensorId.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colSensorType.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colStreetId.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colTimestamp.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colLat.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colLong.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colAlt.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colDistrict.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colNeighborhood.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colTemp.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colHumid.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colAqi.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colLux.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colSoundDb.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colAtmHpa.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colUvIndex.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colBsecStatus.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colIaq.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colStaticIaq.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colCo2Eq.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colBreathVocEq.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colRawTemperature.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colRawHumidity.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colPressureHpa.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colGasResistanceOhm.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colGasPercentage.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colStabilizationStatus.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colRunInStatus.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colSensorHeatCompTemp.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
        holder.colSensorHeatCompHum.setVisibility(visibleColumns[idx++]? View.VISIBLE: View.GONE);
    }

    private String getString(Map<String, Object> r, String key) {
        if (r == null || r.get(key) == null) return "";
        return r.get(key).toString();
    }

    private String getNumber(Map<String, Object> r, String key) {
        Object v = r == null ? null : r.get(key);
        if (v == null) return "";
        try {
            if (v instanceof Number) return String.format("%.2f", ((Number) v).doubleValue());
            return v.toString();
        } catch (Exception e) { return v.toString(); }
    }

    private String getIntString(Map<String, Object> r, String key) {
        Object v = r == null ? null : r.get(key);
        if (v == null) return "";
        return v.toString();
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView colSensorId, colSensorType, colStreetId, colTimestamp, colLat, colLong, colAlt,
                colDistrict, colNeighborhood, colTemp, colHumid, colAqi, colLux, colSoundDb, colAtmHpa,
                colUvIndex, colBsecStatus, colIaq, colStaticIaq, colCo2Eq, colBreathVocEq, colRawTemperature,
                colRawHumidity, colPressureHpa, colGasResistanceOhm, colGasPercentage, colStabilizationStatus,
                colRunInStatus, colSensorHeatCompTemp, colSensorHeatCompHum;

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
            colBsecStatus = itemView.findViewById(R.id.col_bsec_status);
            colIaq = itemView.findViewById(R.id.col_iaq);
            colStaticIaq = itemView.findViewById(R.id.col_static_iaq);
            colCo2Eq = itemView.findViewById(R.id.col_co2_eq);
            colBreathVocEq = itemView.findViewById(R.id.col_breath_voc_eq);
            colRawTemperature = itemView.findViewById(R.id.col_raw_temperature);
            colRawHumidity = itemView.findViewById(R.id.col_raw_humidity);
            colPressureHpa = itemView.findViewById(R.id.col_pressure_hpa);
            colGasResistanceOhm = itemView.findViewById(R.id.col_gas_resistance_ohm);
            colGasPercentage = itemView.findViewById(R.id.col_gas_percentage);
            colStabilizationStatus = itemView.findViewById(R.id.col_stabilization_status);
            colRunInStatus = itemView.findViewById(R.id.col_run_in_status);
            colSensorHeatCompTemp = itemView.findViewById(R.id.col_sensor_heat_comp_temp);
            colSensorHeatCompHum = itemView.findViewById(R.id.col_sensor_heat_comp_hum);
        }

        void setText(TextView tv, String v) { if (tv != null) tv.setText(v == null ? "" : v); }
    }
}
