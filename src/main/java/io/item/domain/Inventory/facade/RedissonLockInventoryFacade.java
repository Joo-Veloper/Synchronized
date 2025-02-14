package io.item.domain.Inventory.facade;

import io.item.domain.Inventory.service.InventoryService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedissonLockInventoryFacade {
    private RedissonClient redissonClient;
    private InventoryService inventoryService;

    public RedissonLockInventoryFacade(RedissonClient redissonClient, InventoryService inventoryService) {
        this.redissonClient = redissonClient;
        this.inventoryService = inventoryService;
    }

    public void decrease(Long id, Long quantity) {
        RLock lock = redissonClient.getLock(id.toString());
        try {
            boolean available = lock.tryLock(10, 1, TimeUnit.SECONDS);

            if (!available) {
                System.out.println("lock획득 실패");
                return;
            }
            inventoryService.decrease(id, quantity);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}
