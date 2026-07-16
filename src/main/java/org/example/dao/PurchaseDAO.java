package org.example.dao;

import org.example.database.DatabaseManager;
import org.example.model.Party;
import org.example.model.Purchase;
import org.example.model.PurchaseLine;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class PurchaseDAO {
    //====================================================
    // SAVE PURCHASE
    //====================================================
    public void save(Purchase purchase) {


        String headerSql =
            """
            INSERT INTO purchase_header
            (
                invoice_no,
                invoice_date,
                supplier_id,
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
            INSERT INTO purchase_line
            (
                purchase_id,
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
            COALESCE(opening_stock,0)+?
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
                    purchase.getInvoiceNo()
                );


                headerPs.setString(
                    2,
                    purchase.getInvoiceDate().toString()
                );


                headerPs.setInt(
                    3,
                    purchase.getSupplier().getId()
                );


                headerPs.setDouble(
                    4,
                    purchase.getSubtotal()
                );


                headerPs.setDouble(
                    5,
                    purchase.getGstAmount()
                );


                headerPs.setDouble(
                    6,
                    purchase.getTotalAmount()
                );


                headerPs.setString(
                    7,
                    purchase.getRemarks()
                );


                headerPs.executeUpdate();



                ResultSet keys =
                    headerPs.getGeneratedKeys();


                keys.next();


                int purchaseId =
                    keys.getInt(1);



                for(PurchaseLine line :
                    purchase.getLines()) {


                    linePs.setInt(
                        1,
                        purchaseId
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
            catch(Exception e){

                con.rollback();

                throw e;

            }


        }
        catch(Exception e){

            throw new RuntimeException(
                "Unable to save purchase",
                e
            );

        }

    }
    //====================================================
    // PURCHASE REGISTER LOAD
    //====================================================


    public List<Purchase> getAll() {

        List<Purchase> purchases = new ArrayList<>();


        String sql =
            """
           
                SELECT
               ph.*,
               pm.party_code,
               pm.name,
               pm.email,
               COALESCE(SUM(pl.quantity),0) AS total_qty
           FROM purchase_header ph
           LEFT JOIN party_master pm
               ON ph.supplier_id = pm.id
           LEFT JOIN purchase_line pl
               ON ph.id = pl.purchase_id
           GROUP BY
               ph.id
           ORDER BY
               ph.invoice_date DESC,
               ph.id DESC
            """;


        try (
            Connection con = DatabaseManager.getConnection();

            PreparedStatement ps =
                con.prepareStatement(sql);

            ResultSet rs =
                ps.executeQuery()

        ) {


            while(rs.next()) {


                Purchase purchase =
                    new Purchase();


                purchase.setId(
                    rs.getInt("id")
                );


                purchase.setInvoiceNo(
                    rs.getString("invoice_no")
                );


                purchase.setInvoiceDate(
                    LocalDate.parse(
                        rs.getString("invoice_date")
                    )
                );



                Party supplier =
                    new Party();


                supplier.setId(
                    rs.getInt("supplier_id")
                );


                supplier.setPartyCode(
                    rs.getString("party_code")
                );


                supplier.setName(
                    rs.getString("name")
                );


                supplier.setEmail(
                    rs.getString("email")
                );


                purchase.setSupplier(
                    supplier
                );

                purchase.setQuantity(
                    rs.getDouble("total_qty")
                );



                purchase.setSubtotal(
                    rs.getDouble("subtotal")
                );


                purchase.setGstAmount(
                    rs.getDouble("gst_amount")
                );


                purchase.setTotalAmount(
                    rs.getDouble("total_amount")
                );


                purchase.setRemarks(
                    rs.getString("remarks")
                );


                purchase.setCreatedAt(
                    rs.getString("created_at")
                );


                purchase.setEmailSent(
                    rs.getInt("email_sent") == 1
                );


                purchases.add(
                    purchase
                );

            }



        }
        catch(SQLException ex) {


            throw new RuntimeException(
                "Unable to load purchase register.",
                ex
            );

        }


        return purchases;

    }
    //====================================================
    // NEXT INVOICE
    //====================================================
    public String nextInvoiceNo(){


        try(
            Connection con =
                DatabaseManager.getConnection();

            Statement st =
                con.createStatement();

            ResultSet rs =
                st.executeQuery(
                    "SELECT COUNT(*) FROM purchase_header"
                )

        ){

            int no=1;


            if(rs.next()){

                no =
                    rs.getInt(1)+1;

            }


            return
                "PUR-"
                    +String.format("%05d",no);


        }
        catch(Exception e){

            throw new RuntimeException(e);

        }

    }

    public Purchase getByInvoice(String invoiceNo){


        String headerSql =
            """
            SELECT
                ph.*,
                pm.party_code,
                pm.name,
                pm.email
            FROM purchase_header ph
            LEFT JOIN party_master pm
            ON ph.supplier_id = pm.id
            WHERE ph.invoice_no=?
            """;



        String lineSql =
            """
            SELECT
                pl.*,
                im.description
            FROM purchase_line pl
            LEFT JOIN item_master im
            ON pl.item_code = im.item_code
            WHERE pl.purchase_id=?
            ORDER BY pl.id
            """;



        try(
            Connection con =
                DatabaseManager.getConnection()

        ){



            Purchase p=null;



            PreparedStatement ps =
                con.prepareStatement(headerSql);


            ps.setString(
                1,
                invoiceNo
            );


            ResultSet rs =
                ps.executeQuery();



            if(rs.next()){


                p=new Purchase();


                p.setId(
                    rs.getInt("id")
                );


                p.setInvoiceNo(
                    rs.getString("invoice_no")
                );


                p.setInvoiceDate(
                    LocalDate.parse(
                        rs.getString("invoice_date")
                    )
                );


                Party party =
                    new Party();


                party.setId(
                    rs.getInt("supplier_id")
                );


                party.setName(
                    rs.getString("name")
                );


                party.setPartyCode(
                    rs.getString("party_code")
                );


                party.setEmail(
                    rs.getString("email")
                );


                p.setSupplier(party);



                p.setSubtotal(
                    rs.getDouble("subtotal")
                );


                p.setGstAmount(
                    rs.getDouble("gst_amount")
                );


                p.setTotalAmount(
                    rs.getDouble("total_amount")
                );


                p.setRemarks(
                    rs.getString("remarks")
                );

            }



            if(p==null)
                return null;



            List<PurchaseLine> lines =
                new ArrayList<>();



            PreparedStatement lp =
                con.prepareStatement(lineSql);


            lp.setInt(
                1,
                p.getId()
            );


            ResultSet lr =
                lp.executeQuery();



            while (lr.next()) {

                PurchaseLine line =
                    new PurchaseLine();

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

                // Recalculate amounts
                double net =
                    line.getQuantity() * line.getRate();

                double gstAmount =
                    net * line.getGstPercent() / 100.0;

                double total =
                    net + gstAmount;

                line.setNetAmount(net);

                line.setGstAmount(gstAmount);

                line.setTotalAmount(total);

                // Keep compatibility with existing code
                line.setLineTotal(total);

                lines.add(line);

            }


            p.setLines(lines);


            return p;


        }
        catch(Exception e){

            throw new RuntimeException(
                "Unable to load purchase",
                e
            );

        }


    }
    //====================================================
// UPDATE PURCHASE
//====================================================

    public void update(Purchase purchase) {


        String updateHeader =
            """
            UPDATE purchase_header
            SET 
                invoice_date = ?,
                supplier_id = ?,
                subtotal = ?,
                gst_amount = ?,
                total_amount = ?,
                remarks = ?
            WHERE id = ?
            """;


        String deleteLines =
            """
            DELETE FROM purchase_line
            WHERE purchase_id = ?
            """;


        String insertLine =
            """
            INSERT INTO purchase_line
            (
                purchase_id,
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
                    purchase.getInvoiceDate().toString()
                );


                updatePs.setInt(
                    2,
                    purchase.getSupplier().getId()
                );


                updatePs.setDouble(
                    3,
                    purchase.getSubtotal()
                );


                updatePs.setDouble(
                    4,
                    purchase.getGstAmount()
                );


                updatePs.setDouble(
                    5,
                    purchase.getTotalAmount()
                );


                updatePs.setString(
                    6,
                    purchase.getRemarks()
                );


                updatePs.setInt(
                    7,
                    purchase.getId()
                );


                updatePs.executeUpdate();



                // remove old lines

                deletePs.setInt(
                    1,
                    purchase.getId()
                );


                deletePs.executeUpdate();



                // insert updated lines

                for(PurchaseLine line :
                    purchase.getLines()) {


                    linePs.setInt(
                        1,
                        purchase.getId()
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
                "Unable to update purchase",
                e
            );

        }

    }

    public void delete(String invoiceNo) {

        String deleteLines =
            """
            DELETE FROM purchase_line
            WHERE purchase_id =
            (
                SELECT id
                FROM purchase_header
                WHERE invoice_no = ?
            )
            """;


        String deleteHeader =
            """
            DELETE FROM purchase_header
            WHERE invoice_no = ?
            """;


        try(Connection con = DatabaseManager.getConnection()) {

            con.setAutoCommit(false);


            try(
                PreparedStatement ps1 =
                    con.prepareStatement(deleteLines);

                PreparedStatement ps2 =
                    con.prepareStatement(deleteHeader)
            ) {


                ps1.setString(1, invoiceNo);
                ps1.executeUpdate();


                ps2.setString(1, invoiceNo);
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
                "Unable to delete purchase.",
                e
            );

        }

    }

    public void markEmailSent(int purchaseId){


        String sql =
            """
            UPDATE purchase_header
            SET email_sent=1
            WHERE id=?
            """;


        try(
            Connection con =
                DatabaseManager.getConnection();

            PreparedStatement ps =
                con.prepareStatement(sql)

        ){


            ps.setInt(
                1,
                purchaseId
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
