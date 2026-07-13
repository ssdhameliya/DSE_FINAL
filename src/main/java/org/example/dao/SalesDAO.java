package org.example.dao;

import org.example.database.DatabaseManager;
import org.example.model.PurchaseLine;
import org.example.model.Sales;

import java.sql.*;

public class SalesDAO {
    public void save(Sales sales) {
        try (Connection con = DatabaseManager.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement hp = con.prepareStatement("INSERT INTO sales_header (invoice_no,invoice_date,customer_id,subtotal,gst_amount,total_amount,remarks) VALUES (?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS); PreparedStatement lp = con.prepareStatement("INSERT INTO sales_line (sales_id,item_code,quantity,rate,gst_percent,line_total) VALUES (?,?,?,?,?,?)"); PreparedStatement stock = con.prepareStatement("UPDATE item_master SET opening_stock=COALESCE(opening_stock,0)-? WHERE item_code=? AND COALESCE(opening_stock,0)>=?")) {
                hp.setString(1, sales.getInvoiceNo());
                hp.setString(2, sales.getInvoiceDate().toString());
                hp.setInt(3, sales.getCustomer().getId());
                hp.setDouble(4, sales.getSubtotal());
                hp.setDouble(5, sales.getGstAmount());
                hp.setDouble(6, sales.getTotalAmount());
                hp.setString(7, sales.getRemarks());
                hp.executeUpdate();
                ResultSet keys = hp.getGeneratedKeys();
                if (!keys.next()) throw new SQLException("No sales id generated");
                int id = keys.getInt(1);
                for (PurchaseLine l : sales.getLines()) {
                    stock.setDouble(1, l.getQuantity());
                    stock.setString(2, l.getItemCode());
                    stock.setDouble(3, l.getQuantity());
                    if (stock.executeUpdate() != 1)
                        throw new IllegalStateException("Insufficient stock for " + l.getItemDescription());
                    lp.setInt(1, id);
                    lp.setString(2, l.getItemCode());
                    lp.setDouble(3, l.getQuantity());
                    lp.setDouble(4, l.getRate());
                    lp.setDouble(5, l.getGstPercent());
                    lp.setDouble(6, l.getLineTotal());
                    lp.addBatch();
                }
                lp.executeBatch();
                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not save sales invoice", e);
        }
    }

    public String nextInvoiceNo() {
        try (Connection con = DatabaseManager.getConnection(); PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM sales_header"); ResultSet rs = ps.executeQuery()) {
            return "SAL-" + String.format("%05d", rs.next() ? rs.getInt(1) + 1 : 1);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not generate sales number", e);
        }
    }
}
