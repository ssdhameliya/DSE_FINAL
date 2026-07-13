package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import org.example.database.DatabaseManager;
import org.example.service.BusinessReportService;

import java.io.File;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.util.Locale;

public class ReportsController {
    @FXML
    private Label lblPurchase, lblSales, lblStock, lblLowStock;
    @FXML
    private DatePicker dpFrom, dpTo;
    private final BusinessReportService reportService = new BusinessReportService();

    @FXML
    public void initialize() {
        dpFrom.setValue(LocalDate.now().withDayOfMonth(1));
        dpTo.setValue(LocalDate.now());
        refresh();
    }

    @FXML
    private void refresh() {
        if (dpFrom == null || dpFrom.getValue() == null || dpTo.getValue() == null) return;
        if (dpFrom.getValue().isAfter(dpTo.getValue())) {
            error("The From date must be on or before the To date.");
            return;
        }
        String from = dpFrom.getValue().toString(), to = dpTo.getValue().toString();
        lblPurchase.setText(money("SELECT COALESCE(SUM(total_amount),0) FROM purchase_header WHERE invoice_date BETWEEN ? AND ?", from, to));
        lblSales.setText(money("SELECT COALESCE(SUM(total_amount),0) FROM sales_header WHERE invoice_date BETWEEN ? AND ?", from, to));
        lblStock.setText(money("SELECT COALESCE(SUM(opening_stock*purchase_price),0) FROM item_master"));
        lblLowStock.setText(value("SELECT COUNT(*) FROM item_master WHERE COALESCE(opening_stock,0)<=COALESCE(minimum_stock,0)"));
    }

    @FXML
    private void exportPdf() {
        export("PDF Report", "business-report.pdf", "*.pdf", true);
    }

    @FXML
    private void exportExcel() {
        export("Excel Report", "business-report.xlsx", "*.xlsx", false);
    }

    private void export(String title, String suggestedName, String extension, boolean pdf) {
        LocalDate from = dpFrom.getValue(), to = dpTo.getValue();
        if (from == null || to == null || from.isAfter(to)) {
            error("Choose a valid reporting date range.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.setInitialFileName(suggestedName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(title, extension));
        File file = chooser.showSaveDialog(dpFrom.getScene().getWindow());
        if (file == null) return;
        Path output = file.toPath();
        String suffix = pdf ? ".pdf" : ".xlsx";
        if (!output.toString().toLowerCase(Locale.ROOT).endsWith(suffix)) output = Path.of(output + suffix);
        try {
            if (pdf) reportService.exportPdf(output, from, to);
            else reportService.exportExcel(output, from, to);
            new Alert(Alert.AlertType.INFORMATION, "Report created successfully:\n" + output).showAndWait();
        } catch (Exception ex) {
            error("Could not create report: " + ex.getMessage());
        }
    }

    private String money(String sql, String... values) {
        return "₹ " + String.format("%,.2f", number(sql, values));
    }

    private String value(String sql, String... values) {
        return String.valueOf((long) number(sql, values));
    }

    private double number(String sql, String... values) {
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) s.setString(i + 1, values[i]);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? r.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    private void error(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Reporting error");
        alert.showAndWait();
    }
}
