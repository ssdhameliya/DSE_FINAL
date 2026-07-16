package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import org.example.model.Item;
import org.example.service.ItemService;
import org.example.service.ItemSpreadsheetService;
import org.example.service.NotificationService;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Locale;

public class ItemMasterController {

    @FXML
    private TextField txtSearch;

    @FXML
    private TableView<Item> tableItems;
    @FXML
    private Label lblRecordCount;

    @FXML
    private TableColumn<Item, String> colCode;

    @FXML
    private TableColumn<Item, String> colDescription;

    @FXML
    private TableColumn<Item, String> colCategory;

    @FXML
    private TableColumn<Item, String> colBrand;

    @FXML
    private TableColumn<Item, Double> colStock;

    @FXML
    private TableColumn<Item, String> colUnit;

    @FXML
    private TableColumn<Item, Void> colAction;

    private final ItemService service = new ItemService();
    private final ItemSpreadsheetService spreadsheetService = new ItemSpreadsheetService();

    @FXML
    public void initialize() {
        colCode.setCellValueFactory(new PropertyValueFactory<>("itemCode"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colBrand.setCellValueFactory(new PropertyValueFactory<>("brand"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("openingStock"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        txtSearch.textProperty().addListener((obs, oldValue, newValue) -> loadItems());
        loadItems();

    }

    @FXML
    private void newItem() {

        try {

            URL url = getClass().getResource("/fxml/pages/Itemdialog.fxml");

            if (url == null) {
                throw new RuntimeException("Itemdialog.fxml not found");
            }

            FXMLLoader loader = new FXMLLoader(url);

            Parent root = loader.load();

            showDialog(loader, root, "Add New Item", null);

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

    @FXML
    private void editItem() {
        Item selected = tableItems.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select an item to edit.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/pages/Itemdialog.fxml"));
            Parent root = loader.load();
            showDialog(loader, root, "Edit Item", selected);
        } catch (Exception e) {
            showError("Could not open the item dialog: " + e.getMessage());
        }
    }

    @FXML
    private void deleteItem() {
        Item selected = tableItems.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select an item to delete.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete item '" + selected.getDescription() + "'? This cannot be undone.",
            ButtonType.YES, ButtonType.NO);
        confirmation.setHeaderText("Confirm deletion");
        if (confirmation.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            service.delete(selected.getItemCode());
            NotificationService.add("Item '" + selected.getDescription() + "' was deleted.");
            loadItems();
        }
    }

    @FXML
    private void refresh() {
        loadItems();
    }

    @FXML
    private void importItems() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Item Master");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook", "*.xlsx", "*.xls"));
        File file = chooser.showOpenDialog(tableItems.getScene().getWindow());
        if (file == null) return;
        try {
            ItemSpreadsheetService.ImportResult result = spreadsheetService.importItems(file.toPath());
            NotificationService.add(result.imported() + " item(s) were imported or updated.");
            loadItems();
            String message = result.imported() + " item(s) imported or updated.";
            if (result.hasErrors())
                message += "\n\nSkipped rows:\n" + String.join("\n", result.errors().stream().limit(8).toList());
            Alert alert = new Alert(result.hasErrors() ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION, message);
            alert.setHeaderText("Item import complete");
            alert.showAndWait();
        } catch (Exception ex) {
            showError("Could not import the workbook: " + ex.getMessage());
        }
    }

    @FXML
    private void exportItems() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Item Master");
        chooser.setInitialFileName("item-master.xlsx");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook", "*.xlsx"));
        File file = chooser.showSaveDialog(tableItems.getScene().getWindow());
        if (file == null) return;
        Path path = file.toPath();
        if (!path.toString().toLowerCase(Locale.ROOT).endsWith(".xlsx")) path = Path.of(path + ".xlsx");
        try {
            spreadsheetService.exportItems(service.getAll(), path);
            new Alert(Alert.AlertType.INFORMATION, "Item master exported to:\n" + path).showAndWait();
        } catch (Exception ex) {
            showError("Could not export the workbook: " + ex.getMessage());
        }
    }

    private void showDialog(FXMLLoader loader, Parent root, String title, Item item) {
        ItemDialogController controller = loader.getController();
        if (item != null) {
            controller.setItem(item);
        }
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.setResizable(false);
        stage.showAndWait();
        loadItems();
    }

    private void loadItems() {
        String query = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        tableItems.getItems().setAll(service.getAll().stream()
            .filter(item -> query.isEmpty()
                || item.getItemCode().toLowerCase(Locale.ROOT).contains(query)
                || item.getDescription().toLowerCase(Locale.ROOT).contains(query)
                || (item.getCategory() != null && item.getCategory().toLowerCase(Locale.ROOT).contains(query))
                || (item.getBrand() != null && item.getBrand().toLowerCase(Locale.ROOT).contains(query)))
            .toList());
        lblRecordCount.setText("Showing " + tableItems.getItems().size() + " Record" + (tableItems.getItems().size() == 1 ? "" : "s"));
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }


}
