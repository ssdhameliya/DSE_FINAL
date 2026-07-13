package org.example.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;

public class NavigationManager {

    private final StackPane contentPane;

    public NavigationManager(StackPane contentPane) {
        this.contentPane = contentPane;
    }

    public void loadPage(String fxml) {

        try {

            URL url = getClass().getResource(fxml);

            if (url == null) {
                throw new RuntimeException("FXML not found: " + fxml);
            }

            FXMLLoader loader = new FXMLLoader(url);

            Node page = loader.load();

            contentPane.getChildren().setAll(page);

            contentPane.getChildren().setAll(page);

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

}