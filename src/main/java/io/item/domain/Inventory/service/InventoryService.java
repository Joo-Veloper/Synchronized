package io.item.domain.Inventory.service;

import io.item.domain.Inventory.entity.Inventory;
import io.item.domain.Inventory.repository.InventoryRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

@Service
@AllArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decrease(Long id, Long quantity) {
        Inventory inventory = inventoryRepository.findById(id).orElseThrow();
        inventory.decrease(quantity);

        inventoryRepository.save(inventory);
    }
}
