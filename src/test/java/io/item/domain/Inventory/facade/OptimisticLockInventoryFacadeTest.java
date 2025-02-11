package io.item.domain.Inventory.facade;

import io.item.domain.Inventory.entity.Inventory;
import io.item.domain.Inventory.repository.InventoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
@SpringBootTest
class OptimisticLockInventoryFacadeTest {
    @Autowired
    private OptimisticLockInventoryFacade optimisticLockInventoryFacade;

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    public void before() {
        inventoryRepository.saveAndFlush(new Inventory(1L, 100L));
    }

    @AfterEach
    public void after() {
        inventoryRepository.deleteAll();
    }

    @DisplayName("동시에 100개의 요청")
    @Test
    public void RequestsAtTheSameTime() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    optimisticLockInventoryFacade.decrease(1L, 1L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        Inventory inventory = inventoryRepository.findById(1L).orElseThrow();

        // 100 - (100 * 1) = 0
        assertEquals(0, inventory.getQuantity());
    }
}