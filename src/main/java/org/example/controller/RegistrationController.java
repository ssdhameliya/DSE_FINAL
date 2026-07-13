package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.model.AppUser;
import org.example.service.*;
import org.example.util.SceneManager;

public class RegistrationController {
    @FXML
    private TextField txtName, txtUsername, txtEmail, txtOtp;
    @FXML
    private PasswordField txtPassword, txtConfirm;
    @FXML
    private Label lblMessage;
    private final UserService users = new UserService();
    private AppUser pending;

    @FXML
    private void sendOtp() {
        if (txtName.getText().isBlank() || txtUsername.getText().isBlank() || txtEmail.getText().isBlank() || txtPassword.getText().isBlank() || !txtPassword.getText().equals(txtConfirm.getText())) {
            message("Complete all fields and make sure passwords match.", true);
            return;
        }
        pending = new AppUser();
        pending.setFullName(txtName.getText().trim());
        pending.setUsername(txtUsername.getText().trim());
        pending.setEmail(txtEmail.getText().trim());
        pending.setPassword(txtPassword.getText());
        try {
            EmailService.sendOtp(pending.getEmail(), OtpService.issue());
            message("OTP sent. Enter it to create your User account.", false);
        } catch (Exception e) {
            message(e.getMessage(), true);
        }
    }

    @FXML
    private void register() {
        if (pending == null) {
            message("Send the OTP first.", true);
            return;
        }
        if (!OtpService.verify(txtOtp.getText())) {
            message("The OTP is invalid or expired.", true);
            return;
        }
        try {
            users.register(pending);
            message("Registration complete. You can now sign in as USER.", false);
        } catch (Exception e) {
            message(e.getMessage(), true);
        }
    }

    @FXML
    private void back() {
        SceneManager.showLogin();
    }

    private void message(String t, boolean error) {
        lblMessage.setText(t);
        lblMessage.setStyle("-fx-text-fill:" + (error ? "#d32f2f" : "#2e7d32") + ";");
    }
}
