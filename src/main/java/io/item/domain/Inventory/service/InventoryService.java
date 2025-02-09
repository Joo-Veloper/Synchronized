package io.item.domain.Inventory.service;

import io.item.domain.Inventory.entity.Inventory;
import io.item.domain.Inventory.repository.InventoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;

    // @Transactional
    public synchronized void decrease(Long id, Long quantity) {
        Inventory inventory = inventoryRepository.findById(id).orElseThrow();
        inventory.decrease(quantity);

        inventoryRepository.save(inventory);
    }
}
