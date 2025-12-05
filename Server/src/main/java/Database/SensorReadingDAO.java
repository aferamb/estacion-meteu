package Database;

import Logic.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.Map;

public class SensorReadingDAO {

    private static final Gson gson = new Gson();

    /**
     * Insert sensor reading parsed from topic payload JSON.
     * Accepts nested structure used by stations: `timestamp`, `location`, `data`, `extra`.
     * Missing fields are inserted as NULL (no random substitution).
     */
    public static boolean insertFromTopicPayload(String topic, String payload) {
        ConectionDDBB conector = new ConectionDDBB();
        Connection con = null;
        try {
            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> map = gson.fromJson(payload, mapType);

            con = conector.obtainConnection(false); // start transaction

            String sql = "INSERT INTO sensor_readings (sensor_id, sensor_type, street_id, recorded_at, latitude, longitude, altitude, district, neighborhood, temp, humid, aqi, lux, sound_db, atmhpa, uv_index, bsec_status, iaq, static_iaq, co2_eq, breath_voc_eq, raw_temperature, raw_humidity, pressure_hpa, gas_resistance_ohm, gas_percentage, stabilization_status, run_in_status, sensor_heat_comp_temp, sensor_heat_comp_hum) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql);

            // top-level fields
            String sensorId = getString(map, "sensor_id");
            String sensorType = getString(map, "sensor_type");
            String streetId = getString(map, "street_id");

            // timestamp may be under "timestamp" or "recorded_at"
            Timestamp recordedAt = getTimestamp(map, "recorded_at");
            if (recordedAt == null) recordedAt = getTimestamp(map, "timestamp");

            // location is nested
            Map<String, Object> location = getMap(map, "location");
            Double latitude = getDouble(location, "lat");
            Double longitude = getDouble(location, "long");
            if (longitude == null) longitude = getDouble(location, "lng");
            Double altitude = getDouble(location, "alt");
            String district = getString(location, "district");
            String neighborhood = getString(location, "neighborhood");

            // data nested
            Map<String, Object> data = getMap(map, "data");
            Double temp = getDouble(data, "temp");
            Double humid = getDouble(data, "humid");
            Integer aqi = getInt(data, "aqi");
            Double lux = getDouble(data, "lux");
            Double soundDb = getDouble(data, "sound_db");
            Double atmhpa = getDouble(data, "atmhpa");
            Double uvIndex = getDouble(data, "uv_index");

            // extra nested
            Map<String, Object> extra = getMap(map, "extra");
            Integer bsecStatus = getInt(extra, "bsec_status");
            Double iaq = getDouble(extra, "iaq");
            Double staticIaq = getDouble(extra, "static_iaq");
            Double co2Eq = getDouble(extra, "co2_eq");
            Double breathVocEq = getDouble(extra, "breath_voc_eq");
            Double rawTemperature = getDouble(extra, "raw_temperature");
            Double rawHumidity = getDouble(extra, "raw_humidity");
            Double pressureHpa = getDouble(extra, "pressure_hpa");
            Double gasResistanceOhm = getDouble(extra, "gas_resistance_ohm");
            Double gasPercentage = getDouble(extra, "gas_percentage");
            Double stabilizationStatus = getDouble(extra, "stabilization_status");
            Double runInStatus = getDouble(extra, "run_in_status");
            Double sensorHeatCompTemp = getDouble(extra, "sensor_heat_comp_temp");
            Double sensorHeatCompHum = getDouble(extra, "sensor_heat_comp_hum");

            int idx = 1;
            ps.setString(idx++, sensorId);
            ps.setString(idx++, sensorType);
            ps.setString(idx++, streetId);

            if (recordedAt != null) ps.setTimestamp(idx++, recordedAt); else ps.setNull(idx++, java.sql.Types.TIMESTAMP);

            if (latitude != null) ps.setDouble(idx++, latitude); else ps.setNull(idx++, java.sql.Types.DOUBLE);
            if (longitude != null) ps.setDouble(idx++, longitude); else ps.setNull(idx++, java.sql.Types.DOUBLE);
            if (altitude != null) ps.setDouble(idx++, altitude); else ps.setNull(idx++, java.sql.Types.DOUBLE);
            if (district != null) ps.setString(idx++, district); else ps.setNull(idx++, java.sql.Types.VARCHAR);
            if (neighborhood != null) ps.setString(idx++, neighborhood); else ps.setNull(idx++, java.sql.Types.VARCHAR);

            if (temp != null) ps.setDouble(idx++, temp); else ps.setNull(idx++, java.sql.Types.DOUBLE);
            if (humid != null) ps.setDouble(idx++, humid); else ps.setNull(idx++, java.sql.Types.DOUBLE);
            if (aqi != null) ps.setInt(idx++, aqi); else ps.setNull(idx++, java.sql.Types.INTEGER);
            if (lux != null) ps.setDouble(idx++, lux); else ps.setNull(idx++, java.sql.Types.DOUBLE);
            if (soundDb != null) ps.setDouble(idx++, soundDb); else ps.setNull(idx++, java.sql.Types.DOUBLE);
            if (atmhpa != null) ps.setDouble(idx++, atmhpa); else ps.setNull(idx++, java.sql.Types.DOUBLE);
            if (uvIndex != null) ps.setDouble(idx++, uvIndex); else ps.setNull(idx++, java.sql.Types.DOUBLE);

            if (bsecStatus != null) ps.setInt(idx++, bsecStatus); else ps.setNull(idx++, java.sql.Types.INTEGER);
            if (iaq != null) ps.setDouble(idx++, iaq); else ps.setNull(idx++, java.sql.Types.DOUBLE);
            if (staticIaq != null) ps.setDouble(idx++, staticIaq); else ps.setNull(idx++, java.sql.Types.DOUBLE);
            if (co2Eq != null) ps.setDouble(idx++, co2Eq); else ps.setNull(idx++, java.sql.Types.DOUBLE);
            if (breathVocEq != null) ps.setDouble(idx++, breathVocEq); else ps.setNull(idx++, java.sql.Types.DOUBLE);
            if (rawTemperature != null) ps.setDouble(idx++, rawTemperature); else ps.setNull(idx++, java.sql.Types.DOUBLE);
            if (rawHumidity != null) ps.setDouble(idx++, rawHumidity); else ps.setNull(idx++, java.sql.Types.DOUBLE);
            if (pressureHpa != null) ps.setDouble(idx++, pressureHpa); else ps.setNull(idx++, java.sql.Types.DOUBLE);
            if (gasResistanceOhm != null) ps.setDouble(idx++, gasResistanceOhm); else ps.setNull(idx++, java.sql.Types.DOUBLE);
            if (gasPercentage != null) ps.setDouble(idx++, gasPercentage); else ps.setNull(idx++, java.sql.Types.DOUBLE);
            if (stabilizationStatus != null) ps.setDouble(idx++, stabilizationStatus); else ps.setNull(idx++, java.sql.Types.DOUBLE);
            if (runInStatus != null) ps.setDouble(idx++, runInStatus); else ps.setNull(idx++, java.sql.Types.DOUBLE);
            if (sensorHeatCompTemp != null) ps.setDouble(idx++, sensorHeatCompTemp); else ps.setNull(idx++, java.sql.Types.DOUBLE);
            if (sensorHeatCompHum != null) ps.setDouble(idx++, sensorHeatCompHum); else ps.setNull(idx++, java.sql.Types.DOUBLE);

            ps.executeUpdate();
            conector.closeTransaction(con);
            return true;
        } catch (Exception e) {
            Log.log.error("Error inserting sensor reading: {}", e);
            if (con != null) {
                conector.cancelTransaction(con);
            }
            return false;
        } finally {
            conector.closeConnection(con);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMap(Map<String, Object> root, String key) {
        if (root == null) return null;
        Object o = root.get(key);
        if (o instanceof Map) return (Map<String, Object>) o;
        return null;
    }

    private static String getString(Map<String, Object> m, String key) {
        if (m == null) return null;
        Object o = m.get(key);
        return o == null ? null : o.toString();
    }

    private static Double getDouble(Map<String, Object> m, String key) {
        if (m == null) return null;
        Object o = m.get(key);
        if (o == null) return null;
        try {
            return Double.parseDouble(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer getInt(Map<String, Object> m, String key) {
        if (m == null) return null;
        Object o = m.get(key);
        if (o == null) return null;
        try {
            // handle floats that represent integers
            String s = o.toString();
            if (s.contains(".")) s = s.substring(0, s.indexOf('.'));
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static Timestamp getTimestamp(Map<String, Object> m, String key) {
        String s = getString(m, key);
        if (s == null) return null;
        try {
            s = s.replace('T', ' ');
            if (s.endsWith("Z")) s = s.substring(0, s.length()-1);
            // Trim fractional seconds to avoid parsing issues when present
            int dot = s.indexOf('.');
            if (dot > 0) {
                // keep up to microseconds compatible with Timestamp.valueOf (needs up to nanos)
                String before = s.substring(0, dot);
                String after = s.substring(dot+1);
                // remove timezone remnants if any
                if (after.contains(" ")) after = after.substring(0, after.indexOf(' '));
                s = before + " " + after;
                // attempt to keep only first 6-9 digits is not necessary for now
            }
            return Timestamp.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }
}
