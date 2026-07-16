package org.example.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableCell;
import org.example.model.Item;
import org.example.model.Party;
import org.example.model.Purchase;
import org.example.model.PurchaseLine;
import org.example.navigation.NavigationManager;
import org.example.service.ItemService;
import org.example.service.PartyService;
import org.example.service.PurchaseService;
import org.example.service.NotificationService;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DoubleStringConverter;

import java.time.LocalDate;
import java.util.List;


public class PurchaseController {


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
    private ComboBox<Party> cmbSupplier;


    @FXML
    private ComboBox<Item> cmbItem;


    @FXML
    private TextArea txtRemarks;


    private PurchaseLine editingLine = null;

    private int editingIndex = -1;


    @FXML
    private Label lblNetAmount;

    @FXML
    private Label lblGst;

    @FXML
    private Label lblGrandTotal;



    @FXML
    private TableView<PurchaseLine> tableLines;


    @FXML
    private TableColumn<PurchaseLine,String> colItem;


    @FXML
    private TableColumn<PurchaseLine,Double> colQuantity;


    @FXML
    private TableColumn<PurchaseLine,Double> colRate;


    @FXML
    private TableColumn<PurchaseLine,Double> colGst;


    @FXML
    private TableColumn<PurchaseLine,Double> colGstAmount;


    @FXML
    private TableColumn<PurchaseLine,Double> colNetAmount;


    @FXML
    private TableColumn<PurchaseLine,Double> colTotal;



    private final ItemService itemService =
        new ItemService();


    private final PartyService partyService =
        new PartyService();


    private final PurchaseService purchaseService =
        new PurchaseService();

    private Purchase editingPurchase = null;

    @FXML
    private Button btnAddLine;




    @FXML
    public void initialize(){


        setupTable();

        setupAmountFormatting();
        tableLines.setEditable(true);
        setupEditableColumns();

        tableLines.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldLine, newLine) -> {

                if(newLine == null)
                    return;

                editingLine = newLine;
                editingIndex = tableLines.getSelectionModel().getSelectedIndex();

                txtQuantity.setText(String.valueOf(newLine.getQuantity()));
                txtRate.setText(String.valueOf(newLine.getRate()));
                txtGST.setText(String.valueOf(newLine.getGstPercent()));

                // Select the correct item
                for(Item item : cmbItem.getItems()){

                    if(item.getItemCode().equals(newLine.getItemCode())){

                        cmbItem.getSelectionModel().select(item);
                        break;
                    }
                }
            }
        );

        cmbSupplier.setItems(
            FXCollections.observableArrayList(
                partyService.getByType("SUPPLIER")
            )
        );


        cmbItem.setItems(
            FXCollections.observableArrayList(
                itemService.getAll()
            )
        );


        cmbItem.setCellFactory(list ->
            new ListCell<>(){

                @Override
                protected void updateItem(Item item, boolean empty){

                    super.updateItem(item,empty);

                    setText(
                        empty || item==null
                            ? null
                            : item.getItemCode()
                              +" - "
                              +item.getDescription()
                    );
                }
            });


        cmbItem.setButtonCell(
            new ListCell<>(){

                @Override
                protected void updateItem(Item item, boolean empty){

                    super.updateItem(item,empty);

                    setText(
                        empty || item==null
                            ? null
                            : item.getItemCode()
                              +" - "
                              +item.getDescription()
                    );
                }
            });


        cmbSupplier.setCellFactory(list ->
            new ListCell<>(){

                @Override
                protected void updateItem(Party party, boolean empty){

                    super.updateItem(party,empty);

                    setText(
                        empty || party==null
                            ? null
                            : party.getPartyCode()
                              +" - "
                              +party.getName()
                    );

                }

            });


        cmbSupplier.setButtonCell(
            new ListCell<>(){

                @Override
                protected void updateItem(Party party, boolean empty){

                    super.updateItem(party,empty);

                    setText(
                        empty || party==null
                            ? null
                            : party.getPartyCode()
                              +" - "
                              +party.getName()
                    );

                }
            });


        newPurchase();

    }





    private void setupTable(){


        colItem.setCellValueFactory(
            new PropertyValueFactory<>("itemDescription")
        );


        colQuantity.setCellValueFactory(
            new PropertyValueFactory<>("quantity")
        );


        colRate.setCellValueFactory(
            new PropertyValueFactory<>("rate")
        );


        colGst.setCellValueFactory(
            new PropertyValueFactory<>("gstPercent")
        );


        colGstAmount.setCellValueFactory(
            new PropertyValueFactory<>("gstAmount")
        );


        colNetAmount.setCellValueFactory(
            new PropertyValueFactory<>("netAmount")
        );


        colTotal.setCellValueFactory(
            new PropertyValueFactory<>("totalAmount")
        );

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



            PurchaseLine line =
                new PurchaseLine();


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

        PurchaseLine line =
            tableLines
                .getSelectionModel()
                .getSelectedItem();


        if(line!=null){

            tableLines.getItems().remove(line);

            recalculate();

        }

    }





    @FXML
    private void savePurchase(){

        Purchase purchase = buildPurchase();

        if(purchase == null)
            return;

        try {

            if(editingPurchase != null){

                purchase.setId(editingPurchase.getId());

                purchaseService.update(purchase);

                NotificationService.add(
                    "Purchase "
                        + purchase.getInvoiceNo()
                        + " updated"
                );

            }
            else {

                purchaseService.save(purchase);

                NotificationService.add(
                    "Purchase "
                        + purchase.getInvoiceNo()
                        + " saved"
                );

            }


            new Alert(
                Alert.AlertType.INFORMATION,
                "Purchase saved successfully"
            ).showAndWait();



            NavigationManager.getInstance()
                .loadPage(
                    "/fxml/pages/PurchaseList.fxml"
                );


        }
        catch(Exception e){

            new Alert(
                Alert.AlertType.ERROR,
                e.getMessage()
            ).showAndWait();

        }

    }

    private Purchase buildPurchase(){
        if(dpInvoiceDate.getValue()==null){

            warn("Select invoice date");

            return null;

        }

        if(cmbSupplier.getValue()==null){

            warn("Select supplier");

            return null;

        }


        if(tableLines.getItems().isEmpty()){

            warn("Add items");

            return null;

        }



        Purchase purchase =
            new Purchase();


        purchase.setInvoiceNo(
            txtInvoiceNo.getText()
        );


        purchase.setInvoiceDate(
            dpInvoiceDate.getValue()
        );


        purchase.setSupplier(
            cmbSupplier.getValue()
        );


        purchase.setLines(
            List.copyOf(
                tableLines.getItems()
            )
        );



        double net =
            tableLines.getItems()
                .stream()
                .mapToDouble(
                    PurchaseLine::getNetAmount
                )
                .sum();



        double gst =
            tableLines.getItems()
                .stream()
                .mapToDouble(
                    PurchaseLine::getGstAmount
                )
                .sum();



        double total =
            net + gst;



        purchase.setSubtotal(net);

        purchase.setGstAmount(gst);

        purchase.setTotalAmount(total);


        purchase.setRemarks(
            txtRemarks.getText()
        );



        return purchase;

    }





    @FXML
    private void newPurchase(){

        editingPurchase = null;


        txtInvoiceNo.setText(
            purchaseService.nextInvoiceNo()
        );

        dpInvoiceDate.setValue(
            LocalDate.now()
        );


        cmbSupplier.setValue(null);

        cmbItem.setValue(null);


        txtQuantity.clear();

        txtRate.clear();

        txtGST.clear();


        txtRemarks.clear();


        tableLines.getItems().clear();


        recalculate();


    }





    private void recalculate(){


        double net =
            tableLines.getItems()
                .stream()
                .mapToDouble(
                    PurchaseLine::getNetAmount
                )
                .sum();


        double gst =
            tableLines.getItems()
                .stream()
                .mapToDouble(
                    PurchaseLine::getGstAmount
                )
                .sum();


        double total =
            net + gst;



        lblNetAmount.setText(
            String.format("₹ %.2f",net)
        );


        lblGst.setText(
            String.format("₹ %.2f",gst)
        );


        lblGrandTotal.setText(
            String.format("₹ %.2f",total)
        );

    }





    private void warn(String msg){

        new Alert(
            Alert.AlertType.WARNING,
            msg
        ).showAndWait();

    }





    @FXML
    private void cancel(){


        NavigationManager.getInstance()
            .loadPage(
                "/fxml/pages/PurchaseList.fxml"
            );

    }

    public void loadPurchase(Purchase purchase)
    {
        System.out.println(
            "Invoice = " + purchase.getInvoiceNo()
        );


        tableLines.getItems().clear();


        if(purchase.getLines()!=null &&
            !purchase.getLines().isEmpty()) {

            tableLines.getItems()
                .addAll(
                    purchase.getLines()
                );

        }
        else{

            System.out.println(
                "Lines = NULL"
            );

        }
        editingPurchase = purchase;


        txtInvoiceNo.setText(
            purchase.getInvoiceNo()
        );


        dpInvoiceDate.setValue(
            purchase.getInvoiceDate()
        );


        // FIX SUPPLIER SELECTION
        if(purchase.getSupplier()!=null){

            for(Party party : cmbSupplier.getItems()){

                if(party.getId() == purchase.getSupplier().getId()){

                    cmbSupplier.getSelectionModel()
                        .select(party);

                    break;
                }
            }
        }



        txtRemarks.setText(
            purchase.getRemarks()==null
                ? ""
                : purchase.getRemarks()
        );



        tableLines.getItems().clear();



        if(purchase.getLines()!=null){

            tableLines.getItems()
                .addAll(
                    purchase.getLines()
                );

        }


        recalculate();

    }    public void setViewMode(boolean value){

    }
    private void setupAmountFormatting() {


        colQuantity.setCellFactory(column -> {

            TextFieldTableCell<PurchaseLine, Double> cell =
                new TextFieldTableCell<>(
                    new DoubleStringConverter()
                );

            return cell;

        });


        colRate.setCellFactory(column -> {

            TextFieldTableCell<PurchaseLine, Double> cell =
                new TextFieldTableCell<>(
                    new DoubleStringConverter()
                );

            return cell;

        });


        colGst.setCellFactory(column -> {

            TextFieldTableCell<PurchaseLine, Double> cell =
                new TextFieldTableCell<>(
                    new DoubleStringConverter()
                );

            return cell;

        });


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

    private void setupEditableColumns() {

        // Quantity
        colQuantity.setCellFactory(
            TextFieldTableCell.forTableColumn(
                new DoubleStringConverter()
            )
        );

        colQuantity.setOnEditCommit(event -> {

            PurchaseLine line = event.getRowValue();

            line.setQuantity(event.getNewValue());

            recalculateLine(line);

            tableLines.refresh();

            recalculate();

        });


        // Rate
        colRate.setCellFactory(
            TextFieldTableCell.forTableColumn(
                new DoubleStringConverter()
            )
        );

        colRate.setOnEditCommit(event -> {

            PurchaseLine line = event.getRowValue();

            line.setRate(event.getNewValue());

            recalculateLine(line);

            tableLines.refresh();

            recalculate();

        });


        // GST %
        colGst.setCellFactory(
            TextFieldTableCell.forTableColumn(
                new DoubleStringConverter()
            )
        );

        colGst.setOnEditCommit(event -> {

            PurchaseLine line = event.getRowValue();

            line.setGstPercent(event.getNewValue());

            recalculateLine(line);

            tableLines.refresh();

            recalculate();

        });

    }

    private void recalculateLine(PurchaseLine line) {

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

}
