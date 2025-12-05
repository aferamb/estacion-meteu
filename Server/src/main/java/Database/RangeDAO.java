package Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import Logic.Log;

/**
 * RangeDAO with a lightweight in-memory cache to avoid DB lookup on every message.
 * Cache is reloaded periodically (TTL) and updateRange updates the DB and the cache.
 */
public class RangeDAO {

    private static final ConcurrentHashMap<String, Range> cache = new ConcurrentHashMap<>();
    private static volatile long lastLoad = 0L;
    // TTL in ms --- tune as needed
    private static final long TTL_MS = 30_000; // 30 seconds

    public Range getRangeForParameter(String parameter) {
        long now = System.currentTimeMillis();
        if (now - lastLoad > TTL_MS) {
            // reload all ranges asynchronously but keep it simple and do it synchronously here
            reloadAllRanges();
        }
        return cache.get(parameter);
    }

    public synchronized void reloadAllRanges() {
        ConectionDDBB conector = new ConectionDDBB();
        Connection con = null;
        try {
            con = conector.obtainConnection(true);
            String sql = "SELECT parameter, min_value, max_value FROM parameter_ranges";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            ConcurrentHashMap<String, Range> newMap = new ConcurrentHashMap<>();
            while (rs.next()) {
                String param = rs.getString("parameter");
                Double min = rs.getObject("min_value") == null ? null : rs.getDouble("min_value");
                Double max = rs.getObject("max_value") == null ? null : rs.getDouble("max_value");
                newMap.put(param, new Range(min, max));
            }
            cache.clear();
            cache.putAll(newMap);
            lastLoad = System.currentTimeMillis();
        } catch (Exception e) {
            Log.log.error("Error reloading parameter ranges: {}", e);
        } finally {
            conector.closeConnection(con);
        }
    }

    public boolean updateRange(String parameter, Double minValue, Double maxValue) {
        ConectionDDBB conector = new ConectionDDBB();
        Connection con = null;
        try {
            con = conector.obtainConnection(true);
            String sql = "INSERT INTO parameter_ranges (parameter, min_value, max_value) VALUES (?,?,?) ON DUPLICATE KEY UPDATE min_value = VALUES(min_value), max_value = VALUES(max_value)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, parameter);
            if (minValue == null) ps.setNull(2, java.sql.Types.DOUBLE); else ps.setDouble(2, minValue);
            if (maxValue == null) ps.setNull(3, java.sql.Types.DOUBLE); else ps.setDouble(3, maxValue);
            ps.executeUpdate();
            // update cache immediately
            cache.put(parameter, new Range(minValue, maxValue));
            lastLoad = System.currentTimeMillis();
            return true;
        } catch (Exception e) {
            Log.log.error("Error updating range for {}: {}", parameter, e);
            return false;
        } finally {
            conector.closeConnection(con);
        }
    }
}
