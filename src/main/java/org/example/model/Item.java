package org.example.model;

public class Item {
    private String itemCode;
    private String description;
    private String category;
    private String brand;
    private String material;
    private String size;
    private String unit;
    private String hsn;

    // Use wrapper types so we can represent missing values as null
    private Double gst;
    private Double purchasePrice;
    private Double sellingPrice;
    private Double openingStock;
    private Double minimumStock;

    private String location;
    private String remarks;

    // --- getters / setters ---
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getHsn() { return hsn; }
    public void setHsn(String hsn) { this.hsn = hsn; }

    public Double getGst() { return gst; }
    public void setGst(Double gst) { this.gst = gst; }

    public Double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(Double purchasePrice) { this.purchasePrice = purchasePrice; }

    public Double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(Double sellingPrice) { this.sellingPrice = sellingPrice; }

    public Double getOpeningStock() { return openingStock; }
    public void setOpeningStock(Double openingStock) { this.openingStock = openingStock; }

    public Double getMinimumStock() { return minimumStock; }
    public void setMinimumStock(Double minimumStock) { this.minimumStock = minimumStock; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
