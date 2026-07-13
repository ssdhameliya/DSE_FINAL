package org.example.service;

import org.example.dao.PurchaseDAO;
import org.example.model.Purchase;

public class PurchaseService {
    private final PurchaseDAO dao = new PurchaseDAO();

    public void save(Purchase purchase) {
        dao.save(purchase);
    }

    public String nextInvoiceNo() {
        return dao.nextInvoiceNo();
    }
}
