package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.config.ConfigManager;
import org.example.service.NotificationService;

/**
 * Settings entered here are persisted locally; no source-file configuration is required.
 */
public class SettingsController {

    @FXML
    private TextField txtCompanyName;
    @FXML
    private TextField txtPhone;
    @FXML
    private TextField txtEmail;
    @FXML
    private TextField txtGstin;
    @FXML
    private TextField txtSmtpEmail;
    @FXML
    private PasswordField txtSmtpPassword;
    @FXML
    private CheckBox chkNotifications;

    @FXML
    public void initialize() {
        txtCompanyName.setText(ConfigManager.get("company.name", ""));
        txtPhone.setText(ConfigManager.get("company.phone", ""));
        txtEmail.setText(ConfigManager.get("company.email", ""));
        txtGstin.setText(ConfigManager.get("company.gstin", ""));
        txtSmtpEmail.setText(ConfigManager.get("smtp.email", ""));
        txtSmtpPassword.setText(ConfigManager.get("smtp.appPassword", ""));
        chkNotifications.setSelected(NotificationService.isEnabled());
    }

    @FXML
    private void save() {
        ConfigManager.set("company.name", txtCompanyName.getText().trim());
        ConfigManager.set("company.phone", txtPhone.getText().trim());
        ConfigManager.set("company.email", txtEmail.getText().trim());
        ConfigManager.set("company.gstin", txtGstin.getText().trim());
        ConfigManager.set("smtp.email", txtSmtpEmail.getText().trim());
        ConfigManager.set("smtp.appPassword", txtSmtpPassword.getText());
        NotificationService.setEnabled(chkNotifications.isSelected());

        if (chkNotifications.isSelected()) {
            NotificationService.add("Application settings were updated.");
        }

        new Alert(Alert.AlertType.INFORMATION,
            "Settings saved. Email delivery will use the address and app password entered here.")
            .showAndWait();
    }
}
