# ☕ 카페 실시간 선주문 & 픽업 플랫폼

> **바쁜 출근 시간, 기다림 없는 간편한 음료 주문!**  
> **출근 시간대 주문을 안정적으로 처리하고, 매장의 조리/픽업 상태를 실시간 동기화하는 스마트오더 플랫폼입니다.**


---

## 기술 스택

| 구분 | 기술 |
| :--- | :--- |
| **Language & Framework** | Kotlin, Spring Boot 3.x, Spring Data JPA |
| **Database** | PostgreSQL, H2 (Testing) |
| **Cache & Concurrency** | Redis (Redisson) |
| **Event & Messaging** | Apache Kafka, Spring Event |
| **Dev Tools** | Git, Docker, Claude Code |

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

---
