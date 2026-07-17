    package org.example.controller;

    import javafx.fxml.FXML;
    import javafx.scene.control.Label;
    import javafx.scene.control.PasswordField;
    import javafx.scene.control.TextField;
    import javafx.scene.control.ToggleButton;
    import org.example.model.AppUser;
    import org.example.service.EmailService;
    import org.example.service.NotificationService;
    import org.example.service.OtpService;
    import org.example.service.SessionService;
    import org.example.service.UserService;
    import org.example.theme.ThemeManager;
    import org.example.util.ClockService;
    import org.example.util.SceneManager;

    public class LoginController {

        @FXML
        private TextField txtUsername;
        @FXML
        private TextField txtOtp;
        @FXML
        private PasswordField txtPassword;
        @FXML
        private ToggleButton btnTheme;
        @FXML
        private Label lblClock;
        @FXML
        private Label lblMessage;

        private final UserService users = new UserService();
        private AppUser pendingUser;

        @FXML
        public void initialize() {
            ClockService.start(lblClock);
            txtOtp.setDisable(true);
        }

        @FXML
        private void toggleTheme() {
            ThemeManager.toggle(btnTheme.getScene());
        }

        @FXML
        private void login() {
            if (pendingUser == null) {
                authenticateUser();
            } else {
                verifyOtp();
            }
        }

        private void authenticateUser() {
            String identity = txtUsername.getText().trim();
            String password = txtPassword.getText();

            if (identity.isBlank() || password.isBlank()) {
                message("Enter your email or username and password.", true);
                return;
            }

            pendingUser = users.authenticate(identity, password);
            if (pendingUser == null) {
                message("Invalid email/username or password.", true);
                return;
            }

            try {
                EmailService.sendOtp(pendingUser.getEmail(), OtpService.issue());
                txtOtp.setDisable(false);
                message("Verification code sent to " + pendingUser.getEmail() + ". Enter it and click Login again.", false);
            } catch (Exception exception) {
                pendingUser = null;
                message(exception.getMessage(), true);
            }
        }

        private void verifyOtp() {
            if (!OtpService.verify(txtOtp.getText())) {
                message("The verification code is invalid or expired.", true);
                return;
            }

            SessionService.signIn(pendingUser);
            NotificationService.add("Signed in successfully.");
            SceneManager.showDashboard();
        }

        @FXML
        private void register() {
            SceneManager.showRegistration();
        }

        private void message(String text, boolean error) {
            lblMessage.setText(text);
            lblMessage.setStyle("-fx-text-fill:" + (error ? "#d32f2f" : "#2e7d32") + ";");
        }
    }
