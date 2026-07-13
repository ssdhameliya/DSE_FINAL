package org.example.service;

import org.example.config.ConfigManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores user-visible application notifications and honours the user's preference.
 */
public final class NotificationService {

    private static final String ENABLED_KEY = "notifications.enabled";
    private static final String LOG_KEY = "notifications.log";
    private static final int MAX_NOTIFICATIONS = 50;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private NotificationService() {
    }

    public static boolean isEnabled() {
        return Boolean.parseBoolean(ConfigManager.get(ENABLED_KEY, "true"));
    }

    public static void setEnabled(boolean enabled) {
        ConfigManager.set(ENABLED_KEY, Boolean.toString(enabled));
    }

    public static void add(String message) {
        if (!isEnabled() || message == null || message.isBlank()) {
            return;
        }

        List<String> notifications = new ArrayList<>(getAll());
        notifications.add(0, TIME_FORMAT.format(LocalDateTime.now()) + " — " + message.replace('\n', ' '));

        if (notifications.size() > MAX_NOTIFICATIONS) {
            notifications = notifications.subList(0, MAX_NOTIFICATIONS);
        }

        ConfigManager.set(LOG_KEY, String.join("\n", notifications));
    }

    public static List<String> getAll() {
        String value = ConfigManager.get(LOG_KEY, "");
        if (value.isBlank()) {
            return Collections.emptyList();
        }

        return List.of(value.split("\\R"));
    }

    public static void clear() {
        ConfigManager.set(LOG_KEY, "");
    }
}
