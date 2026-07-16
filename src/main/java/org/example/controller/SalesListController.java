package org.example.controller;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
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
import java.util.List;

import org.example.model.Purchase;
import org.example.model.Sales;
import org.example.navigation.NavigationManager;
import org.example.service.NotificationService;
import org.example.service.SalesService;
import org.example.service.InvoicePdfService;
import org.example.service.EmailService;

import java.nio.file.Files;
import java.nio.file.Path;

public class SalesListController {

    @FXML
    private TableView<Sales> tableSales;

    @FXML
    private TableColumn<Sales, String> colInvoice;

    @FXML
    private TableColumn<Sales, String> colDate;

    @FXML
    private TableColumn<Sales, String> colCustomer;

    @FXML
    private TableColumn<Sales, Double> colQuantity;

    @FXML
    private TableColumn<Sales, Double> colNetAmount;

    @FXML
    private TableColumn<Sales, Double> colGST;

    @FXML
    private TableColumn<Sales, Double> colTotal;

    @FXML
    private TableColumn<Sales, Boolean> colMail;

    @FXML
    private TableColumn<Sales, String> colCreated;

    @FXML
    private TableColumn<Sales, Void> colAction;

    @FXML
    private TextField txtSearch;

    @FXML
    private DatePicker dpFrom;

    @FXML
    private DatePicker dpTo;

    @FXML
    private ComboBox<String> cmbMailStatus;

    private final SalesService salesService =
        new SalesService();

    @FXML
    public void initialize() {

        cmbMailStatus.getItems().addAll(
            "All",
            "Sent",
            "Pending"
        );

        cmbMailStatus.setValue("All");

        txtSearch.textProperty()
            .addListener((o, a, b) -> filter());

        dpFrom.valueProperty()
            .addListener((o, a, b) -> filter());

        dpTo.valueProperty()
            .addListener((o, a, b) -> filter());

        cmbMailStatus.valueProperty()
            .addListener((o, a, b) -> filter());

       colInvoice.setCellValueFactory(
            new PropertyValueFactory<>("invoiceNo")
        );

        colDate.setCellValueFactory(cell ->
            new SimpleStringProperty(
                cell.getValue()
                    .getInvoiceDate()
                    .toString()
            )
        );

        colCustomer.setCellValueFactory(cell ->
            new SimpleStringProperty(
                cell.getValue()
                    .getCustomer()
                    .getName()
            )
        );
        colQuantity.setCellValueFactory(
            new PropertyValueFactory<>("quantity")
        );

        colNetAmount.setCellValueFactory(cell ->

            new SimpleDoubleProperty(

                cell.getValue().getTotalAmount()
                    -
                    cell.getValue().getGstAmount()

            ).asObject()

        );

        colGST.setCellValueFactory(
            new PropertyValueFactory<>("gstAmount")
        );

        colTotal.setCellValueFactory(
            new PropertyValueFactory<>("totalAmount")
        );

        colCreated.setCellValueFactory(
            new PropertyValueFactory<>("createdAt")
        );

        colMail.setCellValueFactory(cell ->

            new SimpleBooleanProperty(

                cell.getValue().isEmailSent()

            )

        );

        colMail.setCellFactory(
            CheckBoxTableCell.forTableColumn(colMail)
        );

        colMail.setEditable(false);

        tableSales.setEditable(false);

        setupRegisterFormatting();

        colAction.setCellFactory(param -> new TableCell<>() {

            private final Button btnView = new Button("👁");
            private final Button btnEdit = new Button("✏");
            private final Button btnDelete = new Button("🗑");
            private final Button btnPrint = new Button("🖨");
            private final Button btnEmail = new Button("📧");

            {
                btnView.setTooltip(new Tooltip("View Sales Details"));
                btnEdit.setTooltip(new Tooltip("Edit Sales Invoice"));
                btnDelete.setTooltip(new Tooltip("Delete Sales Invoice"));
                btnPrint.setTooltip(new Tooltip("Print Invoice"));
                btnEmail.setTooltip(new Tooltip("Send Email"));

                btnView.setOnAction(e -> {
                    Sales sale = getTableView().getItems().get(getIndex());
                    viewSale(sale);
                });

                btnEdit.setOnAction(e -> {
                    Sales sale = getTableView().getItems().get(getIndex());
                    editSale(sale);
                });

                btnDelete.setOnAction(e -> {
                    Sales sale = getTableView().getItems().get(getIndex());
                    deleteSale(sale);
                });

                btnPrint.setOnAction(e -> {
                    Sales sale = getTableView().getItems().get(getIndex());
                    printSale(sale);
                });

                btnEmail.setOnAction(e -> {
                    Sales sale = getTableView().getItems().get(getIndex());
                    resendEmail(sale);
                });
            }

            private final HBox pane = new HBox(
                8,
                btnView,
                btnEdit,
                btnDelete,
                btnPrint,
                btnEmail
            );

            {
                pane.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });



        refresh();
    }

    @FXML
    private void refresh() {

        List<Sales> list =
            salesService.getAll();

        System.out.println("======================");
        System.out.println("SALES COUNT : " + list.size());

        for (Sales s : list) {

            System.out.println(
                s.getInvoiceNo()
                    + " | "
                    + s.getInvoiceDate()
                    + " | "
                    + s.getCustomer().getName()
                    + " | "
                    + s.getTotalAmount()
            );

        }

        System.out.println("======================");

        tableSales.setItems(
            FXCollections.observableArrayList(list)
        );

    }

    @FXML
    private void newSale() {

        StackPane contentPane =
            (StackPane) tableSales
                .getScene()
                .lookup("#contentPane");

        if (contentPane == null) {

            new Alert(
                Alert.AlertType.ERROR,
                "Unable to open Sales Screen."
            ).showAndWait();

            return;
        }

        NavigationManager navigation =
            new NavigationManager(contentPane);

        navigation.loadPage(
            "/fxml/pages/Sale.fxml"
        );

    }

    @FXML
    private void exportSale() {

        if (tableSales.getItems().isEmpty()) {

            new Alert(
                Alert.AlertType.WARNING,
                "No sales data available to export."
            ).showAndWait();

            return;

        }

        FileChooser chooser =
            new FileChooser();

        chooser.setTitle(
            "Export Sales Register"
        );

        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(
                "Excel File",
                "*.xlsx"
            )
        );

        chooser.setInitialFileName(
            "Sales_Register.xlsx"
        );

        File file =
            chooser.showSaveDialog(
                tableSales.getScene().getWindow()
            );

        if (file == null) {

            return;

        }

        try (

            Workbook workbook =
                new XSSFWorkbook();

            FileOutputStream out =
                new FileOutputStream(file)

        ) {

            Sheet sheet =
                workbook.createSheet(
                    "Sales Register"
                );

            Row header =
                sheet.createRow(0);

            String[] columns = {

                "Invoice No",
                "Date",
                "Customer",
                "Net Amount",
                "GST Amount",
                "Grand Total",
                "Email Status",
                "Created At"

            };

            for (int i = 0; i < columns.length; i++) {

                header.createCell(i)
                    .setCellValue(columns[i]);

            }

            int rowIndex = 1;

            for (Sales s : tableSales.getItems()) {

                Row row =
                    sheet.createRow(rowIndex++);

                row.createCell(0)
                    .setCellValue(s.getInvoiceNo());

                row.createCell(1)
                    .setCellValue(
                        s.getInvoiceDate().toString()
                    );

                row.createCell(2)
                    .setCellValue(
                        s.getCustomer().getName()
                    );

                row.createCell(3)
                    .setCellValue(
                        s.getTotalAmount()
                            - s.getGstAmount()
                    );

                row.createCell(4)
                    .setCellValue(
                        s.getGstAmount()
                    );

                row.createCell(5)
                    .setCellValue(
                        s.getTotalAmount()
                    );

                row.createCell(6)
                    .setCellValue(
                        s.isEmailSent()
                            ? "Sent"
                            : "Pending"
                    );

                row.createCell(7)
                    .setCellValue(
                        s.getCreatedAt()
                    );

            }

            for (int i = 0; i < columns.length; i++) {

                sheet.autoSizeColumn(i);

            }

            workbook.write(out);

            new Alert(
                Alert.AlertType.INFORMATION,
                "Sales register exported successfully."
            ).showAndWait();

        }
        catch (Exception e) {

            new Alert(
                Alert.AlertType.ERROR,
                e.getMessage()
            ).showAndWait();

        }

    }

    private void viewSale(Sales sale) {

        double netAmount =
            sale.getTotalAmount()
                -
                sale.getGstAmount();

        Alert alert =
            new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Sales Details");

        alert.setHeaderText(
            "Invoice : " + sale.getInvoiceNo()
        );

        alert.setContentText(

            "Invoice No : "
                + sale.getInvoiceNo()

                + "\n\nDate : "
                + sale.getInvoiceDate()

                + "\nCustomer : "
                + sale.getCustomer().getName()

                + "\n\nNet Amount : ₹ "
                + String.format("%.2f", netAmount)

                + "\nGST Amount : ₹ "
                + String.format("%.2f",
                sale.getGstAmount())

                + "\nGrand Total : ₹ "
                + String.format("%.2f",
                sale.getTotalAmount())

                + "\n\nEmail Status : "
                + (sale.isEmailSent()
                ? "Sent"
                : "Pending")

                + "\n\nCreated : "
                + sale.getCreatedAt()

        );

        alert.showAndWait();

    }

    private void resendEmail(Sales sale) {

        try {

            Sales invoice =
                salesService.getByInvoice(
                    sale.getInvoiceNo()
                );

            if (invoice == null) {

                new Alert(
                    Alert.AlertType.ERROR,
                    "Invoice not found."
                ).showAndWait();

                return;

            }

            String email =
                invoice.getCustomer().getEmail();

            if (email == null || email.isBlank()) {

                new Alert(
                    Alert.AlertType.WARNING,
                    "Customer email not available."
                ).showAndWait();

                return;

            }

            Path pdf =
                InvoicePdfService.sales(invoice);

            EmailService.send(
                email,
                "Sales Invoice " + invoice.getInvoiceNo(),
                "Please find attached sales invoice.",
                pdf
            );

            Files.deleteIfExists(pdf);

            salesService.markEmailSent(
                invoice.getId()
            );

            NotificationService.add(
                "Sales Invoice "
                    + invoice.getInvoiceNo()
                    + " emailed."
            );

            refresh();

            Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

            alert.setTitle("Email Sent");

            alert.setHeaderText(
                "Invoice emailed successfully."
            );

            alert.setContentText(

                "Invoice No : "
                    + invoice.getInvoiceNo()

                    + "\nSent To : "
                    + invoice.getCustomer().getEmail()

            );

            alert.showAndWait();

        }
        catch (Exception ex) {

            new Alert(
                Alert.AlertType.ERROR,
                ex.getMessage()
            ).showAndWait();

        }

    }

    private void printSale(Sales sale) {

        try {

            Sales invoice =
                salesService.getByInvoice(
                    sale.getInvoiceNo()
                );

            Path pdf =
                InvoicePdfService.sales(invoice);

            java.awt.Desktop
                .getDesktop()
                .open(pdf.toFile());

        }
        catch (Exception ex) {

            new Alert(
                Alert.AlertType.ERROR,
                ex.getMessage()
            ).showAndWait();

        }

    }

    private void editSale(Sales sale) {

        try {

            // Load complete sale with lines
            Sales fullSale =
                salesService.getByInvoice(
                    sale.getInvoiceNo()
                );

            if (fullSale == null) {

                new Alert(
                    Alert.AlertType.ERROR,
                    "Sale not found."
                ).showAndWait();

                return;

            }

            StackPane contentPane =
                (StackPane) tableSales
                    .getScene()
                    .lookup("#contentPane");

            FXMLLoader loader =
                new FXMLLoader(
                    getClass().getResource(
                        "/fxml/pages/Sale.fxml"
                    )
                );

            Parent root =
                loader.load();

            SalesController controller =
                loader.getController();

            controller.loadSale(fullSale);

            contentPane.getChildren().setAll(root);

        }
        catch (Exception e) {

            e.printStackTrace();

            new Alert(
                Alert.AlertType.ERROR,
                e.getMessage()
            ).showAndWait();

        }

    }

    private void deleteSale(Sales sale) {

        Alert confirm =
            new Alert(Alert.AlertType.CONFIRMATION);

        confirm.setTitle("Delete Sale");

        confirm.setHeaderText("Delete Invoice?");

        confirm.setContentText(
            "Invoice No : "
                + sale.getInvoiceNo()
        );

        confirm.showAndWait()
            .ifPresent(response -> {

                if (response == ButtonType.OK) {

                    try {

                        salesService.delete(
                            sale.getInvoiceNo()
                        );

                        refresh();

                        new Alert(
                            Alert.AlertType.INFORMATION,
                            "Sale deleted successfully."
                        ).showAndWait();

                    }
                    catch (Exception e) {

                        new Alert(
                            Alert.AlertType.ERROR,
                            e.getMessage()
                        ).showAndWait();

                    }

                }

            });

    }

    private void filter() {

        List<Sales> list =
            salesService.getAll();

        String search =
            txtSearch.getText();

        if (search != null &&
            !search.isBlank()) {

            String keyword =
                search.toLowerCase();

            list = list.stream()

                .filter(s ->

                    s.getInvoiceNo()
                        .toLowerCase()
                        .contains(keyword)

                        ||

                        s.getCustomer()
                            .getName()
                            .toLowerCase()
                            .contains(keyword)

                )

                .toList();

        }

        if (dpFrom.getValue() != null) {

            list = list.stream()

                .filter(s ->

                    !s.getInvoiceDate()
                        .isBefore(
                            dpFrom.getValue()
                        )

                )

                .toList();

        }

        if (dpTo.getValue() != null) {

            list = list.stream()

                .filter(s ->

                    !s.getInvoiceDate()
                        .isAfter(
                            dpTo.getValue()
                        )

                )

                .toList();

        }

        switch (cmbMailStatus.getValue()) {

            case "Sent" ->

                list = list.stream()

                    .filter(
                        Sales::isEmailSent
                    )

                    .toList();

            case "Pending" ->

                list = list.stream()

                    .filter(
                        s -> !s.isEmailSent()
                    )

                    .toList();

        }

        tableSales.setItems(
            FXCollections.observableArrayList(
                list
            )
        );

    }

    private void setupRegisterFormatting() {

        colGST.setCellFactory(column ->
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

        colQuantity.setCellFactory(column ->
            new TableCell<>() {

                @Override
                protected void updateItem(Double value, boolean empty) {

                    super.updateItem(value, empty);

                    if (empty || value == null) {

                        setText(null);

                    } else {

                        setText(
                            String.format("%.0f", value)
                        );

                    }

                }

            });

        colDate.setCellFactory(column ->
            new TableCell<>() {

                private final DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("dd-MM-yyyy");

                @Override
                protected void updateItem(String value, boolean empty) {

                    super.updateItem(value, empty);

                    if (empty || value == null) {

                        setText(null);

                    } else {

                        try {

                            setText(
                                java.time.LocalDate
                                    .parse(value)
                                    .format(formatter)
                            );

                        } catch (Exception e) {

                            setText(value);

                        }

                    }

                }

            });

        colCreated.setCellFactory(column ->
            new TableCell<>() {

                @Override
                protected void updateItem(String value, boolean empty) {

                    super.updateItem(value, empty);

                    if (empty || value == null) {

                        setText(null);

                    } else {

                        setText(value);

                    }

                }

            });

    }



}
