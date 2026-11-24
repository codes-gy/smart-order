# ☕ 카페 실시간 선주문 & 픽업 플랫폼

> **바쁜 출근 시간, 기다림 없는 간편한 음료 주문!**  
> **출근 시간대 주문을 안정적으로 처리하고, 매장의 조리/픽업 상태를 실시간 동기화하는 스마트오더 플랫폼입니다.**


---

## 기술 스택

| 구분 | 기술 |
| :--- | :--- |
| **Language & Framework** | Kotlin, Spring Boot 3.x, Spring Data JPA |
| **Database** | PostgreSQL (Flyway 마이그레이션), H2 (로컬 · 테스트) |
| **Cache** | Redis (SMS 인증번호, 로그아웃 토큰 무효화) |
| **Concurrency** | DB 조건부 UPDATE · 복합 유니크 제약 기반 (분산 락 미사용) |
| **Realtime & Event** | SSE (`SseEmitter`), Spring Event (`@TransactionalEventListener`) |
| **Frontend** | Next.js, React, TanStack Query |
| **Load Test** | k6 (`load-test/`) |
| **Dev Tools** | Git, GitHub Actions, Docker, Claude Code |

---

## 주요 기능

### 1. Store (매장 관리 & 위치 기반 검색)
- 매장 정보 등록, 수정, 삭제 및 상세 조회
- **Haversine 공식**을 적용한 사용자 위치 기반 실시간 거리 계산
- 대용량 트래픽에 대비한 **위치 기반 커서 페이징(Cursor Pagination)** 목록 조회
- 매장 영업 상태(`OPEN`, `PAUSED`, `CLOSED`) 및 예상 조리 시간 실시간 관리

### 2. Category (메뉴 카테고리)
- 매장별 메뉴 카테고리(`에스프레소`, `논커피`, `디저트` 등) CRUD
- 복합 인덱스(`store_id`, `display_order`)를 통한 매장별 카테고리 노출 순서 정렬
- 동일 매장 내 중복 카테고리명 검증 로직

### 3. Menu (메뉴 관리)
- 카테고리별 메뉴 등록, 수정, 삭제 및 상세 조회
- 메뉴 상태 관리 (`ON_SALE` 판매중, `SOLD_OUT` 품절, `HIDDEN` 숨김)

### 4. Order (주문 처리 및 이력 관리)
- 주문 생성 및 메뉴 수량별 자동 총액 계산
- **주문 스냅샷(Snapshot)** 구조: 주문 당시의 메뉴명과 가격을 별도 저장하여 향후 메뉴 변경 시에도 과거 주문 이력의 정합성 보장
- 점주용 매장별/상태별 주문 목록 조회
- 주문 상태 라이프사이클 관리
- 주문 상태 실시간 추적(SSE): 구독 즉시 현재 상태 전송, 15초 PING 하트비트, 상태 변경은 **트랜잭션 커밋 후에만** 전송

---

## 동시성 · 정합성 처리

출근 시간대처럼 같은 회원의 요청이 겹치거나(더블 탭 · 네트워크 재시도) 같은 자원을 동시에 쓰려는 상황을 DB 레벨에서 막는다.

| 상황 | 처리 방식 |
| :--- | :--- |
| 같은 쿠폰을 동시에 두 번 사용 | `UPDATE ... WHERE used_at IS NULL` 조건부 UPDATE → 갱신 행 수가 0이면 `409 COUPON_ALREADY_USED` |
| 스탬프 리워드 동시 사용 / 동시 적립 | `WHERE stamp_count >= 10` 조건부 리셋, `stamp_count = stamp_count + 1` 원자적 증가 |
| 같은 멱등성 키로 주문 동시 재전송 | `(member_id, idempotency_key)` 복합 유니크 제약. 늦은 요청의 트랜잭션은 통째로 롤백되고, 트랜잭션 밖에서 먼저 커밋된 주문을 재조회해 같은 `orderId`를 반환 |
| 다른 회원이 우연히 같은 멱등성 키 사용 | 키를 회원 단위로 조회하므로 다른 회원의 주문이 반환되지 않음 |

- 검증: `backend/src/test/kotlin/com/gy/smartorder/order/OrderConcurrencyIntegrationTest.kt` — 실제 DB(H2)에
  16개 스레드를 `CountDownLatch`로 동시에 출발시켜 위 시나리오를 재현한다.
- 처리량: 아침 피크 주문 폭주 k6 시나리오는 [`load-test/`](load-test/README.md)에 있다. **처리량 · 지연 수치는 아직 측정 전(측정 예정)** 이다.
- 한계: SSE 구독자 레지스트리가 인스턴스 메모리에 있어 **단일 인스턴스 전제**다. 다중 인스턴스 확장(Redis Pub/Sub 등)은 후속 과제.

---

## 로컬 실행 · 테스트

```bash
# 백엔드 (JDK 25). 테스트는 H2 인메모리 DB를 쓰므로 Postgres/Redis 없이 돈다.
cd backend
./gradlew test

# 프론트엔드
cd frontend
npm ci && npm run dev
```

---
