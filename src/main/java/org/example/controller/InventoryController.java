package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.model.Item;
import org.example.service.ItemService;
import org.example.service.NotificationService;

import java.util.Locale;

public class InventoryController {
    @FXML
    private TextField txtSearch;
    @FXML
    private CheckBox chkLowStock;
    @FXML
    private TableView<Item> tableItems;
    @FXML
    private TableColumn<Item, String> colCode, colDescription, colCategory, colUnit;
    @FXML
    private TableColumn<Item, Double> colStock, colMinimum;
    @FXML
    private Label lblSummary;
    private final ItemService service = new ItemService();
    private boolean lowStockNotified;

    @FXML
    public void initialize() {
        colCode.setCellValueFactory(new PropertyValueFactory<>("itemCode"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("openingStock"));
        colMinimum.setCellValueFactory(new PropertyValueFactory<>("minimumStock"));
        txtSearch.textProperty().addListener((o, a, b) -> load());
        chkLowStock.selectedProperty().addListener((o, a, b) -> load());
        load();
    }

    @FXML
    private void refresh() {
        load();
    }

    private void load() {
        String q = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        tableItems.getItems().setAll(service.getAll().stream().filter(i -> (q.isEmpty() || i.getItemCode().toLowerCase().contains(q) || i.getDescription().toLowerCase().contains(q)) && (!chkLowStock.isSelected() || i.getOpeningStock() <= i.getMinimumStock())).toList());
        long low = service.getAll().stream().filter(i -> i.getOpeningStock() <= i.getMinimumStock()).count();
        lblSummary.setText("Items: " + tableItems.getItems().size() + " | Low stock: " + low);
        if (low > 0 && !lowStockNotified) {
            NotificationService.add(low + " item(s) are at or below their minimum stock level.");
            lowStockNotified = true;
        }
    }
}
