package org.example.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.model.Lookup;
import org.example.service.LookupService;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;

public class MasterDataController {

    @FXML
    private ListView<String> lstTypes;

    @FXML
    private TableView<Lookup> tblLookup;

    @FXML
    private TableColumn<Lookup, String> colCode;

    @FXML
    private TableColumn<Lookup, String> colValue;

    @FXML
    private TableColumn<Lookup, String> colDescription;

    @FXML
    private TextField txtSearch;

    private final LookupService service = new LookupService();

    @FXML
    public void initialize() {

        // Table Columns
        colCode.setCellValueFactory(new PropertyValueFactory<>("lookupCode"));
        colValue.setCellValueFactory(new PropertyValueFactory<>("lookupValue"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Lookup Types
        lstTypes.getItems().addAll(
            "CATEGORY",
            "MATERIAL",
            "UNIT",
            "BRAND",
            "GST"
        );

        lstTypes.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldValue, newValue) -> loadTable()
        );

        lstTypes.getSelectionModel().selectFirst();

        txtSearch.textProperty().addListener((obs, oldValue, newValue) -> loadTable());

    }

    private void loadTable() {

        String type = lstTypes.getSelectionModel().getSelectedItem();

        if (type == null)
            return;

        String query = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        tblLookup.setItems(FXCollections.observableArrayList(service.getByType(type).stream()
            .filter(lookup -> query.isEmpty()
                || lookup.getLookupCode().toLowerCase(Locale.ROOT).contains(query)
                || lookup.getLookupValue().toLowerCase(Locale.ROOT).contains(query)
                || (lookup.getDescription() != null && lookup.getDescription().toLowerCase(Locale.ROOT).contains(query)))
            .toList()));

    }

    @FXML
    private void addLookup() {

        try {

            URL url = getClass().getResource("/fxml/pages/lookupDialog.fxml");

            if (url == null) {
                throw new RuntimeException("Itemdialog.fxml not found");
            }

            FXMLLoader loader = new FXMLLoader(url);

            Parent root = loader.load();

            LookupDialogController controller = loader.getController();

            controller.setLookupType(
                lstTypes.getSelectionModel().getSelectedItem()
            );

            Stage stage = new Stage();

            stage.initModality(Modality.APPLICATION_MODAL);

            stage.setTitle("Add Master");

            stage.setScene(new Scene(root));

            stage.sizeToScene();      // <-- Add this

            stage.setResizable(false);

            stage.showAndWait();

            loadTable();

        } catch (IOException ex) {

            ex.printStackTrace();

        }

    }

    @FXML
    private void editLookup() {
        Lookup selected = tblLookup.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a master record to edit.");
            return;
        }
        openDialog(selected);

    }

    @FXML
    private void deleteLookup() {
        Lookup selected = tblLookup.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a master record to delete.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete '" + selected.getLookupValue() + "'? This cannot be undone.",
            ButtonType.YES, ButtonType.NO);
        confirmation.setHeaderText("Confirm deletion");
        if (confirmation.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            service.delete(selected.getId());
            loadTable();
        }

    }

    @FXML
    private void refresh() {

        loadTable();

    }

    private void openDialog(Lookup lookup) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/pages/lookupDialog.fxml"));
            Parent root = loader.load();
            LookupDialogController controller = loader.getController();
            controller.setLookup(lookup);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Edit Master");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
            loadTable();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Could not open the edit dialog: " + ex.getMessage()).showAndWait();
        }
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

}
