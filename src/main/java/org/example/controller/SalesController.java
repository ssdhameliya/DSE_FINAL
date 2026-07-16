package org.example.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DoubleStringConverter;

import org.example.model.*;

import org.example.navigation.NavigationManager;

import org.example.service.ItemService;
import org.example.service.NotificationService;
import org.example.service.PartyService;
import org.example.service.SalesService;

import java.time.LocalDate;
import java.util.List;

public class SalesController {

    @FXML
    private TextField txtInvoiceNo;

    @FXML
    private TextField txtQuantity;

    @FXML
    private TextField txtRate;

    @FXML
    private TextField txtGST;

    @FXML
    private DatePicker dpInvoiceDate;

    @FXML
    private ComboBox<Party> cmbCustomer;

    @FXML
    private ComboBox<Item> cmbItem;

    @FXML
    private TextArea txtRemarks;

    @FXML
    private Label lblInvoiceDisplay;

    @FXML
    private Label lblNetAmount;

    @FXML
    private Label lblGst;

    @FXML
    private Label lblGrandTotal;

    @FXML
    private TableView<SalesLine> tableLines;

    @FXML
    private TableColumn<SalesLine, String> colItem;

    @FXML
    private TableColumn<SalesLine, Double> colQuantity;

    @FXML
    private TableColumn<SalesLine, Double> colRate;

    @FXML
    private TableColumn<SalesLine, Double> colGst;

    @FXML
    private TableColumn<SalesLine, Double> colGstAmount;

    @FXML
    private TableColumn<SalesLine, Double> colNetAmount;

    @FXML
    private TableColumn<SalesLine, Double> colTotal;

    @FXML
    private Button btnAddLine;



    @FXML
    private Button btnSaveSale;

    //-------------------------------------------------------
    // Services
    //-------------------------------------------------------

    private final ItemService itemService =
        new ItemService();

    private final PartyService partyService =
        new PartyService();

    private final SalesService salesService =
        new SalesService();

    //-------------------------------------------------------
    // Editing
    //-------------------------------------------------------

    private Sales editingSale = null;

    private SalesLine editingLine = null;

    private int editingIndex = -1;

    //-------------------------------------------------------
    // Initialize
    //-------------------------------------------------------

    @FXML
    public void initialize() {

        setupTable();

        setupAmountFormatting();

        tableLines.setEditable(true);

        setupEditableColumns();

        tableLines.getSelectionModel()
            .selectedItemProperty()
            .addListener((obs, oldLine, newLine) -> {

                if (newLine == null)
                    return;

                editingLine = newLine;

                editingIndex =
                    tableLines.getSelectionModel()
                        .getSelectedIndex();

                txtQuantity.setText(
                    String.valueOf(
                        newLine.getQuantity()));

                txtRate.setText(
                    String.valueOf(
                        newLine.getRate()));

                txtGST.setText(
                    String.valueOf(
                        newLine.getGstPercent()));

                for (Item item : cmbItem.getItems()) {

                    if (item.getItemCode()
                        .equals(newLine.getItemCode())) {

                        cmbItem.getSelectionModel()
                            .select(item);

                        break;
                    }
                }

            });

        //-------------------------------------------------------
        // Load Customers
        //-------------------------------------------------------

        cmbCustomer.setItems(

            FXCollections.observableArrayList(

                partyService.getByType("CUSTOMER")

            )

        );

        //-------------------------------------------------------
        // Load Items
        //-------------------------------------------------------

        cmbItem.setItems(

            FXCollections.observableArrayList(

                itemService.getAll()

            )

        );

        //-------------------------------------------------------
        // Customer Combo
        //-------------------------------------------------------

        cmbCustomer.setCellFactory(list ->
            new ListCell<>() {

                @Override
                protected void updateItem(
                    Party party,
                    boolean empty) {

                    super.updateItem(party, empty);

                    setText(

                        empty || party == null

                            ? null

                            : party.getPartyCode()
                              + " - "
                              + party.getName()

                    );

                }

            });

        cmbCustomer.setButtonCell(
            new ListCell<>() {

                @Override
                protected void updateItem(
                    Party party,
                    boolean empty) {

                    super.updateItem(party, empty);

                    setText(

                        empty || party == null

                            ? null

                            : party.getPartyCode()
                              + " - "
                              + party.getName()

                    );

                }

            });

        //-------------------------------------------------------
        // Item Combo
        //-------------------------------------------------------

        cmbItem.setCellFactory(list ->
            new ListCell<>() {

                @Override
                protected void updateItem(
                    Item item,
                    boolean empty) {

                    super.updateItem(item, empty);

                    setText(

                        empty || item == null

                            ? null

                            : item.getItemCode()
                              + " - "
                              + item.getDescription()

                    );

                }

            });

        cmbItem.setButtonCell(
            new ListCell<>() {

                @Override
                protected void updateItem(
                    Item item,
                    boolean empty) {

                    super.updateItem(item, empty);

                    setText(

                        empty || item == null

                            ? null

                            : item.getItemCode()
                              + " - "
                              + item.getDescription()

                    );

                }

            });

        newSale();

    }

    //-------------------------------------------------------
    // Setup Table
    //-------------------------------------------------------

    private void setupTable() {

        colItem.setCellValueFactory(
            new PropertyValueFactory<>("itemDescription"));

        colQuantity.setCellValueFactory(
            new PropertyValueFactory<>("quantity"));

        colRate.setCellValueFactory(
            new PropertyValueFactory<>("rate"));

        colGst.setCellValueFactory(
            new PropertyValueFactory<>("gstPercent"));

        colGstAmount.setCellValueFactory(
            new PropertyValueFactory<>("gstAmount"));

        colNetAmount.setCellValueFactory(
            new PropertyValueFactory<>("netAmount"));

        colTotal.setCellValueFactory(
            new PropertyValueFactory<>("totalAmount"));

    }

    //-------------------------------------------------------
    // Amount Formatting
    //-------------------------------------------------------

    private void setupAmountFormatting() {

        colQuantity.setCellFactory(column ->
            new TextFieldTableCell<>(
                new DoubleStringConverter()
            ));

        colRate.setCellFactory(column ->
            new TextFieldTableCell<>(
                new DoubleStringConverter()
            ));

        colGst.setCellFactory(column ->
            new TextFieldTableCell<>(
                new DoubleStringConverter()
            ));

        colGstAmount.setCellFactory(column ->
            new TableCell<>() {

                @Override
                protected void updateItem(Double value, boolean empty) {

                    super.updateItem(value, empty);

                    if (empty || value == null) {

                        setText(null);

                    } else {

                        setText(
                            String.format("₹ %.2f", value)
                        );

                    }

                }

            });

        colNetAmount.setCellFactory(column ->
            new TableCell<>() {

                @Override
                protected void updateItem(Double value, boolean empty) {

                    super.updateItem(value, empty);

                    if (empty || value == null) {

                        setText(null);

                    } else {

                        setText(
                            String.format("₹ %.2f", value)
                        );

                    }

                }

            });

        colTotal.setCellFactory(column ->
            new TableCell<>() {

                @Override
                protected void updateItem(Double value, boolean empty) {

                    super.updateItem(value, empty);

                    if (empty || value == null) {

                        setText(null);

                    } else {

                        setText(
                            String.format("₹ %.2f", value)
                        );

                    }

                }

            });

    }

    //-------------------------------------------------------
    // Editable Columns
    //-------------------------------------------------------

    private void setupEditableColumns() {

        colQuantity.setCellFactory(
            TextFieldTableCell.forTableColumn(
                new DoubleStringConverter()
            ));

        colQuantity.setOnEditCommit(event -> {

            SalesLine line = event.getRowValue();

            line.setQuantity(event.getNewValue());

            recalculateLine(line);

            tableLines.refresh();

            recalculate();

        });

        colRate.setCellFactory(
            TextFieldTableCell.forTableColumn(
                new DoubleStringConverter()
            ));

        colRate.setOnEditCommit(event -> {

            SalesLine line = event.getRowValue();

            line.setRate(event.getNewValue());

            recalculateLine(line);

            tableLines.refresh();

            recalculate();

        });

        colGst.setCellFactory(
            TextFieldTableCell.forTableColumn(
                new DoubleStringConverter()
            ));

        colGst.setOnEditCommit(event -> {

            SalesLine line = event.getRowValue();

            line.setGstPercent(event.getNewValue());

            recalculateLine(line);

            tableLines.refresh();

            recalculate();

        });

    }

    //-------------------------------------------------------
    // Recalculate One Line
    //-------------------------------------------------------

    private void recalculateLine(SalesLine line) {

        double net =
            line.getQuantity()
                * line.getRate();

        double gst =
            net
                * line.getGstPercent()
                / 100;

        double total =
            net + gst;

        line.setNetAmount(net);

        line.setGstAmount(gst);

        line.setTotalAmount(total);

    }

    //--------------------------------------------------
// SAVE SALE
//--------------------------------------------------

    @FXML
    private void saveSale() {

        Sales sale = buildSale();

        if (sale == null)
            return;

        try {

            if (editingSale != null) {

                sale.setId(editingSale.getId());

                salesService.update(sale);

                NotificationService.add(
                    "Sales "
                        + sale.getInvoiceNo()
                        + " updated"
                );

            } else {

                salesService.save(sale);

                NotificationService.add(
                    "Sales "
                        + sale.getInvoiceNo()
                        + " saved"
                );

            }

            new Alert(
                Alert.AlertType.INFORMATION,
                "Sales saved successfully"
            ).showAndWait();

            NavigationManager.getInstance()
                .loadPage("/fxml/pages/SalesList.fxml");

        }
        catch (Exception e) {

            new Alert(
                Alert.AlertType.ERROR,
                e.getMessage()
            ).showAndWait();

        }

    }


//--------------------------------------------------
// BUILD SALES OBJECT
//--------------------------------------------------

    private Sales buildSale() {

        if (dpInvoiceDate.getValue() == null) {

            warn("Select invoice date");

            return null;

        }

        if (cmbCustomer.getValue() == null) {

            warn("Select customer");

            return null;

        }

        if (tableLines.getItems().isEmpty()) {

            warn("Add items");

            return null;

        }

        Sales sale = new Sales();

        sale.setInvoiceNo(
            txtInvoiceNo.getText()
        );

        sale.setInvoiceDate(
            dpInvoiceDate.getValue()
        );

        sale.setCustomer(
            cmbCustomer.getValue()
        );

        sale.setLines(
            List.copyOf(
                tableLines.getItems()
            )
        );

        double net =
            tableLines.getItems()
                .stream()
                .mapToDouble(
                    SalesLine::getNetAmount
                )
                .sum();

        double gst =
            tableLines.getItems()
                .stream()
                .mapToDouble(
                    SalesLine::getGstAmount
                )
                .sum();

        double total =
            net + gst;

        sale.setSubtotal(net);

        sale.setGstAmount(gst);

        sale.setTotalAmount(total);

        sale.setRemarks(
            txtRemarks.getText()
        );

        return sale;

    }


//--------------------------------------------------
// NEW SALE
//--------------------------------------------------

    @FXML
    private void newSale() {

        editingSale = null;

        txtInvoiceNo.setText(
            salesService.nextInvoiceNo()
        );

        dpInvoiceDate.setValue(
            LocalDate.now()
        );

        cmbCustomer.setValue(null);

        cmbItem.setValue(null);

        txtQuantity.clear();

        txtRate.clear();

        txtGST.clear();

        txtRemarks.clear();

        tableLines.getItems().clear();

        recalculate();

    }
    //--------------------------------------------------
// RECALCULATE TOTALS
//--------------------------------------------------

    private void recalculate() {

        double net =
            tableLines.getItems()
                .stream()
                .mapToDouble(
                    SalesLine::getNetAmount
                )
                .sum();

        double gst =
            tableLines.getItems()
                .stream()
                .mapToDouble(
                    SalesLine::getGstAmount
                )
                .sum();

        double total =
            net + gst;

        lblNetAmount.setText(
            String.format("₹ %.2f", net)
        );

        lblGst.setText(
            String.format("₹ %.2f", gst)
        );

        lblGrandTotal.setText(
            String.format("₹ %.2f", total)
        );

    }


//--------------------------------------------------
// WARNING
//--------------------------------------------------

    private void warn(String msg) {

        new Alert(
            Alert.AlertType.WARNING,
            msg
        ).showAndWait();

    }


//--------------------------------------------------
// CANCEL
//--------------------------------------------------

    @FXML
    private void cancel() {

        NavigationManager.getInstance()
            .loadPage("/fxml/pages/SalesList.fxml");

    }


//--------------------------------------------------
// LOAD SALE FOR EDIT
//--------------------------------------------------

    public void loadSale(Sales sale) {

        System.out.println(
            "Invoice = " + sale.getInvoiceNo()
        );

        editingSale = sale;

        txtInvoiceNo.setText(
            sale.getInvoiceNo()
        );

        lblInvoiceDisplay.setText(
            sale.getInvoiceNo()
        );

        dpInvoiceDate.setValue(
            sale.getInvoiceDate()
        );

        // Select customer

        if (sale.getCustomer() != null) {

            for (Party party : cmbCustomer.getItems()) {

                if (party.getId()
                    == sale.getCustomer().getId()) {

                    cmbCustomer.getSelectionModel()
                        .select(party);

                    break;

                }

            }

        }

        txtRemarks.setText(

            sale.getRemarks() == null

                ? ""

                : sale.getRemarks()

        );

        tableLines.getItems().clear();

        if (sale.getLines() != null) {

            tableLines.getItems()
                .addAll(
                    sale.getLines()
                );

        }

        recalculate();

    }


//--------------------------------------------------
// VIEW MODE
//--------------------------------------------------

    public void setViewMode(boolean value) {

        txtInvoiceNo.setDisable(value);

        dpInvoiceDate.setDisable(value);

        cmbCustomer.setDisable(value);

        cmbItem.setDisable(value);

        txtQuantity.setDisable(value);

        txtRate.setDisable(value);

        txtGST.setDisable(value);

        txtRemarks.setDisable(value);

        btnAddLine.setDisable(value);

        btnSaveSale.setDisable(value);

        tableLines.setDisable(value);

    }
    @FXML
    private void addLine(){


        Item item = cmbItem.getValue();


        if(item==null){

            warn("Select item");

            return;
        }



        try{


            double qty =
                Double.parseDouble(txtQuantity.getText());


            double rate =
                Double.parseDouble(txtRate.getText());


            double gst =
                item.getGst();



            if(txtGST.getText()!=null &&
                !txtGST.getText().isBlank()){

                gst =
                    Double.parseDouble(txtGST.getText());

            }



            double net =
                qty * rate;


            double gstAmount =
                net * gst / 100;


            double total =
                net + gstAmount;



            SalesLine line =
                new SalesLine();


            line.setItemCode(
                item.getItemCode()
            );


            line.setItemDescription(
                item.getItemCode()
                    +" - "
                    +item.getDescription()
            );


            line.setQuantity(qty);


            line.setRate(rate);


            line.setGstPercent(gst);


            line.setNetAmount(net);


            line.setGstAmount(gstAmount);


            line.setTotalAmount(total);



            if(editingLine == null){

                tableLines.getItems().add(line);

            }else{

                tableLines.getItems().set(editingIndex, line);

                editingLine = null;
                editingIndex = -1;

            }



            cmbItem.setValue(null);

            txtQuantity.clear();

            txtRate.clear();

            txtGST.clear();
            tableLines.getSelectionModel().clearSelection();


            recalculate();



        }
        catch(Exception e){

            warn("Enter valid quantity and rate");

        }

    }

    @FXML
    private void cancelEdit() {

        editingLine = null;
        editingIndex = -1;

        cmbItem.setValue(null);

        txtQuantity.clear();
        txtRate.clear();
        txtGST.clear();

        tableLines.getSelectionModel().clearSelection();

        btnAddLine.setText("+ Add Line");
    }



    @FXML
    private void removeLine(){

        SalesLine line =
            tableLines
                .getSelectionModel()
                .getSelectedItem();


        if(line!=null){

            tableLines.getItems().remove(line);

            recalculate();

        }

    }


}
