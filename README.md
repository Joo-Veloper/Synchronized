## Synchronized

### Race Condition(경쟁 상태)란?
여러 개의 쓰레드가 동시에 공유 자원에 접근할 때, 실행 순서나 타이밍에 따라 예상치 못한 결과가 발생하는 현상을 `Race Condition(경쟁 상태)` 라고 합니다.</br>
🔹예시
예를 들어, 어떤 상품의 재고가 10개가 있다고 가정할 때
1. 쓰레드 A가 재고를 읽어와 1개를 차감하려고 할 때
2. 쓰레드 B도 **동시에 같은 재고**를 읽어와서 **1개를 차감**하려고 할 때
3. 쓰레드 A가 재고 = **9로 저장**
4. 쓰레드 B도 재고 = **9로 저장**

원래는 **2개가 감소**해야 하는데, **결과적을 재고 9개로 유지**됨
이렇게 동시에 실행되면서 예상과 다른 값이 나오는 상황을 **RaceCondition**

### Pessimistic Lock(비관적 락)
비관전 락은 `다른 쓰레다 이 데이터를 변경할 수도 있으니, 아예 못 건드리게 막자` 라는 전략입니다.</br>
🔹특징
 - 트랜잭션이 데이터를 가져올 때 다른 트랜잭션이 접근하지 못하도록 락을 걸어버립니다.
 - 다른 트랜잭션은 락이 풀릴 때까지 기다려야 합니다.**(= 성능이 떨어질 수 있음)**
 - 하지만 Race Condition 을 완전히 방지할 수 있습니다.

```java
@Transactional
public void decrease(Long id, Long quantity) {
    Inventory inventory = inventoryRepository.findByIdWithPessimisticLock(id);

    inventory.decrease(quantity);

    inventoryRepository.save(inventory);
}
```

### 이렇게 구현 이유
`findByIdWithPessimisticLock(id)` -> 데이터를 조회할 때 비관적 락을 사용해서 다른 트랜잭션이 접근하지 못하도록 설정
이렇게 하면 동시에 여러 요청이 와도 한 번에 하나씩만 실행되므로 Race Condition이 발생하지 않습니다.
`inventory.decrease(quantity);` -> 재고 차감
`inventoryRepository.save(inventory);` -> 차감된 데이터를 저장

### InventoryServiceTest 코드 분석

```java
@DisplayName("재고 감소")
@Test
public void inventoryReduction() {
    inventoryService.decrease(1L, 1L);

    Inventory inventory = inventoryRepository.findById(1L).orElseThrow();

    assertEquals(99, inventory.getQuantity());
}
```
`decrease(1L, 1L)`을 호출하면 재고가 1개 줄어든다는 것을 확인하는 테스트
`assertEquals(99, inventory.getQuantity())` 100 개에서 1개 감소했으니 99개가 맞는지 확인

### RequestAtTheSameTime() 동시에 100개의 요청을 보낼 때

```java
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
```
### 결과 분석
```java
assertEquals(0, inventory.getQuantity());
```
- 처음 재고는 100개
- 100개의 쓰레드가 동시에 1개씩 감소 요청
- `Pessimistic Lock`을 사용했으므로 한 번에 하나의 요청만 처리
- 결과적은 100 - (100 * 1) = 0 이 정확히 맞음
