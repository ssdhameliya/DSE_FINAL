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

            ps.setDouble(9, item.getGst());
            ps.setDouble(10, item.getPurchasePrice());
            ps.setDouble(11, item.getSellingPrice());
            ps.setDouble(12, item.getOpeningStock());
            ps.setDouble(13, item.getMinimumStock());

            ps.setString(14, item.getLocation());
            ps.setString(15, item.getRemarks());

            ps.executeUpdate();

        } catch (SQLException ex) {
            ex.printStackTrace();
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
            ex.printStackTrace();
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
            ex.printStackTrace();
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
            ex.printStackTrace();
        }

    }

    /**
     * Inserts a new item or updates an existing item with the same business code.
     */
    public void saveOrUpdate(Item item) {
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement exists = con.prepareStatement("SELECT 1 FROM item_master WHERE item_code=?")) {
            exists.setString(1, item.getItemCode());
            try (ResultSet result = exists.executeQuery()) {
                if (result.next()) update(item);
                else save(item);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Database error while saving item " + item.getItemCode(), ex);
        }
    }

}
