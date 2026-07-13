package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.model.Party;
import org.example.service.PartyService;

public class PartyDialogController {
    @FXML
    private Label lblTitle;
    @FXML
    private TextField txtCode, txtName, txtContact, txtPhone, txtEmail, txtGstin, txtOpeningBalance;
    @FXML
    private TextArea txtAddress;
    @FXML
    private CheckBox chkActive;
    private final PartyService service = new PartyService();
    private String type;
    private Party editing;

    public void configure(String type, Party party) {
        this.type = type;
        this.editing = party;
        String title = type.equals("CUSTOMER") ? "Customer" : "Supplier";
        lblTitle.setText((party == null ? "Add " : "Edit ") + title);
        if (party == null) {
            txtCode.setText(service.nextCode(type));
            chkActive.setSelected(true);
        } else {
            txtCode.setText(party.getPartyCode());
            txtName.setText(party.getName());
            txtContact.setText(party.getContactPerson());
            txtPhone.setText(party.getPhone());
            txtEmail.setText(party.getEmail());
            txtGstin.setText(party.getGstin());
            txtAddress.setText(party.getAddress());
            txtOpeningBalance.setText(String.valueOf(party.getOpeningBalance()));
            chkActive.setSelected(party.isActive());
        }
    }

    @FXML
    private void save() {
        if (txtName.getText().isBlank()) {
            warn("Name is required.");
            return;
        }
        Party party = editing == null ? new Party() : editing;
        party.setPartyType(type);
        party.setPartyCode(txtCode.getText());
        party.setName(txtName.getText().trim());
        party.setContactPerson(txtContact.getText().trim());
        party.setPhone(txtPhone.getText().trim());
        party.setEmail(txtEmail.getText().trim());
        party.setGstin(txtGstin.getText().trim());
        party.setAddress(txtAddress.getText().trim());
        try {
            party.setOpeningBalance(txtOpeningBalance.getText().isBlank() ? 0 : Double.parseDouble(txtOpeningBalance.getText().trim()));
        } catch (NumberFormatException exception) {
            warn("Opening balance must be a number.");
            return;
        }
        party.setActive(chkActive.isSelected());
        if (editing == null) service.save(party);
        else service.update(party);
        close();
    }

    @FXML
    private void cancel() {
        close();
    }

    private void close() {
        ((Stage) txtCode.getScene().getWindow()).close();
    }

    private void warn(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
