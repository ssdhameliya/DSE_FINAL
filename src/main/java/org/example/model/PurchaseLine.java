package org.example.model;

public class PurchaseLine {
    private String itemCode;
    private String itemDescription;
    private double quantity;
    private double rate;
    private double gstPercent;
    private double lineTotal;

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public double getGstPercent() {
        return gstPercent;
    }

    public void setGstPercent(double gstPercent) {
        this.gstPercent = gstPercent;
    }

    public double getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(double lineTotal) {
        this.lineTotal = lineTotal;
    }
}
