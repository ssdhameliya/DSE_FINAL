package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.model.Item;
import org.example.service.ItemService;
import org.example.service.ItemSpreadsheetService;
import org.example.service.NotificationService;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class ItemMasterController {

    @FXML private TextField txtSearch;
    @FXML private TableView<Item> tableItems;
    @FXML private Label lblRecordCount;


    @FXML private TableColumn<Item, String> colCode;
    @FXML private TableColumn<Item, String> colDescription;
    @FXML private TableColumn<Item, String> colCategory;
    @FXML private TableColumn<Item, String> colBrand;
    @FXML private TableColumn<Item, String> colMaterial;
    @FXML private TableColumn<Item, String> colSize;
    @FXML private TableColumn<Item, String> colUnit;
    @FXML private TableColumn<Item, String> colHsn;
    @FXML private TableColumn<Item, Double> colGst;
    @FXML private TableColumn<Item, Double> colPurchasePrice;
    @FXML private TableColumn<Item, Double> colSellingPrice;
    @FXML private TableColumn<Item, Double> colOpeningStock;
    @FXML private TableColumn<Item, Double> colMinimumStock;
    @FXML private TableColumn<Item, String> colLocation;
    @FXML private TableColumn<Item, String> colRemarks;
    @FXML private TableColumn<Item, Void> colAction;

    private final ObservableList<Item> items = FXCollections.observableArrayList();
    private final ItemService service = new ItemService();
    private final ItemSpreadsheetService spreadsheetService = new ItemSpreadsheetService();

    @FXML
    public void initialize() {
        // Column bindings

        colCode.setCellValueFactory(new PropertyValueFactory<>("itemCode"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colBrand.setCellValueFactory(new PropertyValueFactory<>("brand"));
        colMaterial.setCellValueFactory(new PropertyValueFactory<>("material"));
        colSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colHsn.setCellValueFactory(new PropertyValueFactory<>("hsn"));
        colGst.setCellValueFactory(new PropertyValueFactory<>("gst"));
        colGst.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f", v));
            }
        });

        colPurchasePrice.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        colPurchasePrice.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : "₹" + String.format("%,.2f", v));
            }
        });

        colSellingPrice.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        colSellingPrice.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : "₹" + String.format("%,.2f", v));
            }
        });

        colOpeningStock.setCellValueFactory(new PropertyValueFactory<>("openingStock"));
        colOpeningStock.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%,.3f", v));
            }
        });

        colMinimumStock.setCellValueFactory(new PropertyValueFactory<>("minimumStock"));
        colMinimumStock.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%,.3f", v));
            }
        });

        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));

        colRemarks.setCellValueFactory(new PropertyValueFactory<>("remarks"));
        colRemarks.setCellFactory(tc -> {
            TableCell<Item, String> cell = new TableCell<>() {
                @Override protected void updateItem(String v, boolean empty) {
                    super.updateItem(v, empty);
                    setText(empty || v == null ? null : v);
                    setWrapText(true);
                }
            };
            return cell;
        });

        // Action column (Edit / Delete)
        colAction.setCellFactory(tc -> new TableCell<>() {
            private final Button btnEdit =
                new Button("✏");


            private final Button btnDelete =
                new Button("🗑");





            {
                btnEdit.getStyleClass().add("small-button");
                btnDelete.getStyleClass().add("small-button-danger");
                btnEdit.setOnAction(e -> {
                    Item item = getTableView().getItems().get(getIndex());
                    openItemDialog(item);
                });
                btnEdit.setTooltip(new Tooltip("Edit Item"));
                btnDelete.setOnAction(e -> {
                    Item item = getTableView().getItems().get(getIndex());
                    deleteItem(item);

                });
                btnDelete.setTooltip(new Tooltip("Delete Item"));
            }

            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) setGraphic(null);
                else setGraphic(new HBox(6, btnEdit, btnDelete));
            }
        });

        tableItems.setItems(items);

        // Search listener
        txtSearch.textProperty().addListener((obs, oldValue, newValue) -> loadItems());

        // initial load
        loadItems();
    }

    private void openItemDialog(Item item) {
        try {
            URL url = getClass().getResource("/fxml/pages/Itemdialog.fxml");
            if (url == null) throw new RuntimeException("Itemdialog.fxml not found");
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            ItemDialogController controller = loader.getController();
            if (item != null) controller.setItem(item);
            Stage stage = new Stage();
            stage.setTitle(item == null ? "Add New Item" : "Edit Item");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
            loadItems();
        } catch (Exception e) {
            showError("Could not open the item dialog: " + e.getMessage());
        }
    }

    @FXML
    private void newItem() { openItemDialog(null); }

    @FXML
    private void editItem() {
        Item selected = tableItems.getSelectionModel().getSelectedItem();
        if (selected == null) { showWarning("Select an item to edit."); return; }
        openItemDialog(selected);
    }

    private void deleteItem(Item selected) {
        if (selected == null) { showWarning("Select an item to delete."); return; }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete item '" + selected.getDescription() + "'? This cannot be undone.",
            ButtonType.YES, ButtonType.NO);
        confirmation.setHeaderText("Confirm deletion");
        if (confirmation.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                service.delete(selected.getItemCode());
                NotificationService.add("Item '" + selected.getDescription() + "' was deleted.");
                loadItems();
            } catch (Exception e) {
                showError("Could not delete item: " + e.getMessage());
            }
        }
    }

    @FXML
    private void deleteItem() { // keep compatibility with FXML onAction
        Item selected = tableItems.getSelectionModel().getSelectedItem();
        deleteItem(selected);
    }

    @FXML
    private void refresh() { loadItems(); }

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

    private void loadItems() {
        String query = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        try {
            List<Item> list = service.getAll();
            items.setAll(list.stream()
                .filter(item -> query.isEmpty()
                    || (item.getItemCode() != null && item.getItemCode().toLowerCase(Locale.ROOT).contains(query))
                    || (item.getDescription() != null && item.getDescription().toLowerCase(Locale.ROOT).contains(query))
                    || (item.getCategory() != null && item.getCategory().toLowerCase(Locale.ROOT).contains(query))
                    || (item.getBrand() != null && item.getBrand().toLowerCase(Locale.ROOT).contains(query)))
                .toList());
            lblRecordCount.setText("Showing " + items.size() + " Record" + (items.size() == 1 ? "" : "s"));
        } catch (Exception e) {
            showError("Could not load items: " + e.getMessage());
        }
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
