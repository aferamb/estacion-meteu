package com.meteuapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.meteuapp.R;
import com.meteuapp.models.SensorReading;

import java.util.ArrayList;
import java.util.List;

public class ReadingTableAdapter extends RecyclerView.Adapter<ReadingTableAdapter.VH> {
    private List<SensorReading> items = new ArrayList<>();
    // visibility flags for each column in the same order as the holder fields
    private boolean[] visibleColumns;

    public ReadingTableAdapter() {
        // default: all visible (number of columns = 30)
        visibleColumns = new boolean[30];
        for (int i = 0; i < visibleColumns.length; i++) visibleColumns[i] = true;
    }

    public void setVisibleColumns(boolean[] flags) {
        if (flags == null) return;
        // copy up to length
        int n = Math.min(flags.length, visibleColumns.length);
        for (int i = 0; i < n; i++) visibleColumns[i] = flags[i];
        notifyDataSetChanged();
    }

    public void setItems(List<SensorReading> list) { items.clear(); if (list != null) items.addAll(list); notifyDataSetChanged(); }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reading_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        SensorReading r = items.get(position);
        holder.setText(holder.colSensorId, r == null ? null : r.getSensorIdSafe());
        holder.setText(holder.colSensorType, r == null ? null : r.getSensorType());
        holder.setText(holder.colStreetId, r == null ? null : r.getStreetIdSafe());
        holder.setText(holder.colTimestamp, r == null ? null : r.getRecordedAtSafe());
        holder.setText(holder.colLat, r == null ? null : doubleToStr(r.getLatitude()));
        holder.setText(holder.colLong, r == null ? null : doubleToStr(r.getLongitude()));
        holder.setText(holder.colAlt, r == null ? null : doubleToStr(r.getAltitude()));
        holder.setText(holder.colDistrict, r == null ? null : r.getDistrictSafe());
        holder.setText(holder.colNeighborhood, r == null ? null : r.getNeighborhoodSafe());
        holder.setText(holder.colTemp, r == null ? null : doubleToStr(r.getTemp()));
        holder.setText(holder.colHumid, r == null ? null : doubleToStr(r.getHumid()));
        holder.setText(holder.colAqi, r == null ? null : (r.getAqi()==null?"":String.valueOf(r.getAqi())));
        holder.setText(holder.colLux, r == null ? null : doubleToStr(r.getLux()));
        holder.setText(holder.colSoundDb, r == null ? null : doubleToStr(r.getSoundDb()));
        holder.setText(holder.colAtmHpa, r == null ? null : doubleToStr(r.getAtmHpa()));
        holder.setText(holder.colUvIndex, r == null ? null : doubleToStr(r.getUvIndex()));
        holder.setText(holder.colBsecStatus, r == null ? null : (r.getBsecStatus()==null?"":String.valueOf(r.getBsecStatus())));
        holder.setText(holder.colIaq, r == null ? null : doubleToStr(r.getIaq()));
        holder.setText(holder.colStaticIaq, r == null ? null : doubleToStr(r.getStaticIaq()));
        holder.setText(holder.colCo2Eq, r == null ? null : doubleToStr(r.getCo2Eq()));
        holder.setText(holder.colBreathVocEq, r == null ? null : doubleToStr(r.getBreathVocEq()));
        holder.setText(holder.colRawTemperature, r == null ? null : doubleToStr(r.getRawTemperature()));
        holder.setText(holder.colRawHumidity, r == null ? null : doubleToStr(r.getRawHumidity()));
        holder.setText(holder.colPressureHpa, r == null ? null : doubleToStr(r.getPressureHpa()));
        holder.setText(holder.colGasResistanceOhm, r == null ? null : doubleToStr(r.getGasResistanceOhm()));
        holder.setText(holder.colGasPercentage, r == null ? null : doubleToStr(r.getGasPercentage()));
        holder.setText(holder.colStabilizationStatus, r == null ? null : doubleToStr(r.getStabilizationStatus()));
        holder.setText(holder.colRunInStatus, r == null ? null : doubleToStr(r.getRunInStatus()));
        holder.setText(holder.colSensorHeatCompTemp, r == null ? null : doubleToStr(r.getSensorHeatCompTemp()));
        holder.setText(holder.colSensorHeatCompHum, r == null ? null : doubleToStr(r.getSensorHeatCompHum()));

        // apply visibility flags (same order as fields)
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

    private String doubleToStr(Double d) { return d == null ? "" : String.format("%.2f", d); }

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
