package io.item.domain.Inventory.service;

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
class InventoryServiceTest {
    @Autowired
    private InventoryService inventoryService;

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


    @DisplayName("재고 감소")
    @Test
    public void inventoryReduction(){
        inventoryService.decrease(1L, 1L);

        Inventory inventory = inventoryRepository.findById(1L).orElseThrow();

        assertEquals(99, inventory.getQuantity());
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
                    inventoryService.decrease(2L, 1L);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        Inventory inventory = inventoryRepository.findById(2L).orElseThrow();

        // 100 - (100 * 1) = 0
        assertEquals(0, inventory.getQuantity());
    }
}