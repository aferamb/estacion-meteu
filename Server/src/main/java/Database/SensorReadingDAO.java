package Database;

import Logic.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class SensorReadingDAO {

    private static final Gson gson = new Gson();
    // whitelist of allowed columns for filtering/sorting to avoid SQL injection
    public static final Set<String> ALLOWED_COLUMNS = new HashSet<String>() {{
        add("id");
        add("sensor_id");
        add("sensor_type");
        add("street_id");
        add("recorded_at");
        add("latitude");
        add("longitude");
        add("altitude");
        add("district");
        add("neighborhood");
        add("temp");
        add("humid");
        add("aqi");
        add("lux");
        add("sound_db");
        add("atmhpa");
        add("uv_index");
        add("bsec_status");
        add("iaq");
        add("static_iaq");
        add("co2_eq");
        add("breath_voc_eq");
        add("raw_temperature");
        add("raw_humidity");
        add("pressure_hpa");
        add("gas_resistance_ohm");
        add("gas_percentage");
        add("stabilization_status");
        add("run_in_status");
        add("sensor_heat_comp_temp");
        add("sensor_heat_comp_hum");
    }};

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
            Timestamp recordedAt = getTimestamp(map, "timestamp");
            if (recordedAt == null) recordedAt = getTimestamp(map, "recorded_at");

            // location is nested
            Map<String, Object> location = getMap(map, "location");
            Double latitude = getDouble(location, "lat");
            Double longitude = getDouble(location, "long");
            if (longitude == null) longitude = getDouble(location, "lng");
            Double altitude = getDouble(location, "alt");
            String district = getString(location, "district");
            String neighborhood = getString(location, "neighborhood");
            // Fallback to top-level flat fields if nested structure not provided
            if (location == null) {
                latitude = getDouble(map, "lat");
                longitude = getDouble(map, "long");
                if (longitude == null) longitude = getDouble(map, "lng");
                altitude = getDouble(map, "alt");
                district = getString(map, "district");
                neighborhood = getString(map, "neighborhood");
                // also accept 'latitude'/'longitude' as top-level
                if (latitude == null) latitude = getDouble(map, "latitude");
                if (longitude == null) longitude = getDouble(map, "longitude");
            } else {
                // also accept top-level as additional fallback
                if (latitude == null) latitude = getDouble(map, "lat");
                if (longitude == null) {
                    longitude = getDouble(map, "long");
                    if (longitude == null) longitude = getDouble(map, "lng");
                }
                if (altitude == null) altitude = getDouble(map, "alt");
                if (district == null) district = getString(map, "district");
                if (neighborhood == null) neighborhood = getString(map, "neighborhood");
            }

            // data nested
            Map<String, Object> data = getMap(map, "data");
            Double temp = getDouble(data, "temp");
            Double humid = getDouble(data, "humid");
            Integer aqi = getInt(data, "aqi");
            Double lux = getDouble(data, "lux");
            Double soundDb = getDouble(data, "sound_db");
            Double atmhpa = getDouble(data, "atmhpa");
            Double uvIndex = getDouble(data, "uv_index");
            // Fallback to top-level flat fields if data map not provided
            if (data == null) {
                temp = getDouble(map, "temp");
                humid = getDouble(map, "humid");
                aqi = getInt(map, "aqi");
                lux = getDouble(map, "lux");
                soundDb = getDouble(map, "sound_db");
                atmhpa = getDouble(map, "atmhpa");
                uvIndex = getDouble(map, "uv_index");
            } else {
                if (temp == null) temp = getDouble(map, "temp");
                if (humid == null) humid = getDouble(map, "humid");
                if (aqi == null) aqi = getInt(map, "aqi");
                if (lux == null) lux = getDouble(map, "lux");
                if (soundDb == null) soundDb = getDouble(map, "sound_db");
                if (atmhpa == null) atmhpa = getDouble(map, "atmhpa");
                if (uvIndex == null) uvIndex = getDouble(map, "uv_index");
            }

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
                // keep up to nanoseconds (Timestamp accepts up to 9 digits)
                String before = s.substring(0, dot);
                String after = s.substring(dot+1);
                // strip any non-digit characters from fractional part (timezone remnants)
                after = after.replaceAll("[^0-9]", "");
                if (after.length() > 9) after = after.substring(0, 9);
                if (after.length() > 0) s = before + "." + after; else s = before;
            }
            return Timestamp.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse a timestamp string accepted by the project: ISO-8601 (with or without Z)
     * or epoch milliseconds as a number string. Returns null on parse failure.
     */
    public static Timestamp parseTimestampString(String s) {
        if (s == null) return null;
        s = s.trim();
        try {
            // try epoch millis
            if (s.matches("^\\d+$")) {
                long ms = Long.parseLong(s);
                return new Timestamp(ms);
            }
        } catch (Exception ignored) {}
        try {
            // fallback to previous tolerant parser
            s = s.replace('T', ' ');
            if (s.endsWith("Z")) s = s.substring(0, s.length()-1);
            int dot = s.indexOf('.');
            if (dot > 0) {
                String before = s.substring(0, dot);
                String after = s.substring(dot+1);
                after = after.replaceAll("[^0-9]", "");
                if (after.length() > 9) after = after.substring(0, 9);
                if (after.length() > 0) s = before + "." + after; else s = before;
            }
            return Timestamp.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Format a Timestamp to the project's strict string format: yyyy-MM-ddHH:mm:ss
     */
    public static String formatTimestamp(Timestamp ts) {
        if (ts == null) return null;
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
        return fmt.format(ts);
    }

    /**
     * Parse a servlet parameter timestamp in the strict expected format: yyyy-MM-ddHH:mm:ss
     * Example: 2025-12-0504:02:33
     * Throws IllegalArgumentException if format does not match.
     */
    public static Timestamp parseParamTimestampStrict(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        // expected: 4-2-2 then 2:2:2 => total length 19 (10 date + 8 time) -> 18? actually 'yyyy-MM-ddHH:mm:ss' length=19
        if (!trimmed.matches("^\\d{4}-\\d{2}-\\d{2}\\d{2}:\\d{2}:\\d{2}$")) {
            throw new IllegalArgumentException("Formato de timestamp inválido. Se espera: yyyy-MM-ddHH:mm:ss (ej. 2025-12-0504:02:33)");
        }
        String datePart = trimmed.substring(0, 10);
        String timePart = trimmed.substring(10);
        String normalized = datePart + " " + timePart;
        try {
            return Timestamp.valueOf(normalized);
        } catch (Exception e) {
            throw new IllegalArgumentException("Formato de timestamp inválido. Se espera: yyyy-MM-ddHH:mm:ss (ej. 2025-12-0504:02:33)");
        }
    }

    /**
     * Flexible query that supports time range, a single column filter with operator,
     * ordering and pagination. Uses ALLOWED_COLUMNS whitelist for column names.
     */
    public static List<Map<String, Object>> queryReadings(QueryParams params) {
        ConectionDDBB conector = new ConectionDDBB();
        Connection con = null;
        List<Map<String, Object>> rows = new ArrayList<>();
        try {
            con = conector.obtainConnection(true);
            StringBuilder sql = new StringBuilder("SELECT * FROM sensor_readings WHERE 1=1");
            List<Object> arguments = new ArrayList<>();

            if (params.getStart() != null) {
                sql.append(" AND recorded_at >= ?");
                arguments.add(params.getStart());
            }
            if (params.getEnd() != null) {
                sql.append(" AND recorded_at <= ?");
                arguments.add(params.getEnd());
            }
            if (params.getFilter() != null && !params.getFilter().isEmpty() && params.getValue() != null) {
                String col = params.getFilter();
                if (!ALLOWED_COLUMNS.contains(col)) throw new IllegalArgumentException("Invalid filter column");
                String op = params.getOperator();
                if (op == null) op = "=";
                // only allow a small set of operators
                switch (op) {
                    case "=":
                    case "!=":
                    case ">":
                    case ">=":
                    case "<":
                    case "<=":
                        sql.append(" AND ").append(col).append(" ").append(op).append(" ?");
                        arguments.add(params.getValue());
                        break;
                    case "like":
                    case "LIKE":
                        sql.append(" AND ").append(col).append(" LIKE ?");
                        String v = params.getValue();
                        if (!v.contains("%")) v = "%" + v + "%";
                        arguments.add(v);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported operator");
                }
            }

            if (params.getSortBy() != null && !params.getSortBy().isEmpty()) {
                String sort = params.getSortBy();
                if (!ALLOWED_COLUMNS.contains(sort)) throw new IllegalArgumentException("Invalid sort column");
                String ord = "ASC".equalsIgnoreCase(params.getOrder()) ? "ASC" : "DESC".equalsIgnoreCase(params.getOrder()) ? "DESC" : "ASC";
                sql.append(" ORDER BY ").append(sort).append(" ").append(ord);
            } else {
                sql.append(" ORDER BY recorded_at DESC");
            }

            // limit/offset
            int limit = params.getLimit() > 0 ? params.getLimit() : 200;
            if (limit > 200) limit = 200;
            int offset = params.getOffset() >= 0 ? params.getOffset() : 0;
            sql.append(" LIMIT ? OFFSET ?");

            PreparedStatement ps = con.prepareStatement(sql.toString());
            int idx = 1;
            for (Object a : arguments) {
                if (a instanceof Timestamp) ps.setTimestamp(idx++, (Timestamp) a);
                else ps.setObject(idx++, a);
            }
            ps.setInt(idx++, limit);
            ps.setInt(idx++, offset);

            ResultSet rs = ps.executeQuery();
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int c = 1; c <= cols; c++) {
                    Object v = rs.getObject(c);
                    String colName = md.getColumnName(c);
                    if (v != null && colName != null && colName.equalsIgnoreCase("recorded_at") && v instanceof java.sql.Timestamp) {
                        row.put(colName, formatTimestamp((java.sql.Timestamp) v));
                    } else {
                        row.put(colName, v);
                    }
                }
                rows.add(row);
            }
        } catch (Exception e) {
            Log.log.error("Error querying sensor_readings: {}", e);
        } finally {
            conector.closeConnection(con);
        }
        return rows;
    }
}
