package org.example.service;

import org.example.dao.SalesDAO;
import org.example.model.Sales;

public class SalesService {
    private final SalesDAO dao = new SalesDAO();

    public void save(Sales sales) {
        dao.save(sales);
    }

    public String nextInvoiceNo() {
        return dao.nextInvoiceNo();
    }
}
