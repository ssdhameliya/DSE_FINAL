package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import org.example.navigation.NavigationManager;
import org.example.theme.ThemeManager;
import org.example.util.ClockService;
import org.example.service.ItemService;
import org.example.service.PartyService;
import org.example.model.Item;
import org.example.model.Party;

import java.util.List;
import java.util.Locale;

import org.example.service.SessionService;
import org.example.service.UserService;
import org.example.service.NotificationService;

public class DashboardController {


    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnMasters;

    @FXML
    private Button btnInventory;

    @FXML
    private Button btnPurchase;

    @FXML
    private Button btnSales;

    @FXML
    private Button btnReports;

    @FXML
    private Button btnSettings;

    @FXML
    private Label lblPageTitle;

    @FXML
    private Button btnItem;

    @FXML
    private Button btnCustomer;

    @FXML
    private Button btnSupplier;

    @FXML
    private Label lblClock;

    @FXML
    private TextField txtSearch;

    @FXML
    private Button btnTheme;

    @FXML
    private MenuButton menuUser;

    public void initialize() {

        ClockService.start(lblClock);


        navigationManager = new NavigationManager(contentPane);

        navigationManager.loadPage("/fxml/pages/DashboardHome.fxml");

        lblPageTitle.setText("Dashboard");

        btnDashboard.getStyleClass().add("menu-selected");
        if (SessionService.current() != null) {
            menuUser.setText("👤 " + SessionService.current().getFullName());
        }

    }

    @FXML
    private StackPane contentPane;

    private NavigationManager navigationManager;
    private final ItemService itemService = new ItemService();
    private final PartyService partyService = new PartyService();


    @FXML
    private void toggleTheme() {

        ThemeManager.toggle(btnTheme.getScene());

    }


    private void clearSelection() {

        if (btnDashboard != null)
            btnDashboard.getStyleClass().remove("menu-selected");

        if (btnItem != null)
            btnItem.getStyleClass().remove("menu-selected");

        if (btnMasters != null)
            btnMasters.getStyleClass().remove("menu-selected");

        if (btnCustomer != null)
            btnCustomer.getStyleClass().remove("menu-selected");

        if (btnSupplier != null)
            btnSupplier.getStyleClass().remove("menu-selected");

        if (btnInventory != null)
            btnInventory.getStyleClass().remove("menu-selected");

        if (btnPurchase != null)
            btnPurchase.getStyleClass().remove("menu-selected");

        if (btnSales != null)
            btnSales.getStyleClass().remove("menu-selected");

        if (btnReports != null)
            btnReports.getStyleClass().remove("menu-selected");

        if (btnSettings != null)
            btnSettings.getStyleClass().remove("menu-selected");
    }

    private void openPage(Button button,
                          String pageTitle,
                          String fxmlPath) {

        clearSelection();

        if (button != null) {
            button.getStyleClass().add("menu-selected");
        }

        lblPageTitle.setText(pageTitle);

        navigationManager.loadPage(fxmlPath);
    }

    @FXML
    private void openDashboard() {

        openPage(
            btnDashboard,
            "Dashboard",
            "/fxml/pages/DashboardHome.fxml"
        );

    }

    @FXML
    private void openItemMaster() {

        openPage(
            btnItem,
            "Item Master",
            "/fxml/pages/ItemMaster.fxml"
        );

    }

    @FXML
    private void openCustomers() {

        openPage(
            btnCustomer,
            "Customers",
            "/fxml/pages/Customer.fxml"
        );

    }

    @FXML
    private void openSupplier() {

        openPage(btnSupplier,
            "Suppliers",
            "/fxml/pages/Suppliers.fxml");

    }

    @FXML
    private void openInventory() {

        openPage(btnInventory,
            "Inventory",
            "/fxml/pages/Inventory.fxml");

    }

    @FXML
    private void openPurchase() {
        openPage(btnPurchase,
            "Purchase",
            "/fxml/pages/Purchase.fxml");

    }

    @FXML
    private void openSales() {
        openPage(btnSales,
            "OpenSales",
            "/fxml/pages/Opensales.fxml");

    }

    @FXML
    private void openReports() {
        openPage(btnReports,
            "Reports",
            "/fxml/pages/Reports.fxml");

    }

    @FXML
    private void openSettings() {
        openPage(btnSettings,
            "Settings",
            "/fxml/pages/Settings.fxml");

    }

    @FXML
    private void openMasters() {

        openPage(
            btnMasters,
            "Masters",
            "/fxml/pages/MasterData.fxml"
        );

    }

    @FXML
    private void search() {
        String query = txtSearch.getText() == null ? "" : txtSearch.getText().trim();
        if (query.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Enter an item, customer, or supplier name to search.").showAndWait();
            return;
        }
        String normalized = query.toLowerCase(Locale.ROOT);
        List<Item> items = itemService.getAll().stream().filter(item ->
            item.getItemCode().toLowerCase(Locale.ROOT).contains(normalized)
                || item.getDescription().toLowerCase(Locale.ROOT).contains(normalized)).toList();
        List<Party> customers = partyService.getByType("CUSTOMER").stream().filter(party ->
            party.getName().toLowerCase(Locale.ROOT).contains(normalized)
                || party.getPartyCode().toLowerCase(Locale.ROOT).contains(normalized)).toList();
        List<Party> suppliers = partyService.getByType("SUPPLIER").stream().filter(party ->
            party.getName().toLowerCase(Locale.ROOT).contains(normalized)
                || party.getPartyCode().toLowerCase(Locale.ROOT).contains(normalized)).toList();
        String result = "Items: " + items.size() + "\nCustomers: " + customers.size() + "\nSuppliers: " + suppliers.size();
        Alert alert = new Alert(Alert.AlertType.INFORMATION, result);
        alert.setHeaderText("Search results for '" + query + "'");
        alert.showAndWait();
    }

    @FXML
    private void showNotifications() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Notifications");
        dialog.setHeaderText("Recent application activity");

        ListView<String> notificationList = new ListView<>();
        notificationList.getItems().setAll(NotificationService.getAll());
        notificationList.setPrefSize(620, 360);

        ButtonType clearButton = new ButtonType("Clear all");
        dialog.getDialogPane().setContent(notificationList);
        dialog.getDialogPane().getButtonTypes().addAll(clearButton, ButtonType.CLOSE);
        dialog.showAndWait().ifPresent(button -> {
            if (button == clearButton) {
                NotificationService.clear();
            }
        });
    }

    @FXML
    private void showProfile() {
        openPage(null, "My Profile", "/fxml/pages/Profile.fxml");
    }

    @FXML
    private void changePassword() {
        if (SessionService.current() == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Change Password");
        dialog.setHeaderText("Set a new password");

        PasswordField newPassword = new PasswordField();
        PasswordField confirmPassword = new PasswordField();
        newPassword.setPromptText("New password");
        confirmPassword.setPromptText("Confirm new password");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("New password:"), newPassword);
        form.addRow(1, new Label("Confirm password:"), confirmPassword);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().filter(button -> button == ButtonType.OK).ifPresent(button -> {
            String password = newPassword.getText();
            if (password.isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Password cannot be empty.").showAndWait();
                return;
            }
            if (!password.equals(confirmPassword.getText())) {
                new Alert(Alert.AlertType.WARNING, "The passwords do not match.").showAndWait();
                return;
            }

            new UserService().changePassword(SessionService.current().getId(), password);
            NotificationService.add("Your account password was changed.");
            new Alert(Alert.AlertType.INFORMATION, "Password changed successfully.").showAndWait();
        });
    }

    @FXML
    private void logout() {
        SessionService.clear();
        org.example.util.SceneManager.showLogin();
    }

}
