package org.example.database;

import java.sql.*;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:JavaAppERP.db";

    /**
     * Initialize database and create required tables.
     */
    public static void initialize() {

        createUsersTable();
        ensureUserColumns();

        createItemMasterTable();

        createLookupTable();

        createPartyTable();

        createPurchaseTables();

        createSalesTables();

        seedLookupData();
        seedAdministrator();

    }

    /**
     * Returns database connection.
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Explicit loading keeps SQLite available in both the IDE and the shaded desktop JAR.
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("SQLite JDBC driver is unavailable", ex);
        }
        return DriverManager.getConnection(DB_URL);

    }

    /**
     * Generic table creator.
     */
    private static void createTable(String sql) {

        try (
            Connection con = getConnection();
            Statement stmt = con.createStatement()
        ) {

            stmt.execute(sql);

        } catch (SQLException ex) {

            ex.printStackTrace();

        }

    }

    //====================================================
    // USERS TABLE
    //====================================================

    private static void createUsersTable() {

        createTable("""
            CREATE TABLE IF NOT EXISTS users
            (
            
                id INTEGER PRIMARY KEY AUTOINCREMENT,
            
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                full_name TEXT,
                role TEXT NOT NULL DEFAULT 'USER',
                email TEXT UNIQUE,
                active INTEGER NOT NULL DEFAULT 1
            
            );
            """);

    }

    //====================================================
    // ITEM MASTER
    //====================================================

    private static void createItemMasterTable() {

        createTable("""
            CREATE TABLE IF NOT EXISTS item_master
            (
            
                id INTEGER PRIMARY KEY AUTOINCREMENT,
            
                item_code TEXT UNIQUE,
            
                description TEXT,
            
                category TEXT,
            
                brand TEXT,
            
                material TEXT,
            
                size TEXT,
            
                unit TEXT,
            
                hsn TEXT,
            
                gst REAL,
            
                purchase_price REAL,
            
                selling_price REAL,
            
                opening_stock REAL,
            
                minimum_stock REAL,
            
                location TEXT,
            
                remarks TEXT
            
            );
            """);

    }

    private static void createLookupTable() {

        createTable("""
            CREATE TABLE IF NOT EXISTS lookup_master (
            
                id INTEGER PRIMARY KEY AUTOINCREMENT,
            
                lookup_type TEXT NOT NULL,
            
                lookup_code TEXT NOT NULL,
            
                lookup_value TEXT NOT NULL,
            
                description TEXT,
            
                display_order INTEGER DEFAULT 0,
            
                is_active INTEGER DEFAULT 1
            
            );
            """);

    }

    private static void seedAdministrator() {
        String sql = "INSERT INTO users(username,password,full_name,role,email,active) SELECT 'admin','admin','Administrator','ADMIN','shailesh.rockstar007@yahoo.com',1 WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='admin')";
        createTable(sql);
    }

    /**
     * Supports databases created by older versions, before email and active user fields existed.
     */
    private static void ensureUserColumns() {
        addColumnIfMissing("users", "email", "TEXT");
        addColumnIfMissing("users", "active", "INTEGER NOT NULL DEFAULT 1");
        try (Connection con = getConnection(); Statement stmt = con.createStatement()) {
            stmt.executeUpdate("UPDATE users SET email='shailesh.rockstar007@yahoo.com', role='ADMIN', active=1 WHERE username='admin'");
        } catch (SQLException ignored) {
        }
    }

    private static void addColumnIfMissing(String table, String column, String definition) {
        try (Connection con = getConnection(); Statement stmt = con.createStatement(); ResultSet columns = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (columns.next()) if (column.equalsIgnoreCase(columns.getString("name"))) return;
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private static void createPartyTable() {
        createTable("""
            CREATE TABLE IF NOT EXISTS party_master (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                party_type TEXT NOT NULL,
                party_code TEXT NOT NULL UNIQUE,
                name TEXT NOT NULL,
                contact_person TEXT,
                phone TEXT,
                email TEXT,
                gstin TEXT,
                address TEXT,
                opening_balance REAL DEFAULT 0,
                is_active INTEGER DEFAULT 1
            );
            """);
    }

    private static void createPurchaseTables() {
        createTable("""
            CREATE TABLE IF NOT EXISTS purchase_header (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                invoice_no TEXT NOT NULL UNIQUE,
                invoice_date TEXT NOT NULL,
                supplier_id INTEGER NOT NULL,
                subtotal REAL NOT NULL,
                gst_amount REAL NOT NULL,
                total_amount REAL NOT NULL,
                remarks TEXT,
                FOREIGN KEY(supplier_id) REFERENCES party_master(id)
            );
            """);
        createTable("""
            CREATE TABLE IF NOT EXISTS purchase_line (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                purchase_id INTEGER NOT NULL,
                item_code TEXT NOT NULL,
                quantity REAL NOT NULL,
                rate REAL NOT NULL,
                gst_percent REAL NOT NULL,
                line_total REAL NOT NULL,
                FOREIGN KEY(purchase_id) REFERENCES purchase_header(id),
                FOREIGN KEY(item_code) REFERENCES item_master(item_code)
            );
            """);
    }

    private static void createSalesTables() {
        createTable("""
            CREATE TABLE IF NOT EXISTS sales_header (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                invoice_no TEXT NOT NULL UNIQUE,
                invoice_date TEXT NOT NULL,
                customer_id INTEGER NOT NULL,
                subtotal REAL NOT NULL,
                gst_amount REAL NOT NULL,
                total_amount REAL NOT NULL,
                remarks TEXT,
                FOREIGN KEY(customer_id) REFERENCES party_master(id)
            );
            """);
        createTable("""
            CREATE TABLE IF NOT EXISTS sales_line (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sales_id INTEGER NOT NULL,
                item_code TEXT NOT NULL,
                quantity REAL NOT NULL,
                rate REAL NOT NULL,
                gst_percent REAL NOT NULL,
                line_total REAL NOT NULL,
                FOREIGN KEY(sales_id) REFERENCES sales_header(id),
                FOREIGN KEY(item_code) REFERENCES item_master(item_code)
            );
            """);
    }

    private static void seedLookupData() {

        String sql = """
            INSERT INTO lookup_master
            (
                lookup_type,
                lookup_code,
                lookup_value,
                is_active
            )
            SELECT ?, ?, ?, 1
            WHERE NOT EXISTS
            (
                SELECT 1
                FROM lookup_master
                WHERE lookup_type = ?
                AND lookup_value = ?
            );
            """;

        String[][] data = {

            {"CATEGORY", "CAT001", "Valve"},
            {"CATEGORY", "CAT002", "Pipe"},
            {"CATEGORY", "CAT003", "Flange"},

            {"UNIT", "UNT001", "Nos"},
            {"UNIT", "UNT002", "Kg"},
            {"UNIT", "UNT003", "Meter"},

            {"MATERIAL", "MAT001", "SS304"},
            {"MATERIAL", "MAT002", "SS316"},
            {"MATERIAL", "MAT003", "Carbon Steel"},

            {"BRAND", "BRD001", "L&T"},
            {"BRAND", "BRD002", "Kirloskar"},

            {"GST", "GST001", "0"},
            {"GST", "GST002", "5"},
            {"GST", "GST003", "12"},
            {"GST", "GST004", "18"},
            {"GST", "GST005", "28"}

        };

        try (
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {

            for (String[] row : data) {

                ps.setString(1, row[0]);
                ps.setString(2, row[1]);
                ps.setString(3, row[2]);
                ps.setString(4, row[0]);
                ps.setString(5, row[2]);

                ps.executeUpdate();

            }

        } catch (SQLException ex) {

            ex.printStackTrace();

        }

    }


}
