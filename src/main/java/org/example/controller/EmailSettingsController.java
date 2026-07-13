package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.config.ConfigManager;
import org.example.util.SceneManager;

public class EmailSettingsController {
    @FXML
    private TextField txtSmtpEmail;
    @FXML
    private PasswordField txtSmtpPassword;

    @FXML
    public void initialize() {
        txtSmtpEmail.setText(ConfigManager.get("smtp.email", "shailesh.rockstar007@yahoo.com"));
        txtSmtpPassword.setText(ConfigManager.get("smtp.appPassword", ""));
    }

    @FXML
    private void save() {
        if (txtSmtpEmail.getText().isBlank() || txtSmtpPassword.getText().isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Enter the Yahoo email and its app password.").showAndWait();
            return;
        }
        ConfigManager.set("smtp.email", txtSmtpEmail.getText().trim());
        ConfigManager.set("smtp.appPassword", txtSmtpPassword.getText());
        new Alert(Alert.AlertType.INFORMATION, "Email settings saved. You can now sign in or register.").showAndWait();
        SceneManager.showLogin();
    }

    @FXML
    private void back() {
        SceneManager.showLogin();
    }
}
