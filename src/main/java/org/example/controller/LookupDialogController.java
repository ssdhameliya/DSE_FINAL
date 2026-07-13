package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.model.Lookup;
import org.example.service.LookupService;

public class LookupDialogController {

    @FXML
    private TextField txtCode;

    @FXML
    private TextField txtValue;

    @FXML
    private TextArea txtDescription;

    @FXML
    private Spinner<Integer> spnOrder;

    @FXML
    private CheckBox chkActive;

    private final LookupService service = new LookupService();

    private String lookupType;
    private Lookup editingLookup;

    @FXML
    public void initialize() {

        spnOrder.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                1,
                999,
                1
            )
        );

    }

    public void setLookupType(String type) {

        this.lookupType = type;

        txtCode.setText(
            service.generateNextCode(type)
        );

    }

    /**
     * Prepares this dialog to update an existing master record.
     */
    public void setLookup(Lookup lookup) {
        this.editingLookup = lookup;
        this.lookupType = lookup.getLookupType();
        txtCode.setText(lookup.getLookupCode());
        txtValue.setText(lookup.getLookupValue());
        txtDescription.setText(lookup.getDescription());
        spnOrder.getValueFactory().setValue(lookup.getDisplayOrder());
        chkActive.setSelected(lookup.isActive());
    }

    @FXML
    private void save() {

        if (txtValue.getText().isBlank()) {

            Alert alert = new Alert(Alert.AlertType.WARNING);

            alert.setHeaderText(null);

            alert.setContentText("Value is required.");

            alert.show();

            return;

        }

        Lookup lookup = editingLookup == null ? new Lookup() : editingLookup;

        lookup.setLookupType(lookupType);
        lookup.setLookupCode(txtCode.getText());
        lookup.setLookupValue(txtValue.getText().trim());
        lookup.setDescription(txtDescription.getText().trim());
        lookup.setDisplayOrder(spnOrder.getValue());
        lookup.setActive(chkActive.isSelected());

        if (editingLookup == null) {
            service.save(lookup);
        } else {
            service.update(lookup);
        }

        close();

    }

    @FXML
    private void cancel() {

        close();

    }

    private void close() {

        Stage stage = (Stage) txtCode.getScene().getWindow();

        stage.close();

    }

}
