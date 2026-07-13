package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.example.database.DatabaseManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DashboardHomeController {
    @FXML
    private Label lblProducts, lblCustomers, lblOrders, lblStockValue;
    @FXML
    private Label lblProductsNote, lblCustomersNote, lblOrdersNote, lblStockNote;

    @FXML
    public void initialize() {
        long products = count("SELECT COUNT(*) FROM item_master");
        long customers = count("SELECT COUNT(*) FROM party_master WHERE party_type='CUSTOMER'");
        long invoices = count("SELECT COUNT(*) FROM sales_header");
        long lowStock = count("SELECT COUNT(*) FROM item_master WHERE COALESCE(opening_stock,0)<=COALESCE(minimum_stock,0)");
        double stockValue = number("SELECT COALESCE(SUM(opening_stock*purchase_price),0) FROM item_master");
        lblProducts.setText(String.valueOf(products));
        lblCustomers.setText(String.valueOf(customers));
        lblOrders.setText(String.valueOf(invoices));
        lblStockValue.setText(String.format("₹ %,.2f", stockValue));
        lblProductsNote.setText(products == 0 ? "Add your first item" : "Items in catalog");
        lblCustomersNote.setText(customers == 0 ? "Add your first customer" : "Active customer records");
        lblOrdersNote.setText(invoices == 0 ? "No sales invoices yet" : "Sales invoices created");
        lblStockNote.setText(lowStock + " low-stock item" + (lowStock == 1 ? "" : "s"));
    }

    private long count(String sql) {
        return (long) number(sql);
    }

    private double number(String sql) {
        try (Connection con = DatabaseManager.getConnection(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (Exception exception) {
            return 0;
        }
    }
}
