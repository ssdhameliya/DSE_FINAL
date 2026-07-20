package org.example.dao;

import org.example.database.DatabaseManager;
import org.example.model.Item;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    /**
     * Save Item
     */
    public void save(Item item) {
        String sql = """
            INSERT INTO item_master (
                item_code,
                description,
                category,
                brand,
                material,
                size,
                unit,
                hsn,
                gst,
                purchase_price,
                selling_price,
                opening_stock,
                minimum_stock,
                location,
                remarks
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (
            Connection con = DatabaseManager.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, item.getItemCode());
            ps.setString(2, item.getDescription());
            ps.setString(3, item.getCategory());
            ps.setString(4, item.getBrand());
            ps.setString(5, item.getMaterial());
            ps.setString(6, item.getSize());
            ps.setString(7, item.getUnit());
            ps.setString(8, item.getHsn());

            // Use setObject for nullable numeric fields if your Item uses wrapper types
            ps.setDouble(9, item.getGst());
            ps.setDouble(10, item.getPurchasePrice());
            ps.setDouble(11, item.getSellingPrice());
            ps.setDouble(12, item.getOpeningStock());
            ps.setDouble(13, item.getMinimumStock());

            ps.setString(14, item.getLocation());
            ps.setString(15, item.getRemarks());

            ps.executeUpdate();

        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save item " + item.getItemCode(), ex);
        }
    }

    /**
     * Get All Items
     */
    public List<Item> getAll() {
        List<Item> list = new ArrayList<>();

        String sql = """
            SELECT *
            FROM item_master
            ORDER BY item_code
            """;

        try (
            Connection con = DatabaseManager.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                Item item = new Item();

                item.setItemCode(rs.getString("item_code"));
                item.setDescription(rs.getString("description"));
                item.setCategory(rs.getString("category"));
                item.setBrand(rs.getString("brand"));
                item.setMaterial(rs.getString("material"));
                item.setSize(rs.getString("size"));
                item.setUnit(rs.getString("unit"));
                item.setHsn(rs.getString("hsn"));

                item.setGst(rs.getDouble("gst"));
                item.setPurchasePrice(rs.getDouble("purchase_price"));
                item.setSellingPrice(rs.getDouble("selling_price"));
                item.setOpeningStock(rs.getDouble("opening_stock"));
                item.setMinimumStock(rs.getDouble("minimum_stock"));

                item.setLocation(rs.getString("location"));
                item.setRemarks(rs.getString("remarks"));

                list.add(item);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to fetch items", ex);
        }

        return list;
    }

    /**
     * Delete Item
     */
    public void delete(String itemCode) {
        String sql = """
            DELETE FROM item_master
            WHERE item_code = ?
            """;

        try (
            Connection con = DatabaseManager.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, itemCode);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete item " + itemCode, ex);
        }
    }

    /**
     * Update Item
     */
    public void update(Item item) {
        String sql = """
            UPDATE item_master
            SET
                description=?,
                category=?,
                brand=?,
                material=?,
                size=?,
                unit=?,
                hsn=?,
                gst=?,
                purchase_price=?,
                selling_price=?,
                opening_stock=?,
                minimum_stock=?,
                location=?,
                remarks=?
            WHERE item_code=?
            """;

        try (
            Connection con = DatabaseManager.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, item.getDescription());
            ps.setString(2, item.getCategory());
            ps.setString(3, item.getBrand());
            ps.setString(4, item.getMaterial());
            ps.setString(5, item.getSize());
            ps.setString(6, item.getUnit());
            ps.setString(7, item.getHsn());

            ps.setDouble(8, item.getGst());
            ps.setDouble(9, item.getPurchasePrice());
            ps.setDouble(10, item.getSellingPrice());
            ps.setDouble(11, item.getOpeningStock());
            ps.setDouble(12, item.getMinimumStock());

            ps.setString(13, item.getLocation());
            ps.setString(14, item.getRemarks());

            ps.setString(15, item.getItemCode());

            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update item " + item.getItemCode(), ex);
        }
    }

    /**
     * Inserts a new item or updates an existing item with the same business code.
     * Uses a simple existence check and delegates to save() or update().
     */
    public void saveOrUpdate(Item item) {
        String checkSql = "SELECT 1 FROM item_master WHERE item_code = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(checkSql)) {
            ps.setString(1, item.getItemCode());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) update(item);
                else save(item);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to saveOrUpdate item " + item.getItemCode(), ex);
        }
    }

    /**
     * Batch upsert for performance.
     * This implementation uses MySQL's ON DUPLICATE KEY UPDATE syntax.
     * If you use PostgreSQL, replace the SQL with an ON CONFLICT (...) DO UPDATE clause.
     */
    public void saveOrUpdateBatch(List<Item> items) {
        if (items == null || items.isEmpty()) return;

        // SQLite UPSERT syntax (requires SQLite 3.24+)
        String sql = """
        INSERT INTO item_master (
            item_code, description, category, brand, material, size, unit, hsn,
            gst, purchase_price, selling_price, opening_stock, minimum_stock,
            location, remarks
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(item_code) DO UPDATE SET
            description = excluded.description,
            category = excluded.category,
            brand = excluded.brand,
            material = excluded.material,
            size = excluded.size,
            unit = excluded.unit,
            hsn = excluded.hsn,
            gst = excluded.gst,
            purchase_price = excluded.purchase_price,
            selling_price = excluded.selling_price,
            opening_stock = excluded.opening_stock,
            minimum_stock = excluded.minimum_stock,
            location = excluded.location,
            remarks = excluded.remarks
        """;

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            boolean prevAuto = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                for (Item item : items) {
                    ps.setString(1, item.getItemCode());
                    ps.setString(2, item.getDescription());
                    ps.setString(3, item.getCategory());
                    ps.setString(4, item.getBrand());
                    ps.setString(5, item.getMaterial());
                    ps.setString(6, item.getSize());
                    ps.setString(7, item.getUnit());
                    ps.setString(8, item.getHsn());

                    // nullable numeric fields (Item getters return Double)
                    if (item.getGst() == null) ps.setNull(9, Types.REAL);
                    else ps.setObject(9, item.getGst(), Types.REAL);

                    if (item.getPurchasePrice() == null) ps.setNull(10, Types.REAL);
                    else ps.setObject(10, item.getPurchasePrice(), Types.REAL);

                    if (item.getSellingPrice() == null) ps.setNull(11, Types.REAL);
                    else ps.setObject(11, item.getSellingPrice(), Types.REAL);

                    if (item.getOpeningStock() == null) ps.setNull(12, Types.REAL);
                    else ps.setObject(12, item.getOpeningStock(), Types.REAL);

                    if (item.getMinimumStock() == null) ps.setNull(13, Types.REAL);
                    else ps.setObject(13, item.getMinimumStock(), Types.REAL);

                    ps.setString(14, item.getLocation());
                    ps.setString(15, item.getRemarks());

                    ps.addBatch();
                }
                ps.executeBatch();
                con.commit();
            } catch (SQLException ex) {
                con.rollback();
                // rethrow so caller sees the real cause
                throw new SQLException("Batch import failed: " + ex.getMessage(), ex);
            } finally {
                con.setAutoCommit(prevAuto);
            }
        } catch (SQLException ex) {
            // Let caller handle and display the message (ImportController shows details)
            throw new RuntimeException(ex);
        }
    }


}
