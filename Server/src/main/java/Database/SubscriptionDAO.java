package Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import Logic.Log;

public class SubscriptionDAO {

    public boolean addSubscription(String topic) {
        ConectionDDBB conector = new ConectionDDBB();
        Connection con = null;
        try {
            con = conector.obtainConnection(true);
            String dataTopic = topic;
            String alertTopic = null;
            if (topic.contains("/alerts")) {
                dataTopic = topic.split("/alerts")[0];
                alertTopic = topic;
            }
            String sql = "INSERT INTO subscriptions (topic, data_topic, alert_topic, active) VALUES (?,?,?,1) ON DUPLICATE KEY UPDATE active=1";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, topic);
            ps.setString(2, dataTopic);
            ps.setString(3, alertTopic);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            Log.log.error("Error adding subscription: {}", e);
            return false;
        } finally {
            conector.closeConnection(con);
        }
    }

    public boolean removeSubscription(String topic) {
        ConectionDDBB conector = new ConectionDDBB();
        Connection con = null;
        try {
            con = conector.obtainConnection(true);
            String sql = "DELETE FROM subscriptions WHERE topic = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, topic);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            Log.log.error("Error removing subscription: {}", e);
            return false;
        } finally {
            conector.closeConnection(con);
        }
    }

    public List<String> getAllActiveTopics() {
        ArrayList<String> results = new ArrayList<>();
        ConectionDDBB conector = new ConectionDDBB();
        Connection con = null;
        try {
            con = conector.obtainConnection(true);
            PreparedStatement ps = con.prepareStatement("SELECT topic FROM subscriptions WHERE active = 1");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(rs.getString("topic"));
            }
        } catch (Exception e) {
            Log.log.error("Error listing subscriptions: {}", e);
        } finally {
            conector.closeConnection(con);
        }
        return results;
    }
}
