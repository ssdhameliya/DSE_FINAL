package org.example.theme;

import javafx.scene.Scene;
import org.example.config.ConfigManager;

public final class ThemeManager {

    public enum Theme {
        LIGHT,
        DARK
    }

    private static Theme currentTheme =
        "DARK".equals(ConfigManager.get("theme", "LIGHT"))
            ? Theme.DARK
            : Theme.LIGHT;

    private ThemeManager() {
    }

    public static void applyTheme(Scene scene) {

        scene.getStylesheets().removeIf(css ->
            css.contains("light-theme1.css")
                || css.contains("dark-theme1.css"));

        if (currentTheme == Theme.DARK) {

            scene.getStylesheets().add(
                ThemeManager.class
                    .getResource("/css/dark-theme1.css")
                    .toExternalForm());

        } else {

            scene.getStylesheets().add(
                ThemeManager.class
                    .getResource("/css/light-theme1.css")
                    .toExternalForm());

        }

    }

    public static void toggle(Scene scene) {

        if (currentTheme == Theme.LIGHT) {
            currentTheme = Theme.DARK;
        } else {
            currentTheme = Theme.LIGHT;
        }

        ConfigManager.set("theme", currentTheme.name());
        applyTheme(scene);
    }

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

}
