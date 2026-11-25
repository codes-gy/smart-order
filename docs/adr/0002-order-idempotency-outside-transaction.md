# 주문 멱등성은 회원 단위 키와 트랜잭션 밖 재조회로 보장한다

같은 멱등성 키로 주문이 동시에 두 번 오면, 늦은 쪽은 쿠폰·스탬프 조건부 UPDATE에서 `ConflictException`을 받거나 `(member_id, idempotency_key)` 유니크 위반을 받는다. 이 예외를 같은 트랜잭션 안에서 잡고 기존 주문을 돌려주면 트랜잭션이 이미 rollback-only라 커밋 때 `UnexpectedRollbackException`이 터졌다. 그래서 재고 재검증 → 쿠폰·스탬프 소비 → 주문 INSERT를 `OrderCreator.create()` 트랜잭션 하나로 좁히고, `OrderService.createOrder()`는 트랜잭션 **밖**에서 예외를 받아 `(memberId, key)`로 먼저 커밋된 주문을 다시 조회해 돌려준다. 키는 전역이 아니라 회원 단위로 유일하다. 전역 유니크일 때는 다른 회원이 같은 키를 보내면 남의 주문 ID와 금액이 반환됐기 때문이다.

근거: 22b8424, PR #20, Flyway `V2__orders_idempotency_key_per_member.sql`.

## Considered Options

- `createOrder`에 `@Transactional(propagation = NOT_SUPPORTED)`: 동시성 통합 테스트에서 커넥션 풀이 고갈돼 교착됐다. 트랜잭션 동기화가 켜진 채 트랜잭션 없이 조회하면 EntityManager가 커넥션을 메서드 끝까지 쥐고, 내부 트랜잭션이 하나를 더 잡아 요청당 커넥션 2개를 쓴다. 그래서 `OrderService`에는 클래스 레벨 `@Transactional`을 두지 않는다.

## Consequences

- 실패한 쪽은 트랜잭션이 통째로 롤백되므로 쿠폰·스탬프를 소비한 흔적이 남지 않는다.
- `OrderService`에 메서드를 추가할 때 트랜잭션을 메서드마다 명시해야 한다.
