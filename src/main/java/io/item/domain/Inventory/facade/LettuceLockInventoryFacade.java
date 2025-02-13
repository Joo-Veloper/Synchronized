package io.item.domain.Inventory.facade;

import io.item.domain.Inventory.repository.RedisLockRepository;
import io.item.domain.Inventory.service.InventoryService;
import org.springframework.stereotype.Component;

@Component
public class LettuceLockInventoryFacade {
    private RedisLockRepository redisLockRepository;
    private InventoryService inventoryService;

    public LettuceLockInventoryFacade(RedisLockRepository redisLockRepository, InventoryService inventoryService) {
        this.redisLockRepository = redisLockRepository;
        this.inventoryService = inventoryService;
    }

    public void decrease(Long id, Long quantity) throws InterruptedException {
        while (!redisLockRepository.lock(id)) {
            Thread.sleep(100);
        }

        try{
            inventoryService.decrease(id, quantity);
        }finally {
            redisLockRepository.unlock(id);
        }
    }
}
