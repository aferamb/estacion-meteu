package Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import Logic.Log;
import Utils.PasswordUtil;

public class UserDAO {

    public static boolean createUser(String username, char[] password, String role) {
        ConectionDDBB cdb = new ConectionDDBB();
        Connection con = null;
        try {
            con = cdb.obtainConnection(false);
            String salt = PasswordUtil.generateSalt(16);
            String hash = PasswordUtil.hashPassword(password, salt);
            // clear password array in memory as soon as possible
            for (int i = 0; i < password.length; i++) password[i] = '\0';
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO users(username,password_hash,salt,role) VALUES (?,?,?,?)")) {
                ps.setString(1, username);
                ps.setString(2, hash);
                ps.setString(3, salt);
                ps.setString(4, role);
                ps.executeUpdate();
            }
            cdb.closeTransaction(con);
            return true;
        } catch (Exception e) {
            Log.log.error("UserDAO.createUser", e);
            if (con != null) cdb.cancelTransaction(con);
            return false;
        } finally {
            cdb.closeConnection(con);
        }
    }

    public static boolean validateCredentials(String username, char[] password) {
        ConectionDDBB cdb = new ConectionDDBB();
        Connection con = null;
        try {
            con = cdb.obtainConnection(true);
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT password_hash, salt FROM users WHERE username=?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String hash = rs.getString("password_hash");
                        String salt = rs.getString("salt");
                        boolean ok = PasswordUtil.verifyPassword(password, salt, hash);
                        // clear password memory
                        for (int i = 0; i < password.length; i++) password[i] = '\0';
                        return ok;
                    }
                }
            }
        } catch (Exception e) {
            Log.log.error("UserDAO.validateCredentials", e);
        } finally {
            cdb.closeConnection(con);
        }
        return false;
    }

    public static boolean deleteUser(String username) {
        ConectionDDBB cdb = new ConectionDDBB();
        Connection con = null;
        try {
            con = cdb.obtainConnection(false);
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM users WHERE username=?")) {
                ps.setString(1, username);
                int rows = ps.executeUpdate();
                cdb.closeTransaction(con);
                return rows > 0;
            }
        } catch (Exception e) {
            Log.log.error("UserDAO.deleteUser", e);
            if (con != null) cdb.cancelTransaction(con);
        } finally {
            cdb.closeConnection(con);
        }
        return false;
    }

    public static String listUsersJson() {
        ConectionDDBB cdb = new ConectionDDBB();
        Connection con = null;
        try {
            con = cdb.obtainConnection(true);
            try (PreparedStatement ps = con.prepareStatement("SELECT username, role, created_at, last_login, failed_attempts, disabled FROM users")) {
                try (ResultSet rs = ps.executeQuery()) {
                    java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();
                    while (rs.next()) {
                        java.util.Map<String, Object> m = new java.util.HashMap<>();
                        m.put("username", rs.getString("username"));
                        m.put("role", rs.getString("role"));
                        m.put("created_at", rs.getTimestamp("created_at"));
                        m.put("last_login", rs.getTimestamp("last_login"));
                        m.put("failed_attempts", rs.getInt("failed_attempts"));
                        m.put("disabled", rs.getInt("disabled") != 0);
                        list.add(m);
                    }
                    return new com.google.gson.Gson().toJson(list);
                }
            }
        } catch (Exception e) {
            Log.log.error("UserDAO.listUsersJson", e);
        } finally {
            cdb.closeConnection(con);
        }
        return "[]";
    }

    public static String getUserRole(String username) {
        ConectionDDBB cdb = new ConectionDDBB();
        Connection con = null;
        try {
            con = cdb.obtainConnection(true);
            try (PreparedStatement ps = con.prepareStatement("SELECT role FROM users WHERE username=?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getString("role");
                }
            }
        } catch (Exception e) {
            Log.log.error("UserDAO.getUserRole", e);
        } finally {
            cdb.closeConnection(con);
        }
        return null;
    }
}
