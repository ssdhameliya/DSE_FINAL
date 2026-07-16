package org.example.model;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class Sales {

    private int id;

    private String createdAt;

    private String invoiceNo;

    private LocalDate invoiceDate;

    private Party customer;

    private double subtotal;

    private double gstAmount;

    private double totalAmount;

    private String remarks;

    private double quantity;

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }
    private boolean emailSent;

    private List<SalesLine> lines;


    public int getId() {
        return id;
    }


    public void setId(int id) {
        this.id = id;
    }


    public String getCreatedAt() {
        return createdAt;
    }


    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }


    public String getInvoiceNo() {
        return invoiceNo;
    }


    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }


    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }


    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }


    public Party getCustomer() {
        return customer;
    }


    public void setCustomer(Party customer) {
        this.customer = customer;
    }


    public double getSubtotal() {
        return subtotal;
    }


    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }


    public double getGstAmount() {
        return gstAmount;
    }


    public void setGstAmount(double gstAmount) {
        this.gstAmount = gstAmount;
    }


    public double getTotalAmount() {
        return totalAmount;
    }


    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }


    public String getRemarks() {
        return remarks;
    }


    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }



    //====================================================
    // EMAIL STATUS
    //====================================================

    public boolean isEmailSent() {

        return emailSent;

    }


    public void setEmailSent(boolean emailSent) {

        this.emailSent = emailSent;

    }



    public List<SalesLine> getLines() {
        return lines;
    }


    public void setLines(List<SalesLine> lines) {
        this.lines = lines;
    }



}
