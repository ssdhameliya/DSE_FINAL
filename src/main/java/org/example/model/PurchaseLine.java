package org.example.model;


public class PurchaseLine implements InvoiceLine {

    private String itemCode;

    private String itemDescription;

    private double quantity;

    private double rate;

    private double gstPercent;

    private double gstAmount;

    private double netAmount;

    private double totalAmount;


    public PurchaseLine() {
    }


    public String getItemCode() {

        return itemCode;
    }


    public void setItemCode(String itemCode) {

        this.itemCode = itemCode;
    }


    @Override
    public String getItemDescription() {

        return itemDescription;
    }


    public void setItemDescription(String itemDescription) {

        this.itemDescription = itemDescription;
    }


    @Override
    public double getQuantity() {

        return quantity;
    }


    public void setQuantity(double quantity) {

        this.quantity = quantity;
    }


    @Override
    public double getRate() {

        return rate;
    }


    public void setRate(double rate) {

        this.rate = rate;
    }


    @Override
    public double getGstPercent() {

        return gstPercent;
    }


    public void setGstPercent(double gstPercent) {

        this.gstPercent = gstPercent;
    }


    public double getGstAmount() {

        return gstAmount;
    }


    public void setGstAmount(double gstAmount) {

        this.gstAmount = gstAmount;
    }


    public double getNetAmount() {

        return netAmount;
    }


    public void setNetAmount(double netAmount) {

        this.netAmount = netAmount;
    }


    public double getTotalAmount() {

        return totalAmount;
    }


    public void setTotalAmount(double totalAmount) {

        this.totalAmount = totalAmount;
    }



    @Override
    public double getLineTotal() {

        return totalAmount;
    }


    public void setLineTotal(double lineTotal) {

        this.totalAmount = lineTotal;
    }



    public void calculateAmounts() {

        netAmount =
            quantity * rate;


        gstAmount =
            netAmount * gstPercent / 100.0;


        totalAmount =
            netAmount + gstAmount;

    }

}
