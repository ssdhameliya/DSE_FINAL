package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.model.Party;
import org.example.service.PartyService;

import java.util.Locale;

public abstract class PartyMasterController {
    @FXML
    protected TextField txtSearch;
    @FXML
    protected TableView<Party> tableParties;
    @FXML
    protected TableColumn<Party, String> colCode, colName, colContact, colPhone, colEmail, colGstin, colAddress;
    @FXML
    protected TableColumn<Party, Double> colOpeningBalance;
    @FXML
    protected TableColumn<Party, Boolean> colActive;
    @FXML
    protected Label lblRecordCount;
    private final PartyService service = new PartyService();

    protected abstract String partyType();

    protected abstract String displayName();

    @FXML
    public void initialize() {
        colCode.setCellValueFactory(new PropertyValueFactory<>("partyCode"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colContact.setCellValueFactory(new PropertyValueFactory<>("contactPerson"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colGstin.setCellValueFactory(new PropertyValueFactory<>("gstin"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colOpeningBalance.setCellValueFactory(new PropertyValueFactory<>("openingBalance"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));
        tableParties.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        txtSearch.textProperty().addListener((o, oldValue, newValue) -> load());
        load();
    }

    @FXML
    protected void newParty() {
        open(null);
    }

    @FXML
    protected void editParty() {
        Party party = tableParties.getSelectionModel().getSelectedItem();
        if (party == null) {
            warning("Select a " + displayName().toLowerCase() + " to edit.");
            return;
        }
        open(party);
    }

    @FXML
    protected void deleteParty() {
        Party party = tableParties.getSelectionModel().getSelectedItem();
        if (party == null) {
            warning("Select a " + displayName().toLowerCase() + " to delete.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Delete '" + party.getName() + "'? This cannot be undone.", ButtonType.YES, ButtonType.NO);
        confirmation.setHeaderText("Confirm deletion");
        if (confirmation.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            service.delete(party.getId());
            load();
        }
    }

    @FXML
    protected void refresh() {
        load();
    }

    private void open(Party party) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/pages/PartyDialog.fxml"));
            Parent root = loader.load();
            PartyDialogController controller = loader.getController();
            controller.configure(partyType(), party);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle((party == null ? "Add " : "Edit ") + displayName());
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
            load();
        } catch (Exception exception) {
            error("Could not open the dialog: " + exception.getMessage());
        }
    }

    private void load() {
        String query = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        tableParties.getItems().setAll(service.getByType(partyType()).stream().filter(p -> query.isEmpty() || p.getPartyCode().toLowerCase(Locale.ROOT).contains(query) || p.getName().toLowerCase(Locale.ROOT).contains(query) || (p.getPhone() != null && p.getPhone().contains(query))).toList());
        int count = tableParties.getItems().size();
        lblRecordCount.setText("Showing " + count + " Record" + (count == 1 ? "" : "s"));
    }

    private void warning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void error(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
