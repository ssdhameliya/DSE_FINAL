package org.example.service;

import org.example.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

/**
 * Notification helper: stores notifications in notifications table and provides a tiny API.
 */
public final class NotificationService {

    private NotificationService() {}

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

}
