package org.example.app;

import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.example.database.DatabaseManager;
import org.example.config.ConfigManager;
import org.example.util.SceneManager;
import org.example.util.WindowUtilsFx;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        ConfigManager.load();

        try {
            DatabaseManager.initialize();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                "Database initialization failed: " + e.getMessage()).showAndWait();
        }

        // Initialize SceneManager with the real Stage first so it can set its primaryStage
        SceneManager.initialize(stage);

        // Apply window behavior to the actual Stage parameter
        WindowUtilsFx.apply(stage, 1200, 800);

        // Show the stage after applying window settings
        stage.show();

        // Show splash (or initial scene) using SceneManager
        SceneManager.showSplash();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
