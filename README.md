# Synchronized

## Race Condition(경쟁 상태)란?
여러 개의 쓰레드가 동시에 공유 자원에 접근할 때, 실행 순서나 타이밍에 따라 예상치 못한 결과가 발생하는 현상을 `Race Condition(경쟁 상태)` 라고 합니다.</br>
🔹예시
예를 들어, 어떤 상품의 재고가 10개가 있다고 가정할 때
1. 쓰레드 A가 재고를 읽어와 1개를 차감하려고 할 때
2. 쓰레드 B도 **동시에 같은 재고**를 읽어와서 **1개를 차감**하려고 할 때
3. 쓰레드 A가 재고 = **9로 저장**
4. 쓰레드 B도 재고 = **9로 저장**

원래는 **2개가 감소**해야 하는데, **결과적을 재고 9개로 유지**됨
이렇게 동시에 실행되면서 예상과 다른 값이 나오는 상황을 **RaceCondition**

## Pessimistic Lock(비관적 락)
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
- `findByIdWithPessimisticLock(id)` -> 데이터를 조회할 때 비관적 락을 사용해서 다른 트랜잭션이 접근하지 못하도록 설정(이렇게 하면 동시에 여러 요청이 와도 한 번에 하나씩만 실행되므로 Race Condition이 발생하지 않습니다.)
- `inventory.decrease(quantity);` -> 재고 차감
- `inventoryRepository.save(inventory);` -> 차감된 데이터를 저장

### InventoryServiceTest 코드 분석

```java
@DisplayName("재고 감소")
@Test
public void inventoryReduction() {
    inventoryService.decrease(1L, 1L); // decrease(1L, 1L)을 호출하면 재고가 1개 줄어든다는 것을 확인하는 테스트

    Inventory inventory = inventoryRepository.findById(1L).orElseThrow();

    assertEquals(99, inventory.getQuantity()); // assertEquals(99, inventory.getQuantity()) 100 개에서 1개 감소했으니 99개가 맞는지 확인
}
```

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


## Optimistic Lock(낙관적 락)
낙관적락(Optimistic Lock)은 다른 트랜잭션이 같은 데이터를 수정할 가능서이 낮다고 가정하고 일단 변경을 시도한 후 충돌이 발생하면 롤백하고 다시 시도 하는 방식
### 비관적 락(Pessimistic)과의 차이
비관적 락 : 데이터를 조회할 때 다른 트랜잭션이 접근하지 못하도록 막습니다.
낙관적 락 : 일단 변경을 허용하지만, 저장할 때 다른 트랜잭션이 변경했으면 예외가 발생합니다.

### 낙관적 락이 유용한 경우
- 트랜잭션 충돌이 자주 발생하지 않는 경우(ex -> 동시 수정 가능성이 낮을 때)
- 데이터베이스 락을 최소화 하여 성능을 최적화 하고 싶을 때

### Inventory Repository에서의 낙관적 락 적용
```java
@Lock(LockModeType.OPTIMISTIC)  // 낙관적 락 사용
@Query("select s from Inventory s where s.id = :id") // 엔티티를 수정할 때 버전 정보를 확인해서 충돌이 발생한다면 예외를 던집니다.
Inventory findByIdWithOptimisticLock(Long id); // findByIdWithOptimisticLock(id)로 Inventory 엔티티를 조회하면, 업데이트 할 때 충돌 여부 확인
```

### OptimisticLockInventoryService 재시도 로직
```java
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
```
- 예외가 발생하면 50ms 기다렸다가 다시 시도(이렇게 만들면 충돌이 발생하도 여러번 시도해서 성공할 가능성이 높아집니다.)
- Pessimistic Lock 은 처음부터 다른 트랜잭션을 막아비리지만 Optimistic Lock 은 트랜잭션이 실패하면 다시 시도합니다.

### Named Lock(네임드 락)
네임드 락은 MySQL에서 제공하는 데이터베이스 수준의 락 기능 중 하나로 특정 이름(문자열)을 기반으로 락을 걸고 해제하는 방식입니다.

네임드 락을 사용하려면 하나의 특정 리소스(ex : 특정 상품 ID)를 대상으로 락을 적용하여 동시성 문제를 방지할 수 있습니다.

### 네임드 락 핵심
`GET_LOCK('key', timeout)`
- key(문자열)을 기준으로 락을 설정하고, timeout(초) 동안 락이 해제되기를 기다립니다.
- 락이 걸리면 1을 반환, 실패하면 0을 반환
`RELEASE_LOCK('key')`
- 해당 key의 락을 해제
- 락을 해제하면 다른 프로세스가 이 리소스를 사용할 수 있습니다.
네임드 락은 트랜잭션과 무관하게 동작합니다.
- 즉, 트랜잭션이 롤백되더라도 락이 유지되므로 반드시 해제해야 합니다.

### 네임드 락을 사용하는 이유
##### 기본 방법(synchronized, PESSIMISTIC_LOCK)의 한계
1. synchronized : JVM 내에서만 동작 -> 멀티 인스턴스 환경에서는 락을 보장할 수 없음.
2. PESSIMISTIC_LOCK : 트랜잭션 내에서만 유효 -> 트랜잭션이 길어지면 락 점유 시간이 증가하여 성능 저하 발생
3. OPTIMISTIC_LOCK : 충돌 발생 시 재시도 필요 -> 고객 경험(UX)가 나빠질 수 있음
네임드 락을 사용하면, 멀티 인스턴스 환경에서도 락을 적용할 수 있으며, 트랜잭션 범위를 벗어나도 락이 유지될 수 있습니다.

### 네임드 락 적용 (NamedLockInventoryFacade)

```java
@Component
@AllArgsConstructor
public class NamedLockInventoryFacade {
    private final LockRepository lockRepository;
    private final InventoryService inventoryService;

    public void decrease(Long id, Long quantity) {
        try {
            lockRepository.getLock(id.toString()); // 🔒 해당 id에 대한 네임드 락을 획득.
            inventoryService.decrease(id, quantity); //상품 재고 감소 로직 실행.
        } finally {
            lockRepository.releaseLock(id.toString()); // 🔓 락 해제 락을 해제하여 다른 요청이 접근할 수 있도록 함.
        }
    }
}
```

### LockRepository()
```java
public interface LockRepository extends JpaRepository<Inventory, Long> {
    @Query(value = "SELECT GET_LOCK(:key, 3000)", nativeQuery = true) // key(상품 ID)로 락을 설정하고 최대 3초 동안 기다림.
    void getLock(String key);

    @Query(value = "SELECT RELEASE_LOCK(:key)", nativeQuery = true) //해당 key에 대한 락을 해제.
    void releaseLock(String key);
}
```

### InventoryService (재고 감소 로직)
```java
@Service
@AllArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;

    /*트랜잭션 설정 (@Transactional(propagation = Propagation.REQUIRES_NEW))
    별도의 새로운 트랜잭션을 생성하여 실행 → 락이 오래 유지되지 않도록 함.
    트랜잭션이 실패해도 네임드 락은 유지되므로 반드시 releaseLock()을 호출해야 함.*/
    @Transactional(propagation = Propagation.REQUIRES_NEW) 
    public void decrease(Long id, Long quantity) {
        Inventory inventory = inventoryRepository.findById(id).orElseThrow();
        inventory.decrease(quantity);

        inventoryRepository.save(inventory);
    }
}

```

### 네임드 락의 장점과 주의점
장점
1. 멀티 인스턴스 환경에서도 동작 - synchronized 는 JVM 내에서만 동작하지만, 네임드 락은 DB 기반이라 여러 서버에서도 공유 가능
2. 트랜잭션과 독립적으로 사용 가능 - PESSIMISTIC_LOCK 은 트랜잭션이 길어질 경우 성능 저하가 발생하지만, 네임드 락은 별도 관리 가능
3. 재고 관리, 예약 시스템 등에서 유용 - 특정 리소스(상품 ID 둥)에 대한 경쟁 조건을 방지할 때 사용 가능

주의점
1. 락을 반드시 해제해야 함 - 트랜잭션이 종료되더라도 네임드 락은 자동 해제되지 않음, 따라 finally 블록에서 releaseLock()을 호출
2. 락 점유 시간이 길어지면 성능 저하 기능 - GET_LOCK(:key, timeout)에서 timeout을 적절하게 설정해야 합니다. , timeout 을 너무 길게 잡으면 시스템이 멈추는 것처럼 보일 수 있음
3. 트랜잭션 롤백과 별개로 동작 - inventoryService.decrease()에서 예외가 발생하면 트랜잭션은 롤백되지만 네임드 락은 유지됨, 따라서 releaseLock()을 호출하지 않으면 다른 요청이 영원히 블록될 수 있습니다.

redis 를 활용한 방법은 MYSQL의 NamedLock 와 비슷합니다.
다른 점으로는 Redis를 이용한다는 점과 Session 관리에 신경을 안써도 된다는 점입니다.

# Redis
redis를 활용한 방법은 MySQL의 NamedLock과 비슷합니다. 다른점은 Redis를 이용한다는 점과 Session 관리에 신경을 안써도 된다는 점입니다.

### Lettuce란?
Lettuce는 Spring에서 Redis와 통신할 때 사용하는 클라이언트 라이브러리 중 하나입니다. 쉽게 말하면 Spring과 Redis가 데이터를 주고받을 수 있도록 도와주는 도구라고 할 수 있습니다.
 
### Lettuce를 사용하면 좋은 경우
💡 그냥 캐싱만 한다고 가정하면 Jedis도 괜찮다 하지만 멀티스레드, 높은 트래픽, 동시성 처리가 필요할 때는 Lettuce를 사용합니다.

---
1. 비동기 & 높은 성능이 필요한 경우 (Lettuce 는 비동기 방식으로 동작할 수 있어서, 성능이 뛰어나고 확장성이 좋습니다.)
    - 싱글 스레드 기반이지만, 넌블로킹 방식(요청을 기다리지 않고 다음 작업 실행)이라 빠르게 처리할 수 있습니다.
    - 특히 트래픽이 많은 시스템에서는 Jedis보다 Lettuce가 더 효율적일 수 있습니다. </br>
      💡언제 유용한가? -> 대규모 트래픽이 들어오는 실시간 서비스에서 유용!

---
2. .Redis 락 같은 동시성 제어가 필요할 때(Lettuce를 사용하면 Redis를 이용한 락을 쉽게 구현할 수 있다.)
    - 여러 개의 요청이 동시에 같은 데이터를 수정하려고 할 때 한 번에 하나씩만 실행되도록 제한 가능합니다.
    - 재고 관리 시스템이나 은행 계좌 거래 같은 경우 동시성 제어를 필수로 사용합니다


### Code 에서 Lettuce 역활
락은 여러개의 요청이 동시에 같은 데이터를 수정하려 할 때, 한 번에 하나의 요청만 처리할 수 있도록 막아주는 역할을 합니다.</br>
Redis를 이용해서 락을 거는 이유는 분산 환경에서도 데이터 충돌 없이 안전하게 처리하기 위해서 사용합니다. 즉 여러 개의 서버가 동시에 같은 재고를 줄이려고 할 때, Lettuce를 사용한 Redis 락을 통해 한 번에 하나의 요청만 처리되도록 보장합니다.

### 락을 관리하는 RedisLockRepository
```java
@Component
public class RedisLockRepository {
    private RedisTemplate<String, String> redisTemplate;

    public RedisLockRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Boolean lock(Long key) { // Redis 에 "lock"이라는 값을 저장해서 락을 건다. (setIfAbsent 사용)
        return redisTemplate
                .opsForValue()
                .setIfAbsent(generateKey(key), "lock", Duration.ofMillis(3_000));
                /* 
                setIfAbsent는 값이 없을 때만 저장하는 함수라서, 누군가 먼저 락을 걸면 다른 요청은 락을 못 건다
                즉, 한 번에 하나의 요청만 락을 가질 수 있도록 보장, 락의 유효 시간은 3초(3,000ms).
                */
    }

    public Boolean unlock(Long key) { // Redis 에서 "lock" 값을 삭제해서 락을 푼다.
        return redisTemplate.delete(generateKey(key));
    }

    private String generateKey(Long key) { // Redis 에 저장할 키 값(String)을 생성.
        return key.toString();
    }
}
```
### 락을 이용해 재고 줄이는 LettuceLockInventoryFacade
```java
@Component
public class LettuceLockInventoryFacade {
    private RedisLockRepository redisLockRepository;
    private InventoryService inventoryService;

    public LettuceLockInventoryFacade(RedisLockRepository redisLockRepository, InventoryService inventoryService) {
        this.redisLockRepository = redisLockRepository;
        this.inventoryService = inventoryService;
    }

    public void decrease(Long id, Long quantity) throws InterruptedException { //특정 id의 재고를 줄이는 메서드
        while (!redisLockRepository.lock(id)) { // 락을 걸 수 있을 때까지 100ms마다 재시도하면서 기다린다.
            Thread.sleep(100);                  // 만약 다른 요청이 먼저 락을 걸었다면, 현재 요청은 락이 풀릴 때까지 기다려야 해.
        }

        try {
            inventoryService.decrease(id, quantity); // 재고를 실제로 줄이는 로직 실행
        } finally {
            redisLockRepository.unlock(id); // 재고를 줄인 후에는 꼭 락을 해제해야 한다.
                                            // 락을 안 풀어주면 다른 요청이 영원히 기다리게 되니까 주의!!!!!!!!
        }
    }
}
```