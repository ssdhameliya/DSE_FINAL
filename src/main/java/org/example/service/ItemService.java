package org.example.service;

import org.example.dao.ItemDAO;
import org.example.model.Item;

import java.util.List;

public class ItemService {

    private final ItemDAO dao = new ItemDAO();

    public void save(Item item) {

        dao.save(item);

    }

    public void update(Item item) {
        dao.update(item);
    }

    public void delete(String itemCode) {
        dao.delete(itemCode);
    }

    public List<Item> getAll() {
        return dao.getAll();
    }

    public void saveOrUpdate(Item item) {
        dao.saveOrUpdate(item);
    }

}
