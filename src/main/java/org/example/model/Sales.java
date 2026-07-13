package org.example.model;

import java.time.LocalDate;
import java.util.List;

public class Sales {
    private String invoiceNo;
    private LocalDate invoiceDate;
    private Party customer;
    private double subtotal;
    private double gstAmount;
    private double totalAmount;
    private String remarks;
    private List<PurchaseLine> lines;

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String value) {
        invoiceNo = value;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate value) {
        invoiceDate = value;
    }

    public Party getCustomer() {
        return customer;
    }

    public void setCustomer(Party value) {
        customer = value;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double value) {
        subtotal = value;
    }

    public double getGstAmount() {
        return gstAmount;
    }

    public void setGstAmount(double value) {
        gstAmount = value;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double value) {
        totalAmount = value;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String value) {
        remarks = value;
    }

    public List<PurchaseLine> getLines() {
        return lines;
    }

    public void setLines(List<PurchaseLine> value) {
        lines = value;
    }
}
