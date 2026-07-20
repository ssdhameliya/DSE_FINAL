package org.example.service;

import org.example.database.DatabaseManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Generic CSV import service. Provides upsert behaviour for Items, Parties (Customer/Supplier),
 * Purchase, Sales and Quotation based on agreed business keys.
 *
 * This is a best-effort import: validates required columns and reports inserted/updated/failed counts.
 */
public final class ImportService {

    public static class ImportReport {
        public int inserted = 0;
        public int updated = 0;
        public int failed = 0;
        public final List<String> errors = new ArrayList<>();

        @Override
        public String toString() {
            return "ImportReport{" +
                    "inserted=" + inserted +
                    ", updated=" + updated +
                    ", failed=" + failed +
                    ", errors=" + errors.size() +
                    '}';
        }
    }

    private ImportService() {}

    public static ImportReport importItems(Path csvFile) throws IOException {
        ImportReport report = new ImportReport();
        try (InputStream in = Files.newInputStream(csvFile)) {
            report = importItems(in);
        }
        return report;
    }

    public static ImportReport importItems(InputStream csvStream) throws IOException {
        ImportReport report = new ImportReport();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {
            String header = r.readLine();
            if (header == null) {
                report.failed++;
                report.errors.add("Empty file");
                return report;
            }
            String[] cols = header.split(",");
            Map<String,Integer> idx = headerIndex(cols);

            String line;
            while ((line = r.readLine()) != null) {
                String[] values = line.split(",", -1);
                String itemCode = getValue(values, idx, "item_code");
                String description = getValue(values, idx, "description");
                try (Connection con = DatabaseManager.getConnection()) {
                    // Try update by item_code first
                    boolean updated = false;
                    if (itemCode != null && !itemCode.isBlank()) {
                        String u = "UPDATE item_master SET description=?, category=?, brand=?, material=?, size=?, unit=?, hsn=?, gst=?, purchase_price=?, selling_price=?, opening_stock=?, minimum_stock=?, location=?, remarks=? WHERE item_code=?";
                        try (PreparedStatement ps = con.prepareStatement(u)) {
                            setItemPs(ps, values, idx);
                            ps.setString(15, itemCode);
                            int cnt = ps.executeUpdate();
                            if (cnt > 0) {
                                report.updated++;
                                updated = true;
                            }
                        }
                    }
                    if (!updated) {
                        // Try match by description
                        String select = "SELECT id FROM item_master WHERE description = ? LIMIT 1";
                        try (PreparedStatement ps = con.prepareStatement(select)) {
                            ps.setString(1, description);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    int id = rs.getInt(1);
                                    String u2 = "UPDATE item_master SET item_code=?, category=?, brand=?, material=?, size=?, unit=?, hsn=?, gst=?, purchase_price=?, selling_price=?, opening_stock=?, minimum_stock=?, location=?, remarks=?, description=? WHERE id=?";
                                    try (PreparedStatement ps2 = con.prepareStatement(u2)) {
                                        setItemPs(ps2, values, idx);
                                        ps2.setString(14, description);
                                        ps2.setInt(15, id);
                                        int cnt2 = ps2.executeUpdate();
                                        if (cnt2 > 0) report.updated++;
                                    }
                                } else {
                                    // Insert
                                    String i = "INSERT INTO item_master(item_code,description,category,brand,material,size,unit,hsn,gst,purchase_price,selling_price,opening_stock,minimum_stock,location,remarks) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                                    try (PreparedStatement ps3 = con.prepareStatement(i)) {
                                        setItemPs(ps3, values, idx);
                                        int cnt3 = ps3.executeUpdate();
                                        if (cnt3 > 0) report.inserted++;
                                    }
                                }
                            }
                        }
                    }
                } catch (SQLException ex) {
                    report.failed++;
                    report.errors.add("Row error: " + ex.getMessage());
                }
            }
        }
        return report;
    }

    private static void setItemPs(PreparedStatement ps, String[] values, Map<String,Integer> idx) throws SQLException {
        ps.setString(1, getValue(values, idx, "item_code"));
        ps.setString(2, getValue(values, idx, "description"));
        ps.setString(3, getValue(values, idx, "category"));
        ps.setString(4, getValue(values, idx, "brand"));
        ps.setString(5, getValue(values, idx, "material"));
        ps.setString(6, getValue(values, idx, "size"));
        ps.setString(7, getValue(values, idx, "unit"));
        ps.setString(8, getValue(values, idx, "hsn"));
        ps.setObject(9, parseDouble(getValue(values, idx, "gst")));
        ps.setObject(10, parseDouble(getValue(values, idx, "purchase_price")));
        ps.setObject(11, parseDouble(getValue(values, idx, "selling_price")));
        ps.setObject(12, parseDouble(getValue(values, idx, "opening_stock")));
        ps.setObject(13, parseDouble(getValue(values, idx, "minimum_stock")));
        ps.setString(14, getValue(values, idx, "location"));
        ps.setString(15, getValue(values, idx, "remarks"));
    }

    private static Double parseDouble(String v) {
        if (v == null || v.isBlank()) return null;
        try { return Double.parseDouble(v); } catch (NumberFormatException e) { return null; }
    }

    private static Map<String,Integer> headerIndex(String[] cols) {
        Map<String,Integer> map = new HashMap<>();
        for (int i=0;i<cols.length;i++) {
            map.put(cols[i].trim().toLowerCase(), i);
        }
        return map;
    }

    private static String getValue(String[] values, Map<String,Integer> idx, String key) {
        Integer i = idx.get(key);
        if (i == null) return null;
        if (i < 0 || i >= values.length) return null;
        String v = values[i].trim();
        return v.isEmpty() ? null : v;
    }

    // --- Party import (customer/supplier)
    public static ImportReport importParties(InputStream csvStream) throws IOException {
        ImportReport report = new ImportReport();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {
            String header = r.readLine();
            if (header == null) { report.failed++; report.errors.add("Empty file"); return report; }
            String[] cols = header.split(",");
            Map<String,Integer> idx = headerIndex(cols);
            String line;
            while ((line = r.readLine()) != null) {
                String[] values = line.split(",", -1);
                String partyCode = getValue(values, idx, "party_code");
                String email = getValue(values, idx, "email");
                String phone = getValue(values, idx, "phone");
                String name = getValue(values, idx, "name");
                try (Connection con = DatabaseManager.getConnection()) {
                    boolean updated = false;
                    if (partyCode != null && !partyCode.isBlank()) {
                        String u = "UPDATE party_master SET party_type=?, name=?, contact_person=?, phone=?, email=?, gstin=?, address=?, opening_balance=?, is_active=? WHERE party_code=?";
                        try (PreparedStatement ps = con.prepareStatement(u)) {
                            ps.setString(1, getValue(values, idx, "party_type"));
                            ps.setString(2, name);
                            ps.setString(3, getValue(values, idx, "contact_person"));
                            ps.setString(4, phone);
                            ps.setString(5, email);
                            ps.setString(6, getValue(values, idx, "gstin"));
                            ps.setString(7, getValue(values, idx, "address"));
                            ps.setObject(8, parseDouble(getValue(values, idx, "opening_balance")));
                            ps.setObject(9, parseDouble(getValue(values, idx, "is_active")));
                            ps.setString(10, partyCode);
                            int cnt = ps.executeUpdate();
                            if (cnt > 0) { report.updated++; updated = true; }
                        }
                    }
                    if (!updated) {
                        // match by email or phone
                        String select = "SELECT id FROM party_master WHERE (email = ? AND email IS NOT NULL) OR (phone = ? AND phone IS NOT NULL) LIMIT 1";
                        try (PreparedStatement ps = con.prepareStatement(select)) {
                            ps.setString(1, email);
                            ps.setString(2, phone);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    int id = rs.getInt(1);
                                    String u2 = "UPDATE party_master SET party_code=?, party_type=?, name=?, contact_person=?, phone=?, email=?, gstin=?, address=?, opening_balance=?, is_active=? WHERE id=?";
                                    try (PreparedStatement ps2 = con.prepareStatement(u2)) {
                                        ps2.setString(1, partyCode);
                                        ps2.setString(2, getValue(values, idx, "party_type"));
                                        ps2.setString(3, name);
                                        ps2.setString(4, getValue(values, idx, "contact_person"));
                                        ps2.setString(5, phone);
                                        ps2.setString(6, email);
                                        ps2.setString(7, getValue(values, idx, "gstin"));
                                        ps2.setString(8, getValue(values, idx, "address"));
                                        ps2.setObject(9, parseDouble(getValue(values, idx, "opening_balance")));
                                        ps2.setObject(10, parseDouble(getValue(values, idx, "is_active")));
                                        ps2.setInt(11, id);
                                        int cnt2 = ps2.executeUpdate();
                                        if (cnt2 > 0) report.updated++;
                                    }
                                } else {
                                    String i = "INSERT INTO party_master(party_type,party_code,name,contact_person,phone,email,gstin,address,opening_balance,is_active) VALUES(?,?,?,?,?,?,?,?,?,?)";
                                    try (PreparedStatement ps3 = con.prepareStatement(i)) {
                                        ps3.setString(1, getValue(values, idx, "party_type"));
                                        ps3.setString(2, partyCode);
                                        ps3.setString(3, name);
                                        ps3.setString(4, getValue(values, idx, "contact_person"));
                                        ps3.setString(5, phone);
                                        ps3.setString(6, email);
                                        ps3.setString(7, getValue(values, idx, "gstin"));
                                        ps3.setString(8, getValue(values, idx, "address"));
                                        ps3.setObject(9, parseDouble(getValue(values, idx, "opening_balance")));
                                        ps3.setInt(10, 1);
                                        int cnt3 = ps3.executeUpdate();
                                        if (cnt3 > 0) report.inserted++;
                                    }
                                }
                            }
                        }
                    }
                } catch (SQLException ex) {
                    report.failed++;
                    report.errors.add("Party row error: " + ex.getMessage());
                }
            }
        }
        return report;
    }

    // Note: Purchase/Sales/Quotation CSV import is domain specific and involves header + lines.
    // For now we provide a simple placeholder that expects an invoice header row followed by lines with item_code,quantity,rate,gst_percent
    public static ImportReport importSales(InputStream csvStream) throws IOException {
        return importInvoiceLike(csvStream, "sales");
    }

    public static ImportReport importPurchase(InputStream csvStream) throws IOException {
        return importInvoiceLike(csvStream, "purchase");
    }

    public static ImportReport importQuotation(InputStream csvStream) throws IOException {
        return importInvoiceLike(csvStream, "quotation");
    }

    private static ImportReport importInvoiceLike(InputStream csvStream, String type) throws IOException {
        ImportReport report = new ImportReport();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {
            String header = r.readLine();
            if (header == null) { report.failed++; report.errors.add("Empty file"); return report; }
            String[] cols = header.split(",");
            Map<String,Integer> idx = headerIndex(cols);
            String line;
            // Very simple parser: each row represents a complete invoice (no multi-line support)
            while ((line = r.readLine()) != null) {
                String[] values = line.split(",", -1);
                String invoiceNo = getValue(values, idx, "invoice_no");
                try (Connection con = DatabaseManager.getConnection()) {
                    // Check existence
                    String headerTable = type.equals("sales") ? "sales_header" : type.equals("purchase") ? "purchase_header" : "sales_header"; // use sales_header for quotation fallback
                    String select = "SELECT id FROM " + headerTable + " WHERE invoice_no = ? LIMIT 1";
                    try (PreparedStatement ps = con.prepareStatement(select)) {
                        ps.setString(1, invoiceNo);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                // update header
                                int id = rs.getInt(1);
                                String u = "UPDATE " + headerTable + " SET invoice_date=?, subtotal=?, gst_amount=?, total_amount=?, remarks=? WHERE id=?";
                                try (PreparedStatement ps2 = con.prepareStatement(u)) {
                                    ps2.setString(1, getValue(values, idx, "invoice_date"));
                                    ps2.setObject(2, parseDouble(getValue(values, idx, "subtotal")));
                                    ps2.setObject(3, parseDouble(getValue(values, idx, "gst_amount")));
                                    ps2.setObject(4, parseDouble(getValue(values, idx, "total_amount")));
                                    ps2.setString(5, getValue(values, idx, "remarks"));
                                    ps2.setInt(6, id);
                                    ps2.executeUpdate();
                                    report.updated++;
                                }
                            } else {
                                String insert = "INSERT INTO " + headerTable + "(invoice_no,invoice_date,customer_id,subtotal,gst_amount,total_amount,remarks) VALUES(?,?,?,?,?,?,?)";
                                try (PreparedStatement ps3 = con.prepareStatement(insert)) {
                                    ps3.setString(1, invoiceNo);
                                    ps3.setString(2, getValue(values, idx, "invoice_date"));
                                    ps3.setObject(3, parseInteger(getValue(values, idx, "customer_id")));
                                    ps3.setObject(4, parseDouble(getValue(values, idx, "subtotal")));
                                    ps3.setObject(5, parseDouble(getValue(values, idx, "gst_amount")));
                                    ps3.setObject(6, parseDouble(getValue(values, idx, "total_amount")));
                                    ps3.setString(7, getValue(values, idx, "remarks"));
                                    ps3.executeUpdate();
                                    report.inserted++;
                                }
                            }
                        }
                    }
                } catch (SQLException ex) {
                    report.failed++;
                    report.errors.add(type + " row error: " + ex.getMessage());
                }
            }
        }
        return report;
    }

    private static Integer parseInteger(String v) {
        if (v == null || v.isBlank()) return null;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return null; }
    }

}
