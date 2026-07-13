package org.example.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.model.*;
import org.example.service.*;

import java.time.LocalDate;
import java.nio.file.*;

public class SalesController {
    @FXML
    private TextField txtInvoiceNo, txtQuantity, txtRate;
    @FXML
    private DatePicker dpInvoiceDate;
    @FXML
    private ComboBox<Party> cmbCustomer;
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
    private final ItemService items = new ItemService();
    private final PartyService parties = new PartyService();
    private final SalesService sales = new SalesService();
    private Sales lastSavedSale;

    @FXML
    public void initialize() {
        colItem.setCellValueFactory(new PropertyValueFactory<>("itemDescription"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colRate.setCellValueFactory(new PropertyValueFactory<>("rate"));
        colGst.setCellValueFactory(new PropertyValueFactory<>("gstPercent"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("lineTotal"));
        cmbCustomer.setItems(FXCollections.observableArrayList(parties.getByType("CUSTOMER")));
        cmbItem.setItems(FXCollections.observableArrayList(items.getAll()));
        configureItemCell();
        configureCustomerCell();
        newSale();
    }

    private void configureItemCell() {
        cmbItem.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Item i, boolean e) {
                super.updateItem(i, e);
                setText(e || i == null ? null : i.getItemCode() + " - " + i.getDescription() + " (Stock: " + i.getOpeningStock() + ")");
            }
        });
        cmbItem.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Item i, boolean e) {
                super.updateItem(i, e);
                setText(e || i == null ? null : i.getItemCode() + " - " + i.getDescription());
            }
        });
    }

    private void configureCustomerCell() {
        cmbCustomer.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Party p, boolean e) {
                super.updateItem(p, e);
                setText(e || p == null ? null : p.getPartyCode() + " - " + p.getName());
            }
        });
        cmbCustomer.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Party p, boolean e) {
                super.updateItem(p, e);
                setText(e || p == null ? null : p.getPartyCode() + " - " + p.getName());
            }
        });
    }

    @FXML
    private void addLine() {
        Item i = cmbItem.getValue();
        if (i == null) {
            warn("Select an item.");
            return;
        }
        double q, r;
        try {
            q = Double.parseDouble(txtQuantity.getText());
            r = Double.parseDouble(txtRate.getText());
        } catch (Exception e) {
            warn("Enter valid quantity and rate.");
            return;
        }
        if (q <= 0 || r < 0) {
            warn("Quantity must be positive and rate cannot be negative.");
            return;
        }
        if (q > i.getOpeningStock()) {
            warn("Only " + i.getOpeningStock() + " units are available.");
            return;
        }
        PurchaseLine l = new PurchaseLine();
        l.setItemCode(i.getItemCode());
        l.setItemDescription(i.getItemCode() + " - " + i.getDescription());
        l.setQuantity(q);
        l.setRate(r);
        l.setGstPercent(i.getGst());
        l.setLineTotal(q * r * (1 + i.getGst() / 100));
        tableLines.getItems().add(l);
        cmbItem.setValue(null);
        txtQuantity.clear();
        txtRate.clear();
        recalculate();
    }

    @FXML
    private void removeLine() {
        PurchaseLine l = tableLines.getSelectionModel().getSelectedItem();
        if (l == null) {
            warn("Select a line to remove.");
            return;
        }
        tableLines.getItems().remove(l);
        recalculate();
    }

    @FXML
    private void saveSale() {
        if (cmbCustomer.getValue() == null) {
            warn("Select a customer.");
            return;
        }
        if (tableLines.getItems().isEmpty()) {
            warn("Add at least one item.");
            return;
        }
        double sub = tableLines.getItems().stream().mapToDouble(l -> l.getQuantity() * l.getRate()).sum(), total = tableLines.getItems().stream().mapToDouble(PurchaseLine::getLineTotal).sum();
        Sales invoice = new Sales();
        invoice.setInvoiceNo(txtInvoiceNo.getText());
        invoice.setInvoiceDate(dpInvoiceDate.getValue());
        invoice.setCustomer(cmbCustomer.getValue());
        invoice.setLines(java.util.List.copyOf(tableLines.getItems()));
        invoice.setSubtotal(sub);
        invoice.setGstAmount(total - sub);
        invoice.setTotalAmount(total);
        invoice.setRemarks(txtRemarks.getText().trim());
        try {
            sales.save(invoice);
            lastSavedSale = invoice;
            if (chkSendEmail.isSelected()) {
                NotificationService.add("Sales invoice " + invoice.getInvoiceNo() + " is waiting to be emailed.");
            }
            NotificationService.add("Sales invoice " + invoice.getInvoiceNo() + " was saved.");
            new Alert(Alert.AlertType.INFORMATION, chkSendEmail.isSelected() ? "Sales saved. Email is marked pending; click Send Invoice Email when ready." : "Sales saved. Email was not requested.").showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void sendInvoiceEmail() {
        if (lastSavedSale == null) {
            warn("Save the sales invoice first, then send its email.");
            return;
        }
        if (!chkSendEmail.isSelected()) {
            warn("Tick 'Email requested' before sending. This keeps email delivery under your control.");
            return;
        }
        String email = lastSavedSale.getCustomer().getEmail();
        if (email == null || email.isBlank()) {
            warn("The selected customer does not have an email address.");
            return;
        }
        try {
            Path pdf = InvoicePdfService.sale(lastSavedSale);
            EmailService.send(email, "Sales invoice " + lastSavedSale.getInvoiceNo(), "Please find your tax invoice attached.", pdf);
            Files.deleteIfExists(pdf);
            chkSendEmail.setText("Email sent ✓");
            chkSendEmail.setDisable(true);
            NotificationService.add("Sales invoice " + lastSavedSale.getInvoiceNo() + " was emailed to " + email + ".");
            new Alert(Alert.AlertType.INFORMATION, "Invoice emailed to " + email + ".").showAndWait();
        } catch (Exception e) {
            NotificationService.add("Sales invoice " + lastSavedSale.getInvoiceNo() + " email could not be sent.");
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void newSale() {
        txtInvoiceNo.setText(sales.nextInvoiceNo());
        dpInvoiceDate.setValue(LocalDate.now());
        cmbCustomer.setValue(null);
        cmbItem.setValue(null);
        txtQuantity.clear();
        txtRate.clear();
        txtRemarks.clear();
        chkSendEmail.setSelected(false);
        chkSendEmail.setDisable(false);
        chkSendEmail.setText("Email requested (send manually)");
        tableLines.getItems().clear();
        lastSavedSale = null;
        recalculate();
    }

    private void recalculate() {
        double sub = tableLines.getItems().stream().mapToDouble(l -> l.getQuantity() * l.getRate()).sum(), tot = tableLines.getItems().stream().mapToDouble(PurchaseLine::getLineTotal).sum();
        lblSubtotal.setText(String.format("₹ %.2f", sub));
        lblGst.setText(String.format("₹ %.2f", tot - sub));
        lblGrandTotal.setText(String.format("₹ %.2f", tot));
    }

    private void warn(String s) {
        Alert a = new Alert(Alert.AlertType.WARNING, s);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
