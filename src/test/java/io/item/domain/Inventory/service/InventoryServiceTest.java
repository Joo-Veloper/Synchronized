package io.item.domain.Inventory.service;

import io.item.domain.Inventory.entity.Inventory;
import io.item.domain.Inventory.repository.InventoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

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


    @Test
    public void inventoryReduction(){
        inventoryService.decrease(1L, 1L);

        Inventory inventory = inventoryRepository.findById(1L).orElseThrow();

        assertEquals(99, inventory.getQuantity());
    }
}