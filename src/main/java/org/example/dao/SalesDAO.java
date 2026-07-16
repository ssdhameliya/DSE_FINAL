package org.example.dao;

import org.example.database.DatabaseManager;
import org.example.model.Party;
import org.example.model.Sales;
import org.example.model.SalesLine;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SalesDAO {

    //====================================================
    // SAVE SALES
    //====================================================

    public void save(Sales sales) {

        String headerSql =
            """
            
                INSERT INTO sales_header
            (
                invoice_no,
                invoice_date,
                customer_id,
                subtotal,
                gst_amount,
                total_amount,
                remarks,
                created_at,
                email_sent
            )
            VALUES
            (
                ?,?,?,?,?,?,?,datetime('now'),0
            )
            """;


        String lineSql =
                """
            INSERT INTO sales_line
            (
                sales_id,
                item_code,
                quantity,
                rate,
                gst_percent,
                line_total
            )
            VALUES
            (?,?,?,?,?,?)
            """;


        String stockSql =
                """
            UPDATE item_master
            SET opening_stock =
            COALESCE(opening_stock,0)-?
            WHERE item_code=?
            """;


        try(Connection con =
                DatabaseManager.getConnection()) {

            con.setAutoCommit(false);

            try(

                PreparedStatement headerPs =
                    con.prepareStatement(
                        headerSql,
                        Statement.RETURN_GENERATED_KEYS
                    );

                PreparedStatement linePs =
                    con.prepareStatement(lineSql);

                PreparedStatement stockPs =
                    con.prepareStatement(stockSql)

            ){

                headerPs.setString(
                    1,
                    sales.getInvoiceNo()
                );

                headerPs.setString(
                    2,
                    sales.getInvoiceDate().toString()
                );

                headerPs.setInt(
                    3,
                    sales.getCustomer().getId()
                );

                headerPs.setDouble(
                    4,
                    sales.getSubtotal()
                );

                headerPs.setDouble(
                    5,
                    sales.getGstAmount()
                );

                headerPs.setDouble(
                    6,
                    sales.getTotalAmount()
                );

                headerPs.setString(
                    7,
                    sales.getRemarks()
                );

                headerPs.executeUpdate();

                ResultSet keys =
                    headerPs.getGeneratedKeys();

                keys.next();

                int salesId =
                    keys.getInt(1);

                for(SalesLine line :
                    sales.getLines()) {

                    linePs.setInt(
                        1,
                        salesId
                    );

                    linePs.setString(
                        2,
                        line.getItemCode()
                    );

                    linePs.setDouble(
                        3,
                        line.getQuantity()
                    );

                    linePs.setDouble(
                        4,
                        line.getRate()
                    );

                    linePs.setDouble(
                        5,
                        line.getGstPercent()
                    );

                    linePs.setDouble(
                        6,
                        line.getTotalAmount()
                    );

                    linePs.addBatch();

                    // Reduce stock after sale
                    stockPs.setDouble(
                        1,
                        line.getQuantity()
                    );

                    stockPs.setString(
                        2,
                        line.getItemCode()
                    );

                    stockPs.addBatch();
                }

                linePs.executeBatch();
                stockPs.executeBatch();

                con.commit();

            }
            catch (Exception e){

                con.rollback();
                throw e;

            }

        }
        catch (Exception e){

            throw new RuntimeException(
                "Unable to save sales",
                e
            );
}

    }

    //====================================================
// SALES REGISTER LOAD
//====================================================

    public List<Sales> getAll() {

        List<Sales> sales = new ArrayList<>();

        String sql =
            """
            SELECT
                sh.*,
                pm.party_code,
                pm.name,
                pm.email,
                COALESCE(SUM(sl.quantity),0) AS total_qty
            FROM sales_header sh
            LEFT JOIN party_master pm
                ON sh.customer_id = pm.id
            LEFT JOIN sales_line sl
                ON sh.id = sl.sales_id
            GROUP BY sh.id
            ORDER BY
                sh.invoice_date DESC,
                sh.id DESC
            """;

        try (
            Connection con = DatabaseManager.getConnection();

            PreparedStatement ps =
                con.prepareStatement(sql);

            ResultSet rs =
                ps.executeQuery()
        ) {

            while (rs.next()) {

                Sales sale = new Sales();

                sale.setId(
                    rs.getInt("id")
                );

                sale.setInvoiceNo(
                    rs.getString("invoice_no")
                );

                sale.setInvoiceDate(
                    LocalDate.parse(
                        rs.getString("invoice_date")
                    )
                );

                Party customer =
                    new Party();

                customer.setId(
                    rs.getInt("customer_id")
                );

                customer.setPartyCode(
                    rs.getString("party_code")
                );

                customer.setName(
                    rs.getString("name")
                );

                customer.setEmail(
                    rs.getString("email")
                );

                sale.setCustomer(customer);

                sale.setQuantity(
                    rs.getDouble("total_qty")
                );

                sale.setSubtotal(
                    rs.getDouble("subtotal")
                );

                sale.setGstAmount(
                    rs.getDouble("gst_amount")
                );

                sale.setTotalAmount(
                    rs.getDouble("total_amount")
                );

                sale.setRemarks(
                    rs.getString("remarks")
                );

                sale.setCreatedAt(
                    rs.getString("created_at")
                );

                sale.setEmailSent(
                    rs.getInt("email_sent") == 1
                );

                sales.add(sale);

            }

        }
        catch (SQLException ex) {

            throw new RuntimeException(
                "Unable to load sales register.",
                ex
            );

        }

        return sales;

    }



//====================================================
// NEXT SALES INVOICE
//====================================================

    public String nextInvoiceNo() {

        try (

            Connection con =
                DatabaseManager.getConnection();

            Statement st =
                con.createStatement();

            ResultSet rs =
                st.executeQuery(
                    "SELECT COUNT(*) FROM sales_header"
                )

        ) {

            int no = 1;

            if (rs.next()) {

                no =
                    rs.getInt(1) + 1;

            }

            return
                "SAL-"
                    + String.format("%05d", no);

        }
        catch (Exception e) {

            throw new RuntimeException(e);

        }

    }

    //====================================================
// LOAD SINGLE SALES
//====================================================

    public Sales getByInvoice(
        String invoiceNo
    ){

        String headerSql =
            """
            SELECT
                sh.*,
                pm.party_code,
                pm.name,
                pm.email
            FROM sales_header sh
            LEFT JOIN party_master pm
            ON sh.customer_id = pm.id
            WHERE sh.invoice_no=?
            """;


        String lineSql =
            """
            SELECT
                sl.*,
                im.description
            FROM sales_line sl
            LEFT JOIN item_master im
            ON sl.item_code = im.item_code
            WHERE sl.sales_id=?
            ORDER BY sl.id
            """;


        try(
            Connection con =
                DatabaseManager.getConnection()
        ){

            Sales s = null;

            PreparedStatement ps =
                con.prepareStatement(headerSql);

            ps.setString(
                1,
                invoiceNo
            );

            ResultSet rs =
                ps.executeQuery();

            if(rs.next()){

                s = new Sales();

                s.setId(
                    rs.getInt("id")
                );

                s.setInvoiceNo(
                    rs.getString("invoice_no")
                );

                s.setInvoiceDate(
                    LocalDate.parse(
                        rs.getString("invoice_date")
                    )
                );

                Party customer =
                    new Party();

                customer.setId(
                    rs.getInt("customer_id")
                );

                customer.setPartyCode(
                    rs.getString("party_code")
                );

                customer.setName(
                    rs.getString("name")
                );

                customer.setEmail(
                    rs.getString("email")
                );

                s.setCustomer(customer);

                s.setSubtotal(
                    rs.getDouble("subtotal")
                );

                s.setGstAmount(
                    rs.getDouble("gst_amount")
                );

                s.setTotalAmount(
                    rs.getDouble("total_amount")
                );

                s.setRemarks(
                    rs.getString("remarks")
                );

                s.setCreatedAt(
                    rs.getString("created_at")
                );

                s.setEmailSent(
                    rs.getInt("email_sent") == 1
                );

            }

            if(s == null)
                return null;


            List<SalesLine> lines =
                new ArrayList<>();


            PreparedStatement lp =
                con.prepareStatement(lineSql);

            lp.setInt(
                1,
                s.getId()
            );

            ResultSet lr =
                lp.executeQuery();

            while(lr.next()){

                SalesLine line =
                    new SalesLine();

                line.setItemCode(
                    lr.getString("item_code")
                );

                line.setItemDescription(
                    lr.getString("item_code")
                        + " - "
                        + lr.getString("description")
                );

                line.setQuantity(
                    lr.getDouble("quantity")
                );

                line.setRate(
                    lr.getDouble("rate")
                );

                line.setGstPercent(
                    lr.getDouble("gst_percent")
                );

                double net =
                    line.getQuantity()
                        * line.getRate();

                double gstAmount =
                    net
                        * line.getGstPercent()
                        / 100.0;

                double total =
                    net + gstAmount;

                line.setNetAmount(net);

                line.setGstAmount(gstAmount);

                line.setTotalAmount(total);

                line.setLineTotal(total);

                lines.add(line);

            }

            s.setLines(lines);

            return s;

        }
        catch(Exception e){

            throw new RuntimeException(
                "Unable to load sales",
                e
            );

        }

    }

    public void update(Sales sales) {

        String updateHeader =
            """
            UPDATE sales_header
            SET
                invoice_date = ?,
                customer_id = ?,
                subtotal = ?,
                gst_amount = ?,
                total_amount = ?,
                remarks = ?
            WHERE id = ?
            """;

        String deleteLines =
            """
            DELETE FROM sales_line
            WHERE sales_id = ?
            """;

        String insertLine =
            """
            INSERT INTO sales_line
            (
                sales_id,
                item_code,
                quantity,
                rate,
                gst_percent,
                line_total
            )
            VALUES (?,?,?,?,?,?)
            """;

        try(Connection con =
                DatabaseManager.getConnection()) {

            con.setAutoCommit(false);

            try(

                PreparedStatement updatePs =
                    con.prepareStatement(updateHeader);

                PreparedStatement deletePs =
                    con.prepareStatement(deleteLines);

                PreparedStatement linePs =
                    con.prepareStatement(insertLine)

            ){

                updatePs.setString(
                    1,
                    sales.getInvoiceDate().toString()
                );

                updatePs.setInt(
                    2,
                    sales.getCustomer().getId()
                );

                updatePs.setDouble(
                    3,
                    sales.getSubtotal()
                );

                updatePs.setDouble(
                    4,
                    sales.getGstAmount()
                );

                updatePs.setDouble(
                    5,
                    sales.getTotalAmount()
                );

                updatePs.setString(
                    6,
                    sales.getRemarks()
                );

                updatePs.setInt(
                    7,
                    sales.getId()
                );

                updatePs.executeUpdate();

                deletePs.setInt(
                    1,
                    sales.getId()
                );

                deletePs.executeUpdate();

                for(SalesLine line : sales.getLines()){

                    linePs.setInt(
                        1,
                        sales.getId()
                    );

                    linePs.setString(
                        2,
                        line.getItemCode()
                    );

                    linePs.setDouble(
                        3,
                        line.getQuantity()
                    );

                    linePs.setDouble(
                        4,
                        line.getRate()
                    );

                    linePs.setDouble(
                        5,
                        line.getGstPercent()
                    );

                    linePs.setDouble(
                        6,
                        line.getTotalAmount()
                    );

                    linePs.addBatch();

                }

                linePs.executeBatch();

                con.commit();

            }
            catch(Exception e){

                con.rollback();

                throw e;

            }

        }
        catch(Exception e){

            throw new RuntimeException(
                "Unable to update sales",
                e
            );

        }

    }

    public void delete(String invoiceNo) {

        String deleteLines =
            """
            DELETE FROM sales_line
            WHERE sales_id =
            (
                SELECT id
                FROM sales_header
                WHERE invoice_no = ?
            )
            """;

        String deleteHeader =
            """
            DELETE FROM sales_header
            WHERE invoice_no = ?
            """;

        try(Connection con =
                DatabaseManager.getConnection()) {

            con.setAutoCommit(false);

            try(

                PreparedStatement ps1 =
                    con.prepareStatement(deleteLines);

                PreparedStatement ps2 =
                    con.prepareStatement(deleteHeader)

            ){

                ps1.setString(
                    1,
                    invoiceNo
                );

                ps1.executeUpdate();

                ps2.setString(
                    1,
                    invoiceNo
                );

                ps2.executeUpdate();

                con.commit();

            }
            catch(Exception e){

                con.rollback();

                throw e;

            }

        }
        catch(Exception e){

            throw new RuntimeException(
                "Unable to delete sales.",
                e
            );

        }

    }

    public void markEmailSent(int salesId){

        String sql =
            """
            UPDATE sales_header
            SET email_sent = 1
            WHERE id = ?
            """;

        try(

            Connection con =
                DatabaseManager.getConnection();

            PreparedStatement ps =
                con.prepareStatement(sql)

        ){

            ps.setInt(
                1,
                salesId
            );

            ps.executeUpdate();

        }
        catch(SQLException e){

            throw new RuntimeException(
                "Unable to update email status",
                e
            );

        }

    }


}



