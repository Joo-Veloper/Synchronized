package io.item.domain.Inventory.Transaction;

import io.item.domain.Inventory.service.InventoryService;

public class TransactionInventoryService {
    private InventoryService inventoryService;

    public TransactionInventoryService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public void decrease(Long id, Long quantity) {
        startTransaction();
        inventoryService.decrease(id, quantity);
        endTransaction();
    }

    private void startTransaction() {
        System.out.println("Transaction Start");
    }

    private void endTransaction() {
        System.out.println("Transaction End");
    }
}
