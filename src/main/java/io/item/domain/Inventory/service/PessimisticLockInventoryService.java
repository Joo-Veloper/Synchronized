package io.item.domain.Inventory.service;

import io.item.domain.Inventory.entity.Inventory;
import io.item.domain.Inventory.repository.InventoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class PessimisticLockInventoryService {
    private final InventoryRepository inventoryRepository;

    @Transactional
    public void decrease(Long id, Long quantity) {
        Inventory inventory = inventoryRepository.findByIdWithPessimisticLock(id);

        inventory.decrease(quantity);

        inventoryRepository.save(inventory);
    }
}
