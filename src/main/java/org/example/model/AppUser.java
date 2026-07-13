package org.example.model;

public class AppUser {
    private int id;
    private String username;
    private String password;
    private String fullName;
    private String role;
    private String email;
    private boolean active;

    public int getId() {
        return id;
    }

    public void setId(int value) {
        id = value;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String value) {
        username = value;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String value) {
        password = value;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String value) {
        fullName = value;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String value) {
        role = value;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String value) {
        email = value;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean value) {
        active = value;
    }
}
