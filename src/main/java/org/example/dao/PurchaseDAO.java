package org.example.dao;

import org.example.database.DatabaseManager;
import org.example.model.Purchase;
import org.example.model.PurchaseLine;

import java.sql.*;

public class PurchaseDAO {
    public void save(Purchase purchase) {
        String header = "INSERT INTO purchase_header (invoice_no, invoice_date, supplier_id, subtotal, gst_amount, total_amount, remarks) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String line = "INSERT INTO purchase_line (purchase_id, item_code, quantity, rate, gst_percent, line_total) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement headerPs = connection.prepareStatement(header, Statement.RETURN_GENERATED_KEYS); PreparedStatement linePs = connection.prepareStatement(line); PreparedStatement stockPs = connection.prepareStatement("UPDATE item_master SET opening_stock=COALESCE(opening_stock, 0)+? WHERE item_code=?")) {
                headerPs.setString(1, purchase.getInvoiceNo());
                headerPs.setString(2, purchase.getInvoiceDate().toString());
                headerPs.setInt(3, purchase.getSupplier().getId());
                headerPs.setDouble(4, purchase.getSubtotal());
                headerPs.setDouble(5, purchase.getGstAmount());
                headerPs.setDouble(6, purchase.getTotalAmount());
                headerPs.setString(7, purchase.getRemarks());
                headerPs.executeUpdate();
                ResultSet keys = headerPs.getGeneratedKeys();
                if (!keys.next()) throw new SQLException("No purchase id was generated");
                int purchaseId = keys.getInt(1);
                for (PurchaseLine purchaseLine : purchase.getLines()) {
                    linePs.setInt(1, purchaseId);
                    linePs.setString(2, purchaseLine.getItemCode());
                    linePs.setDouble(3, purchaseLine.getQuantity());
                    linePs.setDouble(4, purchaseLine.getRate());
                    linePs.setDouble(5, purchaseLine.getGstPercent());
                    linePs.setDouble(6, purchaseLine.getLineTotal());
                    linePs.addBatch();
                    stockPs.setDouble(1, purchaseLine.getQuantity());
                    stockPs.setString(2, purchaseLine.getItemCode());
                    stockPs.addBatch();
                }
                linePs.executeBatch();
                stockPs.executeBatch();
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not save purchase invoice", exception);
        }
    }

    public String nextInvoiceNo() {
        try (Connection connection = DatabaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM purchase_header"); ResultSet rs = ps.executeQuery()) {
            return "PUR-" + String.format("%05d", rs.next() ? rs.getInt(1) + 1 : 1);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not generate purchase number", exception);
        }
    }
}
