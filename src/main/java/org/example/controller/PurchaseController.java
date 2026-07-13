package org.example.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.model.Item;
import org.example.model.Party;
import org.example.model.Purchase;
import org.example.model.PurchaseLine;
import org.example.service.ItemService;
import org.example.service.PartyService;
import org.example.service.PurchaseService;

import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;

import org.example.service.EmailService;
import org.example.service.InvoicePdfService;
import org.example.service.NotificationService;

public class PurchaseController {
    @FXML
    private TextField txtInvoiceNo, txtQuantity, txtRate;
    @FXML
    private DatePicker dpInvoiceDate;
    @FXML
    private ComboBox<Party> cmbSupplier;
    @FXML
    private ComboBox<Item> cmbItem;
    @FXML
    private TextArea txtRemarks;
    @FXML
    private CheckBox chkSendEmail;
    @FXML
    private TableView<PurchaseLine> tableLines;
    @FXML
    private TableColumn<PurchaseLine, String> colItem;
    @FXML
    private TableColumn<PurchaseLine, Double> colQuantity, colRate, colGst, colTotal;
    @FXML
    private Label lblSubtotal, lblGst, lblGrandTotal;
    private final ItemService itemService = new ItemService();
    private final PartyService partyService = new PartyService();
    private final PurchaseService purchaseService = new PurchaseService();
    private Purchase lastSavedPurchase;

    @FXML
    public void initialize() {
        colItem.setCellValueFactory(new PropertyValueFactory<>("itemDescription"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colRate.setCellValueFactory(new PropertyValueFactory<>("rate"));
        colGst.setCellValueFactory(new PropertyValueFactory<>("gstPercent"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("lineTotal"));
        cmbSupplier.setItems(FXCollections.observableArrayList(partyService.getByType("SUPPLIER")));
        cmbItem.setItems(FXCollections.observableArrayList(itemService.getAll()));
        cmbItem.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getItemCode() + " - " + item.getDescription());
            }
        });
        cmbItem.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getItemCode() + " - " + item.getDescription());
            }
        });
        cmbSupplier.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Party party, boolean empty) {
                super.updateItem(party, empty);
                setText(empty || party == null ? null : party.getPartyCode() + " - " + party.getName());
            }
        });
        cmbSupplier.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Party party, boolean empty) {
                super.updateItem(party, empty);
                setText(empty || party == null ? null : party.getPartyCode() + " - " + party.getName());
            }
        });
        newPurchase();
    }

    @FXML
    private void addLine() {
        Item item = cmbItem.getValue();
        if (item == null) {
            warn("Select an item.");
            return;
        }
        double quantity;
        double rate;
        try {
            quantity = Double.parseDouble(txtQuantity.getText());
            rate = Double.parseDouble(txtRate.getText());
        } catch (Exception exception) {
            warn("Enter valid quantity and rate.");
            return;
        }
        if (quantity <= 0 || rate < 0) {
            warn("Quantity must be positive and rate cannot be negative.");
            return;
        }
        PurchaseLine line = new PurchaseLine();
        line.setItemCode(item.getItemCode());
        line.setItemDescription(item.getItemCode() + " - " + item.getDescription());
        line.setQuantity(quantity);
        line.setRate(rate);
        line.setGstPercent(item.getGst());
        line.setLineTotal(quantity * rate * (1 + item.getGst() / 100));
        tableLines.getItems().add(line);
        cmbItem.setValue(null);
        txtQuantity.clear();
        txtRate.clear();
        recalculate();
    }

    @FXML
    private void removeLine() {
        PurchaseLine line = tableLines.getSelectionModel().getSelectedItem();
        if (line == null) {
            warn("Select a line to remove.");
            return;
        }
        tableLines.getItems().remove(line);
        recalculate();
    }

    @FXML
    private void savePurchase() {
        Purchase purchase = buildPurchase();
        if (purchase == null) return;
        try {
            purchaseService.save(purchase);
            lastSavedPurchase = purchase;
            if (chkSendEmail.isSelected()) {
                NotificationService.add("Purchase invoice " + purchase.getInvoiceNo() + " is waiting to be emailed.");
            }
            NotificationService.add("Purchase invoice " + purchase.getInvoiceNo() + " was saved.");
            new Alert(Alert.AlertType.INFORMATION, chkSendEmail.isSelected() ? "Purchase saved. Email is marked pending; click Send Invoice Email when ready." : "Purchase saved. Email was not requested.").showAndWait();
        } catch (Exception exception) {
            new Alert(Alert.AlertType.ERROR, exception.getMessage()).showAndWait();
        }
    }

    @FXML
    private void sendInvoiceEmail() {
        if (lastSavedPurchase == null) {
            warn("Save the purchase first, then send its invoice email.");
            return;
        }
        if (!chkSendEmail.isSelected()) {
            warn("Tick 'Email requested' before sending. This keeps email delivery under your control.");
            return;
        }
        String email = lastSavedPurchase.getSupplier().getEmail();
        if (email == null || email.isBlank()) {
            warn("The selected supplier does not have an email address.");
            return;
        }
        try {
            Path pdf = InvoicePdfService.purchase(lastSavedPurchase);
            EmailService.send(email, "Purchase invoice " + lastSavedPurchase.getInvoiceNo(), "Please find your purchase invoice attached.", pdf);
            Files.deleteIfExists(pdf);
            chkSendEmail.setText("Email sent ✓");
            chkSendEmail.setDisable(true);
            NotificationService.add("Purchase invoice " + lastSavedPurchase.getInvoiceNo() + " was emailed to " + email + ".");
            new Alert(Alert.AlertType.INFORMATION, "Invoice emailed to " + email + ".").showAndWait();
        } catch (Exception ex) {
            NotificationService.add("Purchase invoice " + lastSavedPurchase.getInvoiceNo() + " email could not be sent.");
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    private Purchase buildPurchase() {
        if (cmbSupplier.getValue() == null) {
            warn("Select a supplier.");
            return null;
        }
        if (tableLines.getItems().isEmpty()) {
            warn("Add at least one item.");
            return null;
        }
        Purchase purchase = new Purchase();
        purchase.setInvoiceNo(txtInvoiceNo.getText());
        purchase.setInvoiceDate(dpInvoiceDate.getValue());
        purchase.setSupplier(cmbSupplier.getValue());
        purchase.setLines(java.util.List.copyOf(tableLines.getItems()));
        double subtotal = tableLines.getItems().stream().mapToDouble(line -> line.getQuantity() * line.getRate()).sum();
        double total = tableLines.getItems().stream().mapToDouble(PurchaseLine::getLineTotal).sum();
        purchase.setSubtotal(subtotal);
        purchase.setGstAmount(total - subtotal);
        purchase.setTotalAmount(total);
        purchase.setRemarks(txtRemarks.getText().trim());
        return purchase;
    }

    @FXML
    private void newPurchase() {
        txtInvoiceNo.setText(purchaseService.nextInvoiceNo());
        dpInvoiceDate.setValue(LocalDate.now());
        cmbSupplier.setValue(null);
        cmbItem.setValue(null);
        txtQuantity.clear();
        txtRate.clear();
        txtRemarks.clear();
        chkSendEmail.setSelected(false);
        chkSendEmail.setDisable(false);
        chkSendEmail.setText("Email requested (send manually)");
        tableLines.getItems().clear();
        lastSavedPurchase = null;
        recalculate();
    }

    private void recalculate() {
        double subtotal = tableLines.getItems().stream().mapToDouble(line -> line.getQuantity() * line.getRate()).sum();
        double total = tableLines.getItems().stream().mapToDouble(PurchaseLine::getLineTotal).sum();
        lblSubtotal.setText(String.format("₹ %.2f", subtotal));
        lblGst.setText(String.format("₹ %.2f", total - subtotal));
        lblGrandTotal.setText(String.format("₹ %.2f", total));
    }

    private void warn(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
