# 스마트오더 백엔드 진행 상황 (로컬 세션용)

> 최종 갱신: 2026-09-07 (1.1절 — Coupon 도메인 구현 완료, 코드 리뷰 대기)
> 기준 브랜치: `feature/gy/coupon` (base: `develop`, merge-base에 `feature/gy/auth` PR #5가 이미 병합돼 있음)
> 이 문서는 로컬 Claude Code CLI 세션이 관리합니다. `BACKEND_ROADMAP.md`는 Cowork 세션이 별도로 관리하는
> 문서이니 혼동하지 말 것. 이전 브랜치(`feature/gy/auth`)의 작업 기록은 PR #5로 병합 완료돼 이 문서에서는
> 정리했다 — 상세 이력은 `git log`/PR #5 참고.

## 1. 현재 구현 상태 요약

| 도메인 | 상태 | 비고 |
| :--- | :--- | :--- |
| Auth / Member / Store / Category / Menu / Order / Payment | ✅ `develop` 기준 구현됨 | 이번 브랜치 시작 시점에 이미 병합돼 있음 |
| Coupon | ✅ 구현 완료 (이 브랜치) | 회원가입 시 웰컴 쿠폰 자동 발급 + 주문 시 소비 |

## 2. 지금까지 한 일 (이 브랜치)

### 2.1 Coupon 도메인 신설 — 구현 완료
- **사용자 확인**: 쿠폰 발급 방식을 "회원가입 시 웰컴 쿠폰 자동 발급"으로 결정(프론트 mock의 "웰컴 2,000원
  할인 쿠폰" 컨셉과 일치, 별도 관리자 발급 API는 범위 밖).
- `entities/coupon/Coupon.kt`(신규): 회원에게 발급된 쿠폰 1장. `memberId`, `name`, `discountAmount`,
  `expiresAt`, `usedAt`(null=미사용) 필드. Order/OrderItem과 동일한 스냅샷 컨벤션 — 발급 시점 이름/할인액을
  그대로 들고 있어 이후 쿠폰 정책이 바뀌어도 이미 발급된 쿠폰 값은 유지.
- `repositories/coupon/CouponRepository.kt`(신규): `findByMemberIdAndUsedAtIsNullAndExpiresAtAfterOrderByExpiresAtAsc`
  (사용 가능 쿠폰 목록), `findByIdAndMemberId`(소유권 검증 겸 조회).
- `dtos/coupon/CouponDto.kt`(신규): `CouponResponse`(`{ id, name, discountAmount, expiresAt }`) — 프론트
  `Coupon` 타입과 1:1. 기존 `MemberDto.CouponResponse`(빈 배열 placeholder 시절 정의)는 제거하고 이걸로 통일.
- `services/coupon/CouponService.kt`(신규):
  - `issueWelcomeCoupon(memberId)`: 웰컴 쿠폰(2,000원 할인, 발급일로부터 30일 유효) 발급.
  - `getAvailableCoupons(memberId)`: 미사용 + 미만료 쿠폰만 반환.
  - `redeem(memberId, couponId)`: 본인 소유 검증(404 `COUPON_NOT_FOUND`) → 사용 여부(409
    `COUPON_ALREADY_USED`) → 만료 여부(400 `COUPON_EXPIRED`) 순으로 검증 후 사용 처리, 할인액 반환.
- `AuthService`: 신규 회원이 실제로 생성되는 세 경로(`signup`, `socialLogin`의 최초 provision,
  `verifySms`의 최초 provision)에서 `couponService.issueWelcomeCoupon()` 호출. 기존 회원 재로그인 시에는
  재발급하지 않도록 "새로 생성됐는지" 여부를 로컬 변수로 구분.
- `MemberService.getCoupons()`/`getRewards()`: 빈 배열/0값 placeholder였던 것을 `CouponService` 실제 값으로
  교체. `rewards.availableCouponCount`도 실제 보유 쿠폰 수 반영(단 `stampCount`/`stampGoal`은 Member 적립
  도메인이 아직 없어 여전히 placeholder).
- **Order 도메인에 `memberId` 추가(이번 작업으로 드러난 선행 결함 수정)**: 기존 `Order` 엔티티에 주문한
  회원을 식별하는 필드가 전혀 없었음 — `POST /orders`가 `SecurityConfig`상 이미 `authenticated`를 요구하는데도
  `@AuthenticationPrincipal`을 읽어 쓰지 않고 있던 상태. 쿠폰 소유권 검증(본인 쿠폰만 쓸 수 있어야 함)을
  올바르게 하려면 "누가 주문했는지"가 필요해서 `Order.memberId`(nullable 아님) 추가,
  `OrderController.createOrder()`에 `@AuthenticationPrincipal memberId: Long` 파라미터 추가,
  `OrderService.createOrder()`가 이를 받아 쿠폰 소유권 검증과 `Order.memberId`에 그대로 사용.
- `OrderService.createOrder()`: `req.couponId`가 있으면 `couponService.redeem(memberId, couponId)`로 소비하고
  할인액을 반환받아 `itemsAmount - couponDiscount`(0원 아래로는 내려가지 않음, `coerceAtLeast(0)`)를
  `totalPrice`로 저장. `useStamp` 할인은 Member 적립 도메인이 아직 없어 여전히 반영하지 않음(범위 밖).
- **검증**: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew clean test` 전체 통과(`BUILD SUCCESSFUL`).
  신규 테스트: `CouponServiceTest`(6건), `OrderServiceTest`(6건, 쿠폰 할인 연동 경로만 커버 — OrderService의
  기존 검증/옵션가격 로직은 이번 범위 밖이라 별도 커버 안 함), `AuthServiceTest`에 웰컴 쿠폰 발급/미발급
  케이스 4건 추가. 전부 통과, 기존 테스트 회귀 없음.

### 2.2 `/code-review medium` 발견 2건 수정 — 완료
- **수정 1 — 동일 idempotencyKey 동시 요청 시 쿠폰 이중 소비 경쟁 상태(2.1절 "알려진 한계"였던 것을 실제
  수정)**: 두 요청이 모두 멱등성 캐시 조회를 통과한 뒤 같은 쿠폰을 `redeem()`하면, 늦은 쪽은
  `COUPON_ALREADY_USED` 409를 그대로 사용자에게 돌려주고 있었음 — 원래는 이미 커밋된 동일 주문을 멱등하게
  반환했어야 함. `OrderService.createOrder()`에서 `couponService.redeem()` 호출을
  `try/catch(ConflictException)`로 감싸, 충돌 시 `orderRepository.findByIdempotencyKey()`를 재조회해서
  그 사이 동시 요청이 커밋한 주문이 보이면 그걸 반환하고, 안 보이면(진짜 이미 써버린 쿠폰) 원래 예외를 그대로
  던지도록 수정. 기존 `DataIntegrityViolationException` 캐치-재조회 패턴과 동일한 방식.
- **수정 2 — `AuthService`의 웰컴 쿠폰 발급 조건문 중복**: `socialLogin()`/`verifySms()` 둘 다
  `if (existingMember == null) { couponService.issueWelcomeCoupon(memberId) }`를 그대로 복붙해뒀던 것을
  `issueWelcomeCouponForNewMember(existingMember, memberId)` private 헬퍼로 추출.
- **검증**: `OrderServiceTest`에 회귀 테스트 2건(동시 요청 시 기존 주문 반환 / 진짜 중복 사용 시 예외 전파)
  추가, `./gradlew clean test` 전체 통과(`BUILD SUCCESSFUL`).
- **다음 단계**: 커밋/push/PR 생성만 남음(사용자 확인 후 진행).

## 3. 다음에 할 일 (우선순위 순, `BACKEND_ROADMAP.md` 기준)

### [x] 0. Coupon 도메인 — 완료, 2026-09-07 (이 브랜치)
- 2.1절 참고.

### [ ] 1. Member 적립/스탬프 도메인
- 주문 시 `useStamp` 할인 계산에 필요(현재 `OrderService`가 `useStamp` 값을 저장만 하고 반영 안 함).
- `MemberDto.RewardsSummaryResponse.stampCount/stampGoal`도 이 도메인 구현 후 실제 값으로 교체 필요.

### [ ] 2. Notification 도메인
- 결제/주문 상태 변경 시 서버발 푸시 트리거 필요 여부부터 재확인.

### [ ] 3. Event 도메인
- PRD상 정확한 용도 확인 필요. 우선순위 가장 낮음.

### [ ] 4. Cart 도메인 필요 여부 재확인
- 프론트가 Zustand로 클라이언트에만 보관 중 — 백엔드 API 자체가 불필요할 가능성.

### [ ] 5. 메뉴 옵션 관리자 CRUD
- 매장 관리자가 옵션 그룹/선택지를 등록·수정·삭제·품절처리 하는 기능(조회/주문 반영은 이미 완료).

### [ ] 6. 인프라/운영
- 마이그레이션 도구(Flyway/Liquibase) 도입 — prod 프로필(`ddl-auto: validate`)용 스키마 스크립트 없음.
  이번에 `coupons` 테이블도 추가돼 관리 대상 테이블이 더 늘어남.
- CI에서 `./gradlew build` 자동 검증.

## 4. 리스크 / 확인 필요 항목
- **쿠폰 발급 경로가 웰컴 쿠폰뿐**: 관리자가 프로모션 쿠폰을 임의로 발급하는 기능은 아직 없음. 필요해지면
  별도 우선순위로(관리자 발급 API 신설 또는 `POST /stores/{storeId}/coupons` 등).
- **(해결됨, 2.2절)** ~~Order 동시 요청 경쟁 상태로 쿠폰 이중 소비 방지가 불완전~~ — `ConflictException` 캐치 후
  재조회하는 방식으로 수정 완료.
- **DB 마이그레이션 도구 부재**: `coupons` 테이블 포함, prod 스키마 스크립트가 여전히 없음(3절 6번 참고).
