package org.example.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.theme.ThemeManager;

import java.io.IOException;

import static java.awt.image.ImageObserver.HEIGHT;
import static java.awt.image.ImageObserver.WIDTH;

public class SceneManager {

    private static Stage primaryStage;

    private static final double WIDTH = 1400;
    private static final double HEIGHT = 850;

    private SceneManager() {
    }

    public static void initialize(Stage stage) {

        primaryStage = stage;
        primaryStage.setTitle("JavaApp ERP");
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(700);

    }

    public static void showSplash() { load("/fxml/pages/login.fxml");
    }

    public static void showLogin() {load("/fxml/pages/Login.fxml");
    }
    public static void showRegistration() {load("/fxml/pages/Registration.fxml");}
    public static void showEmailSettings() {load("/fxml/pages/EmailSettings.fxml");}

    public static void showDashboard() {
        load("/fxml/pages/Dashboard.fxml");
    }

    private static void load(String fxml) {

        try {

            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxml));

            Parent root = loader.load();

            Scene scene = new Scene(root, WIDTH, HEIGHT);

            ThemeManager.applyTheme(scene);

            primaryStage.setScene(scene);

            primaryStage.centerOnScreen();

            primaryStage.show();

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

}
