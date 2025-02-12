package io.item.domain.Inventory.facade;

import io.item.domain.Inventory.repository.LockRepository;
import io.item.domain.Inventory.service.InventoryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class NamedLockInventoryFacade {
    private final LockRepository lockRepository;

    private final InventoryService inventoryService;

    public void decrease(Long id, Long quantity) {
        try{
            lockRepository.getLock(id.toString());
            inventoryService.decrease(id, quantity);
        }finally {
            lockRepository.releaseLock(id.toString());
        }
    }
}
