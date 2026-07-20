package org.example.controller;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.control.TableCell;
import java.time.format.DateTimeFormatter;
import org.example.model.Purchase;
import org.example.navigation.NavigationManager;
import org.example.service.NotificationService;
import org.example.service.PurchaseService;
import java.nio.file.Files;
import java.nio.file.Path;

import org.example.service.EmailService;
import org.example.service.InvoicePdfService;


import java.util.List;

public class PurchaseListController {

    @FXML
    private TableView<Purchase> tablePurchase;

    @FXML
    private TableColumn<Purchase, String> colInvoice;

    @FXML
    private TableColumn<Purchase, String> colDate;

    @FXML
    private TableColumn<Purchase, String> colSupplier;

    @FXML
    private TableColumn<Purchase, Double> colGST;

    @FXML
    private TableColumn<Purchase, Double> colTotal;

    @FXML
    private TableColumn<Purchase, Boolean> colMail;

    @FXML
    private TableColumn<Purchase, String> colCreated;

    @FXML
    private TableColumn<Purchase, Void> colAction;

    @FXML
    private TextField txtSearch;

    @FXML
    private TableColumn<Purchase, Double> colNetAmount;

    @FXML
    private DatePicker dpFrom;

    @FXML
    private DatePicker dpTo;

    @FXML
    private ComboBox<String> cmbMailStatus;

    @FXML
    private TableColumn<Purchase, Double> colQuantity;

    private final PurchaseService purchaseService = new PurchaseService();

    @FXML
    public void initialize() {

        cmbMailStatus.getItems().addAll(
            "All",
            "Sent",
            "Pending"
        );

        cmbMailStatus.setValue("All");

        txtSearch.textProperty().addListener((o, a, b) -> filter());
        dpFrom.valueProperty().addListener((o, a, b) -> filter());
        dpTo.valueProperty().addListener((o, a, b) -> filter());
        cmbMailStatus.valueProperty().addListener((o, a, b) -> filter());
        colQuantity.setCellValueFactory(
            new PropertyValueFactory<>("quantity")
        );
        colInvoice.setCellValueFactory(new PropertyValueFactory<>("invoiceNo"));

        colDate.setCellValueFactory(cell ->
            new SimpleStringProperty(
                cell.getValue().getInvoiceDate().toString()));

        colSupplier.setCellValueFactory(cell ->
            new SimpleStringProperty(
                cell.getValue().getSupplier().getName()));
        colNetAmount.setCellValueFactory(cell ->

            new javafx.beans.property.SimpleDoubleProperty(

                cell.getValue().getTotalAmount()
                    -
                    cell.getValue().getGstAmount()

            ).asObject()

        );
        colQuantity.setCellFactory(column ->
            new TableCell<>() {

                @Override
                protected void updateItem(Double value, boolean empty) {

                    super.updateItem(value, empty);

                    if (empty || value == null) {

                        setText(null);

                    } else {

                        setText(String.format("%.0f", value));

                    }
                }
            });
        colNetAmount.setCellFactory(column ->

            new TableCell<>() {

                @Override
                protected void updateItem(Double value, boolean empty) {

                    super.updateItem(value, empty);

                    if(empty || value == null) {

                        setText(null);

                    } else {

                        setText(String.format("₹ %.2f", value));

                    }

                }

            }

        );

        colGST.setCellValueFactory(
            new PropertyValueFactory<>("gstAmount"));

        colTotal.setCellValueFactory(
            new PropertyValueFactory<>("totalAmount"));
        setupRegisterFormatting();

        colCreated.setCellValueFactory(
            new PropertyValueFactory<>("createdAt"));

        colMail.setCellValueFactory(cell ->
            new SimpleBooleanProperty(
                cell.getValue().isEmailSent()));

        colMail.setCellFactory(CheckBoxTableCell.forTableColumn(colMail));
        colMail.setEditable(false);
        tablePurchase.setEditable(false);

        colAction.setCellFactory(param -> new TableCell<>() {


            private final Button btnView =
                new Button("👁");


            private final Button btnEdit =
                new Button("✏");


            private final Button btnDelete =
                new Button("🗑");


            private final Button btnPrint =
                new Button("🖨");


            private final Button btnEmail =
                new Button("📧");



            {

                btnView.setTooltip(
                    new Tooltip("View Purchase Details")
                );


                btnEdit.setTooltip(
                    new Tooltip("Edit Purchase Invoice")
                );


                btnDelete.setTooltip(
                    new Tooltip("Delete Purchase")
                );


                btnPrint.setTooltip(
                    new Tooltip("Print / Open PDF Invoice")
                );


                btnEmail.setTooltip(
                    new Tooltip("Send Invoice Email")
                );

            }


            private final HBox pane =
                new HBox(
                    8,
                    btnView,
                    btnEdit,
                    btnDelete,
                    btnPrint,
                    btnEmail
                );



            {


                pane.setAlignment(Pos.CENTER);



                btnView.setOnAction(e -> {

                    Purchase purchase =
                        getTableView()
                            .getItems()
                            .get(getIndex());

                    viewPurchase(purchase);

                });



                btnEdit.setOnAction(e -> {

                    Purchase purchase =
                        getTableView()
                            .getItems()
                            .get(getIndex());

                    editPurchase(purchase);

                });



                btnDelete.setOnAction(e -> {

                    Purchase purchase =
                        getTableView()
                            .getItems()
                            .get(getIndex());

                    deletePurchase(purchase);

                });



                btnPrint.setOnAction(e -> {

                    Purchase purchase =
                        getTableView()
                            .getItems()
                            .get(getIndex());

                    printPurchase(purchase);

                });



                btnEmail.setOnAction(e -> {

                    Purchase purchase =
                        getTableView()
                            .getItems()
                            .get(getIndex());

                    resendEmail(purchase);

                });


            }



            @Override
            protected void updateItem(Void item, boolean empty) {


                super.updateItem(item, empty);


                if(empty){

                    setGraphic(null);

                }
                else{

                    setGraphic(pane);

                }

            }



        });

        refresh();

    }

    @FXML
    private void refresh() {

        List<Purchase> list =
            purchaseService.getAll();


        System.out.println("======================");
        System.out.println("PURCHASE COUNT : " + list.size());


        for(Purchase p : list){

            System.out.println(
                p.getInvoiceNo()
                    + " | "
                    + p.getInvoiceDate()
                    + " | "
                    + p.getSupplier().getName()
                    + " | "
                    + p.getTotalAmount()
            );

        }

        System.out.println("======================");


        tablePurchase.setItems(
            FXCollections.observableArrayList(list)
        );

    }

    @FXML
    private void newPurchase() {

        StackPane contentPane =
            (StackPane) tablePurchase
                .getScene()
                .lookup("#contentPane");

        if (contentPane == null) {

            new Alert(Alert.AlertType.ERROR,
                "Unable to open Purchase Screen.")
                .showAndWait();

            return;

        }

        NavigationManager navigation =
            new NavigationManager(contentPane);

        navigation.loadPage("/fxml/pages/Purchase.fxml");

    }

    @FXML
    private void exportPurchase() {


        if(tablePurchase.getItems().isEmpty()){

            new Alert(
                Alert.AlertType.WARNING,
                "No purchase data available to export."
            ).showAndWait();

            return;

        }



        FileChooser chooser =
            new FileChooser();


        chooser.setTitle(
            "Export Purchase Register"
        );


        chooser.getExtensionFilters()
            .add(
                new FileChooser.ExtensionFilter(
                    "Excel File",
                    "*.xlsx"
                )
            );


        chooser.setInitialFileName(
            "Purchase_Register.xlsx"
        );



        File file =
            chooser.showSaveDialog(
                tablePurchase.getScene()
                    .getWindow()
            );



        if(file == null){

            return;

        }



        try(
            Workbook workbook =
                new XSSFWorkbook();

            FileOutputStream out =
                new FileOutputStream(file)

        ){


            Sheet sheet =
                workbook.createSheet(
                    "Purchase Register"
                );



            Row header =
                sheet.createRow(0);



            String[] columns = {

                "Invoice No",
                "Date",
                "Supplier",
                "Net Amount",
                "GST Amount",
                "Total Amount",
                "Email Status",
                "Created At"

            };



            for(int i=0;i<columns.length;i++){

                header.createCell(i)
                    .setCellValue(columns[i]);

            }



            int rowIndex = 1;



            for(Purchase p :
                tablePurchase.getItems()){


                Row row =
                    sheet.createRow(
                        rowIndex++
                    );



                row.createCell(0)
                    .setCellValue(
                        p.getInvoiceNo()
                    );


                row.createCell(1)
                    .setCellValue(
                        p.getInvoiceDate()
                            .toString()
                    );


                row.createCell(2)
                    .setCellValue(
                        p.getSupplier()
                            .getName()
                    );


                row.createCell(3)
                    .setCellValue(
                        p.getTotalAmount()
                            -
                            p.getGstAmount()
                    );


                row.createCell(4)
                    .setCellValue(
                        p.getGstAmount()
                    );


                row.createCell(5)
                    .setCellValue(
                        p.getTotalAmount()
                    );


                row.createCell(6)
                    .setCellValue(
                        p.isEmailSent()
                            ?
                            "Sent"
                            :
                            "Pending"
                    );


                row.createCell(7)
                    .setCellValue(
                        p.getCreatedAt()
                    );


            }



            for(int i=0;i<columns.length;i++){

                sheet.autoSizeColumn(i);

            }



            workbook.write(out);



            new Alert(
                Alert.AlertType.INFORMATION,
                "Purchase register exported successfully."
            ).showAndWait();



        }
        catch(Exception e){


            new Alert(
                Alert.AlertType.ERROR,
                e.getMessage()
            ).showAndWait();


        }


    }

    private void viewPurchase(Purchase purchase) {


        double netAmount =
            purchase.getTotalAmount()
                -
                purchase.getGstAmount();



        Alert alert =
            new Alert(Alert.AlertType.INFORMATION);



        alert.setTitle("Purchase Details");

        alert.setHeaderText(
            "Invoice : " + purchase.getInvoiceNo()
        );


        alert.setContentText(

            "Invoice No : "
                + purchase.getInvoiceNo()

                + "\n\nDate : "
                + purchase.getInvoiceDate()

                + "\nSupplier : "
                + purchase.getSupplier().getName()

                + "\n\nNet Amount : ₹ "
                + String.format("%.2f", netAmount)

                + "\nGST Amount : ₹ "
                + String.format("%.2f",
                purchase.getGstAmount())

                + "\nGrand Total : ₹ "
                + String.format("%.2f",
                purchase.getTotalAmount())

                + "\n\nEmail Status : "
                + (purchase.isEmailSent()
                ? "Sent"
                : "Pending")

                + "\n\nCreated : "
                + purchase.getCreatedAt()

        );


        alert.showAndWait();

    }

   /* private void resendEmail(Purchase purchase) {

        try {

            Purchase invoice =
                purchaseService.getByInvoice(
                    purchase.getInvoiceNo());

            if (invoice == null) {

                new Alert(Alert.AlertType.ERROR,
                    "Invoice not found.")
                    .showAndWait();

                return;

            }

            String email =
                invoice.getSupplier().getEmail();

            if (email == null || email.isBlank()) {

                new Alert(Alert.AlertType.WARNING,
                    "Supplier email not available.")
                    .showAndWait();

                return;

            }

            Path pdf =
                InvoicePdfService.purchase(invoice);

            EmailService.send(
                email,
                "Purchase Invoice " + invoice.getInvoiceNo(),
                "Please find attached purchase invoice.",
                pdf);

            Files.deleteIfExists(pdf);

            purchaseService.markEmailSent(invoice.getId());

            NotificationService.add(
                "Purchase Invoice "
                    + invoice.getInvoiceNo()
                    + " emailed.");

            refresh();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);

            alert.setTitle("Email Sent");
            alert.setHeaderText("Invoice emailed successfully.");

            alert.setContentText(
                "Invoice No : " + purchase.getInvoiceNo()
                    + "\nSent To : " + purchase.getSupplier().getEmail()
            );

            alert.showAndWait();

        }
        catch (Exception ex) {

            new Alert(Alert.AlertType.ERROR,
                ex.getMessage())
                .showAndWait();

        }

    }*/

    private void printPurchase(Purchase purchase) {

        try {

            Purchase invoice =
                purchaseService.getByInvoice(
                    purchase.getInvoiceNo());

            Path pdf =
                InvoicePdfService.purchase(invoice);

            java.awt.Desktop
                .getDesktop()
                .open(pdf.toFile());

        }
        catch (Exception ex) {

            new Alert(Alert.AlertType.ERROR,
                ex.getMessage())
                .showAndWait();

        }

    }
    private void editPurchase(Purchase purchase) {

        try {


            // IMPORTANT:
            // Load complete purchase with lines

            Purchase fullPurchase =
                purchaseService.getByInvoice(
                    purchase.getInvoiceNo()
                );


            if(fullPurchase == null){

                new Alert(
                    Alert.AlertType.ERROR,
                    "Purchase not found."
                ).showAndWait();

                return;

            }



            StackPane contentPane =
                (StackPane) tablePurchase
                    .getScene()
                    .lookup("#contentPane");



            FXMLLoader loader =
                new FXMLLoader(
                    getClass()
                        .getResource(
                            "/fxml/pages/Purchase.fxml"
                        )
                );



            Parent root =
                loader.load();



            PurchaseController controller =
                loader.getController();



            controller.loadPurchase(
                fullPurchase
            );



            contentPane.getChildren()
                .setAll(root);



        }
        catch(Exception e){


            e.printStackTrace();


            new Alert(
                Alert.AlertType.ERROR,
                e.getMessage()
            ).showAndWait();


        }

    }
    private void deletePurchase(Purchase purchase) {


        Alert confirm = new Alert(
            Alert.AlertType.CONFIRMATION
        );

        confirm.setTitle("Delete Purchase");
        confirm.setHeaderText("Delete Invoice?");
        confirm.setContentText(
            "Invoice No : " + purchase.getInvoiceNo()
        );


        confirm.showAndWait()
            .ifPresent(response -> {


                if(response == ButtonType.OK) {


                    try {


                        purchaseService.delete(
                            purchase.getInvoiceNo()
                        );


                        refresh();


                        new Alert(
                            Alert.AlertType.INFORMATION,
                            "Purchase deleted successfully."
                        ).showAndWait();


                    }
                    catch(Exception e) {


                        new Alert(
                            Alert.AlertType.ERROR,
                            e.getMessage()
                        ).showAndWait();


                    }


                }


            });


    }

    private void filter() {

        List<Purchase> list = purchaseService.getAll();

        String search = txtSearch.getText();

        if (search != null && !search.isBlank()) {

            String keyword = search.toLowerCase();

            list = list.stream()

                .filter(p ->

                    p.getInvoiceNo().toLowerCase().contains(keyword)

                        ||

                        p.getSupplier().getName().toLowerCase().contains(keyword)

                )

                .toList();

        }

        if (dpFrom.getValue() != null) {

            list = list.stream()

                .filter(p ->

                    !p.getInvoiceDate().isBefore(dpFrom.getValue())

                )

                .toList();

        }

        if (dpTo.getValue() != null) {

            list = list.stream()

                .filter(p ->

                    !p.getInvoiceDate().isAfter(dpTo.getValue())

                )

                .toList();

        }

        switch (cmbMailStatus.getValue()) {

            case "Sent" ->

                list = list.stream()

                    .filter(Purchase::isEmailSent)

                    .toList();

            case "Pending" ->

                list = list.stream()

                    .filter(p -> !p.isEmailSent())

                    .toList();

        }

        tablePurchase.setItems(
            FXCollections.observableArrayList(list));

    }
    private void setupRegisterFormatting() {


        colGST.setCellFactory(column ->
            new TableCell<>() {

                @Override
                protected void updateItem(Double value, boolean empty) {

                    super.updateItem(value, empty);

                    if(empty || value == null){

                        setText(null);

                    }
                    else{

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

                    if(empty || value == null){

                        setText(null);

                    }
                    else{

                        setText(
                            String.format("₹ %.2f", value)
                        );

                    }

                }

            });



        colDate.setCellFactory(column ->
            new TableCell<>() {


                private final DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("dd-MM-yyyy");


                @Override
                protected void updateItem(String value, boolean empty){

                    super.updateItem(value,empty);


                    if(empty || value==null){

                        setText(null);

                    }
                    else{

                        try{

                            setText(
                                java.time.LocalDate
                                    .parse(value)
                                    .format(formatter)
                            );

                        }
                        catch(Exception e){

                            setText(value);

                        }

                    }

                }

            });



        colCreated.setCellFactory(column ->
            new TableCell<>() {

                @Override
                protected void updateItem(String value, boolean empty){

                    super.updateItem(value,empty);

                    if(empty || value==null){

                        setText(null);

                    }
                    else{

                        setText(value);

                    }

                }

            });

    }

    @FXML
    private void resendEmail(Purchase purchase) {
        try {
            Purchase invoice = purchaseService.getByInvoice(purchase.getInvoiceNo());

            if (invoice == null) {
                new Alert(Alert.AlertType.ERROR, "Invoice not found.").showAndWait();
                return;
            }

            String email = invoice.getSupplier().getEmail();
            if (email == null || email.isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Supplier email not available.").showAndWait();
                return;
            }

            Path pdf = InvoicePdfService.purchase(invoice);

            EmailService.send(
                email,
                "Purchase Invoice " + invoice.getInvoiceNo(),
                "Please find attached purchase invoice.",
                pdf
            );

            Files.deleteIfExists(pdf);
            purchaseService.markEmailSent(invoice.getId());
            NotificationService.add("Purchase Invoice " + invoice.getInvoiceNo() + " emailed.");
            refresh();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Email Sent");
            alert.setHeaderText("Invoice emailed successfully.");
            alert.setContentText("Invoice No : " + purchase.getInvoiceNo() +
                "\nSent To : " + invoice.getSupplier().getEmail());
            alert.showAndWait();

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }


}
