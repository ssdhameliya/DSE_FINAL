package org.example.app;

import javafx.application.Application;
import javafx.stage.Stage;
import org.example.database.DatabaseManager;
import org.example.config.ConfigManager;
import org.example.util.SceneManager;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        ConfigManager.load();
        DatabaseManager.initialize();

        SceneManager.initialize(stage);
        SceneManager.showSplash();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
