package Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    // scheduled refresher (refresh-ahead)
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "RangeDAO-Refresher");
        t.setDaemon(true);
        return t;
    });

    static {
        // schedule periodic reloads; initial delay 30s, then every 30s
        scheduler.scheduleAtFixedRate(() -> {
            try {
                new RangeDAO().reloadAllRanges();
            } catch (Throwable t) {
                Log.log.error("Scheduled range reload failed: {}", t);
            }
        }, TTL_MS, TTL_MS, TimeUnit.MILLISECONDS);
    }

    public Range getRangeForParameter(String parameter) {
        long now = System.currentTimeMillis();
        if (now - lastLoad > TTL_MS) {
            // do a non-blocking reload by scheduling one if the scheduled task hasn't run yet
            // but do not block the caller; the scheduled task will refresh soon
            // If cache is empty (first start), perform a blocking reload to ensure values exist
            if (cache.isEmpty()) {
                reloadAllRanges();
            }
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

    public static void shutdownScheduler() {
        try {
            scheduler.shutdownNow();
        } catch (Exception e) {
            Log.log.error("Error shutting down RangeDAO scheduler: {}", e);
        }
    }
}
