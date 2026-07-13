package org.example.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.model.Item;
import org.example.service.ItemService;
import org.example.service.LookupService;

public class ItemDialogController {

    @FXML
    private TextField txtItemCode;
    @FXML
    private TextField txtDescription;
    @FXML
    private TextField txtBrand;
    @FXML
    private TextField txtSize;
    @FXML
    private TextField txtHSN;
    @FXML
    private TextField txtGST;
    @FXML
    private TextField txtPurchasePrice;
    @FXML
    private TextField txtSellingPrice;
    @FXML
    private TextField txtOpeningStock;
    @FXML
    private TextField txtMinimumStock;
    @FXML
    private TextField txtLocation;

    private final LookupService lookupService = new LookupService();

    @FXML
    private TextArea txtRemarks;

    @FXML
    private ComboBox<String> cmbCategory;
    @FXML
    private ComboBox<String> cmbMaterial;
    @FXML
    private ComboBox<String> cmbUnit;

    private final ItemService service = new ItemService();
    private Item editingItem;

    @FXML
    public void initialize() {

        loadDropdowns();

        txtItemCode.setText(generateItemCode());

    }

    private void loadDropdowns() {

        cmbCategory.getItems().setAll(
            lookupService.getValues("CATEGORY")
        );

        cmbMaterial.getItems().setAll(
            lookupService.getValues("MATERIAL")
        );

        cmbUnit.getItems().setAll(
            lookupService.getValues("UNIT")
        );

    }

    private String generateItemCode() {

        return "ITM" + System.currentTimeMillis();

    }

    /**
     * Sets the form fields for editing without generating a new item code.
     */
    public void setItem(Item item) {
        this.editingItem = item;
        txtItemCode.setText(item.getItemCode());
        txtDescription.setText(item.getDescription());
        cmbCategory.setValue(item.getCategory());
        txtBrand.setText(item.getBrand());
        cmbMaterial.setValue(item.getMaterial());
        txtSize.setText(item.getSize());
        cmbUnit.setValue(item.getUnit());
        txtHSN.setText(item.getHsn());
        txtGST.setText(String.valueOf(item.getGst()));
        txtPurchasePrice.setText(String.valueOf(item.getPurchasePrice()));
        txtSellingPrice.setText(String.valueOf(item.getSellingPrice()));
        txtOpeningStock.setText(String.valueOf(item.getOpeningStock()));
        txtMinimumStock.setText(String.valueOf(item.getMinimumStock()));
        txtLocation.setText(item.getLocation());
        txtRemarks.setText(item.getRemarks());
    }

    @FXML
    private void saveItem() {

        if (!validate()) {
            return;
        }

        try {

            Item item = editingItem == null ? new Item() : editingItem;

            item.setItemCode(txtItemCode.getText());

            item.setDescription(txtDescription.getText());

            item.setCategory(cmbCategory.getValue());

            item.setBrand(txtBrand.getText());

            item.setMaterial(cmbMaterial.getValue());

            item.setSize(txtSize.getText());

            item.setUnit(cmbUnit.getValue());

            item.setHsn(txtHSN.getText());

            item.setGst(parseDouble(txtGST));

            item.setPurchasePrice(parseDouble(txtPurchasePrice));

            item.setSellingPrice(parseDouble(txtSellingPrice));

            item.setOpeningStock(parseDouble(txtOpeningStock));

            item.setMinimumStock(parseDouble(txtMinimumStock));

            item.setLocation(txtLocation.getText());

            item.setRemarks(txtRemarks.getText());

            if (editingItem == null) {
                service.save(item);
            } else {
                service.update(item);
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);

            alert.setHeaderText(null);

            alert.setTitle("Success");

            alert.setContentText(editingItem == null ? "Item saved successfully." : "Item updated successfully.");

            alert.showAndWait();

            closeDialog();

        } catch (Exception ex) {

            ex.printStackTrace();

            new Alert(Alert.AlertType.ERROR,
                ex.getMessage()).showAndWait();

        }

    }

    private boolean validate() {

        if (txtDescription.getText().isBlank()) {

            showError("Description is required.");

            return false;

        }

        if (cmbCategory.getValue() == null) {

            showError("Select Category.");

            return false;

        }

        if (cmbUnit.getValue() == null) {

            showError("Select Unit.");

            return false;

        }

        return true;

    }

    private double parseDouble(TextField tf) {

        if (tf.getText().isBlank())
            return 0;

        try {
            return Double.parseDouble(tf.getText().trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Enter a valid number for " + tf.getId().replace("txt", "") + ".");
        }

    }

    private void showError(String msg) {

        Alert alert = new Alert(Alert.AlertType.WARNING);

        alert.setHeaderText(null);

        alert.setTitle("Validation");

        alert.setContentText(msg);

        alert.showAndWait();

    }

    @FXML
    private void closeDialog() {

        Stage stage = (Stage) txtItemCode.getScene().getWindow();

        stage.close();

    }


}
