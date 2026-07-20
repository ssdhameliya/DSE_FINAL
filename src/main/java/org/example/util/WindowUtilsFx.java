package org.example.util;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public final class WindowUtilsFx {

    private static final Preferences PREFS = Preferences.userNodeForPackage(WindowUtilsFx.class);
    private static final String KEY_X = "fx.win.x";
    private static final String KEY_Y = "fx.win.y";
    private static final String KEY_W = "fx.win.w";
    private static final String KEY_H = "fx.win.h";
    private static final String KEY_MAX = "fx.win.max";

    private WindowUtilsFx() {}

    /**
     * Apply saved bounds / sensible defaults to the given Stage.
     * Must be called with a non-null Stage (for example, from start(primaryStage)).
     */
    public static void apply(Stage stage, double defaultW, double defaultH) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage must not be null. Call WindowUtilsFx.apply(primaryStage, ...)");
        }

        double x = PREFS.getDouble(KEY_X, Double.NaN);
        double y = PREFS.getDouble(KEY_Y, Double.NaN);
        double w = PREFS.getDouble(KEY_W, defaultW);
        double h = PREFS.getDouble(KEY_H, defaultH);
        boolean maximized = PREFS.getBoolean(KEY_MAX, false);

        if (!Double.isNaN(x) && !Double.isNaN(y)) {
            Rectangle2D valid = ensureVisibleOnScreens(x, y, w, h);
            stage.setX(valid.getMinX());
            stage.setY(valid.getMinY());
            stage.setWidth(valid.getWidth());
            stage.setHeight(valid.getHeight());
        } else {
            stage.setWidth(w);
            stage.setHeight(h);
            stage.centerOnScreen();
        }

        stage.setMaximized(maximized);

        stage.setOnShown(ev -> bringToFront(stage));

        stage.setOnCloseRequest(ev -> save(stage));
    }

    private static void save(Stage stage) {
        // Save normal bounds (if maximized, still store current bounds)
        PREFS.putDouble(KEY_X, stage.getX());
        PREFS.putDouble(KEY_Y, stage.getY());
        PREFS.putDouble(KEY_W, stage.getWidth());
        PREFS.putDouble(KEY_H, stage.getHeight());
        PREFS.putBoolean(KEY_MAX, stage.isMaximized());
    }

    private static Rectangle2D ensureVisibleOnScreens(double x, double y, double w, double h) {
        for (Screen s : Screen.getScreens()) {
            Rectangle2D b = s.getVisualBounds();
            Rectangle2D r = new Rectangle2D(x, y, w, h);
            if (b.intersects(r) || b.contains(r)) {
                double nx = Math.max(b.getMinX(), Math.min(x, b.getMaxX() - Math.min(w, b.getWidth())));
                double ny = Math.max(b.getMinY(), Math.min(y, b.getMaxY() - Math.min(h, b.getHeight())));
                double nw = Math.min(w, b.getWidth());
                double nh = Math.min(h, b.getHeight());
                return new Rectangle2D(nx, ny, nw, nh);
            }
        }
        Rectangle2D primary = Screen.getPrimary().getVisualBounds();
        double nw = Math.min(w, primary.getWidth());
        double nh = Math.min(h, primary.getHeight());
        double nx = primary.getMinX() + (primary.getWidth() - nw) / 2;
        double ny = primary.getMinY() + (primary.getHeight() - nh) / 2;
        return new Rectangle2D(nx, ny, nw, nh);
    }

    private static void bringToFront(Stage stage) {
        Platform.runLater(() -> {
            try {
                stage.toFront();
                stage.requestFocus();
                // Temporary always-on-top toggle to force OS to bring window forward
                boolean was = stage.isAlwaysOnTop();
                stage.setAlwaysOnTop(true);
                // small pause on UI thread is not ideal; use a short delayed task instead
                Platform.runLater(() -> stage.setAlwaysOnTop(was));
            } catch (Exception ignored) {}
        });
    }
}
