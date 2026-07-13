package org.example.dao;

import org.example.database.DatabaseManager;
import org.example.model.AppUser;

import java.sql.*;

public class UserDAO {
    public AppUser authenticate(String identity, String password) {
        String sql = "SELECT * FROM users WHERE (lower(username)=lower(?) OR lower(email)=lower(?)) AND password=? AND active=1";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, identity.trim());
            statement.setString(2, identity.trim());
            statement.setString(3, password);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not sign in", exception);
        }
    }

    public void register(AppUser user) {
        String sql = "INSERT INTO users(username,password,full_name,role,email,active) VALUES(?,?,?,?,?,1)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPassword());
            statement.setString(3, user.getFullName());
            statement.setString(4, "USER");
            statement.setString(5, user.getEmail());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalArgumentException("Username or email is already registered.", exception);
        }
    }

    public void changePassword(int id, String password) {
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement("UPDATE users SET password=? WHERE id=?")) {
            p.setString(1, password);
            p.setInt(2, id);
            p.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not change password", e);
        }
    }

    private AppUser map(ResultSet r) throws SQLException {
        AppUser u = new AppUser();
        u.setId(r.getInt("id"));
        u.setUsername(r.getString("username"));
        u.setPassword(r.getString("password"));
        u.setFullName(r.getString("full_name"));
        u.setRole(r.getString("role"));
        u.setEmail(r.getString("email"));
        u.setActive(r.getBoolean("active"));
        return u;
    }
}
