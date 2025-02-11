package io.item.domain.Inventory.facade;

import io.item.domain.Inventory.service.OptimisticLockInventoryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class OptimisticLockInventoryFacade {
    private final OptimisticLockInventoryService optimisticLockInventoryService;

    public void decrease(Long id, Long quantity) throws InterruptedException {
        while (true) {
            try {
                optimisticLockInventoryService.decrease(id, quantity);

                break;
            } catch (Exception e) {
                Thread.sleep(50);
            }
        }
    }
}
