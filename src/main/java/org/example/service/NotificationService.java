package org.example.service;

import org.example.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.StringJoiner;

/**
 * Notification helper: stores notifications in notifications table and provides a tiny API.
 */
public final class NotificationService {

    private NotificationService() {}

    /**
     * Store a notification with a simple string. This convenience method uses a default
     * title and severity and stores the provided text in the message column.
     *
     * @param s notification text
     */
    public static void add(String s) {
        createNotification("Notification", s, "INFO");
    }

    public static void createNotification(String title, String message, String severity) {
        String insert = "INSERT INTO notifications(title,message,severity,is_read,created_at) VALUES(?,?,?,?,?)";
        try (Connection con = DatabaseManager.getConnection(); PreparedStatement ps = con.prepareStatement(insert)) {
            ps.setString(1, title);
            ps.setString(2, message);
            ps.setString(3, severity == null ? "INFO" : severity);
            ps.setInt(4, 0);
            ps.setLong(5, Instant.now().toEpochMilli());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Return all stored notification messages joined with a newline. Returns an empty string
     * when there are no notifications.
     *
     * @return joined notification messages
     */
    public static String getAll() {
        String sql = "SELECT title, message FROM notifications ORDER BY created_at ASC";
        StringJoiner sj = new StringJoiner("\n");
        try (Connection con = DatabaseManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String title = rs.getString("title");
                String msg = rs.getString("message");
                if (title == null) title = "";
                if (msg == null) msg = "";
                if (!title.isBlank()) sj.add(title + ": " + msg);
                else sj.add(msg);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return sj.length() == 0 ? "" : sj.toString();
    }

    /**
     * Delete all notifications from the database. Use with caution.
     */
    public static void clear() {
        String sql = "DELETE FROM notifications";
        try (Connection con = DatabaseManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

}
