package Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import Logic.Log;

public class AlarmDAO {

    public boolean createAlarm(String sensorId, String streetId, String parameter, Double triggeredValue) {
        ConectionDDBB conector = new ConectionDDBB();
        Connection con = null;
        try {
            con = conector.obtainConnection(true);
            String sql = "INSERT INTO sensor_alarms (sensor_id, street_id, parameter, triggered_value, triggered_at, active) VALUES (?,?,?,?,NOW(),1)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, sensorId);
            ps.setString(2, streetId);
            ps.setString(3, parameter);
            ps.setDouble(4, triggeredValue);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            Log.log.error("Error creating alarm record: {}", e);
            return false;
        } finally {
            conector.closeConnection(con);
        }
    }

    public boolean resolveAlarm(String sensorId, String parameter, Double resolvedValue) {
        ConectionDDBB conector = new ConectionDDBB();
        Connection con = null;
        try {
            con = conector.obtainConnection(true);
            String sql = "UPDATE sensor_alarms SET resolved_at = NOW(), resolved_value = ?, active = 0 WHERE (sensor_id = ? OR (? IS NULL AND sensor_id IS NULL)) AND parameter = ? AND active = 1";
            PreparedStatement ps = con.prepareStatement(sql);
            if (resolvedValue == null) ps.setNull(1, java.sql.Types.DOUBLE);
            else ps.setDouble(1, resolvedValue);
            ps.setString(2, sensorId);
            ps.setString(3, sensorId);
            ps.setString(4, parameter);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            Log.log.error("Error resolving alarm record: {}", e);
            return false;
        } finally {
            conector.closeConnection(con);
        }
    }

    public boolean hasActiveAlarm(String sensorId, String parameter) {
        ConectionDDBB conector = new ConectionDDBB();
        Connection con = null;
        try {
            con = conector.obtainConnection(true);
            String sql = "SELECT id FROM sensor_alarms WHERE (sensor_id = ? OR (? IS NULL AND sensor_id IS NULL)) AND parameter = ? AND active = 1 LIMIT 1";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, sensorId);
            ps.setString(2, sensorId);
            ps.setString(3, parameter);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            Log.log.error("Error checking active alarm: {}", e);
            return false;
        } finally {
            conector.closeConnection(con);
        }
    }
}
