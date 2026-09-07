# 스마트오더 백엔드 진행 상황 (로컬 세션용)

> 최종 갱신: 2026-09-08 (PR #13 생성 완료 — Member 적립/스탬프 도메인, 리뷰/머지 대기)
> 기준 브랜치: `feature/gy/member-reward` (base: `develop`, merge-base에 Coupon PR #6이 이미 병합돼 있음)
> 이 문서는 로컬 Claude Code CLI 세션이 관리합니다. `BACKEND_ROADMAP.md`는 Cowork 세션이 별도로 관리하는
> 문서이니 혼동하지 말 것. 이전 브랜치(`feature/gy/coupon`)의 작업 기록은 PR #6으로 병합 완료돼 이 문서에서는
> 정리했다 — 상세 이력은 `git log`/PR #6 참고.

## 1. 현재 구현 상태 요약

| 도메인 | 상태 | 비고 |
| :--- | :--- | :--- |
| Auth / Member / Store / Category / Menu / Order / Payment / Coupon | ✅ `develop` 기준 구현됨 | 이번 브랜치 시작 시점에 이미 병합돼 있음 |
| Member 적립/스탬프 | ✅ 구현 완료 (이 브랜치) | 결제 승인 시 자동 적립 + 주문 시 소비(할인 반영) |

## 2. 지금까지 한 일 (이 브랜치)

### 2.1 Member 적립/스탬프 도메인 신설 — 구현 완료
- **설계 결정**: 프론트 mock(`rewardsMock.ts`의 `STAMP_REWARD_DISCOUNT = 4500`, `RewardsBadge`/`CouponStampSelector`의
  목표치 10개 표시)과 값을 맞춰, 스탬프 목표치 10개·리워드 할인액 4,500원으로 확정. 적립 시점은 "결제 승인
  시"(주문 생성 시점이 아님 — 결제 전 취소/이탈된 주문은 적립되지 않아야 하므로)로 결정. 별도 도메인
  패키지를 신설하지 않고 기존 `Member` 엔티티/`MemberService`에 필드·메서드를 추가하는 방식을 택함(스탬프가
  회원 1명당 카운터 하나뿐이라 Coupon처럼 별도 테이블/도메인을 둘 이유가 없음).
- `entities/member/Member.kt`: `stampCount: Int = 0` 필드 추가. `addStamp()`(+1), `useStampReward(cost)`(차감)
  메서드 추가 — 목표치 검증(비즈니스 규칙)은 엔티티가 아니라 `MemberService`가 담당(Coupon의
  `isUsed`/`isExpired`와 동일하게, 상태 판정은 엔티티에 두되 예외를 던지는 검증 로직은 서비스에 둠).
- `services/member/MemberService.kt`:
  - `companion object`에 `STAMP_GOAL = 10`, `STAMP_REWARD_DISCOUNT = 4500` 상수 정의(프론트 상수와 값 동기화).
  - `earnStamp(memberId)`: 스탬프 1개 적립. `PaymentService.confirmPayment()`가 호출한다.
  - `redeemStampReward(memberId): Int`: 목표치(10개) 이상 모았는지 검증(미달 시 `ConflictException`
    `STAMP_NOT_ENOUGH`) 후 목표치만큼 차감하고 할인액(4,500원)을 반환. `OrderService.createOrder()`가 호출한다.
  - `getRewards()`의 `stampCount = 0` / `stampGoal = 10` placeholder를 실제 값(`member.stampCount`,
    `STAMP_GOAL`)으로 교체.
- `services/payment/PaymentService.kt`: `confirmPayment()`가 결제를 **새로** 저장하는 경로(멱등 재조회로 기존
  결제를 재사용하는 경로가 아닌)에서만 `memberService.earnStamp(order.memberId)` 호출 — 재시도/경쟁 상태로
  기존 결제 레코드를 그대로 반환하는 경우에는 중복 적립되지 않도록 `save()` 성공 직후, `catch` 분기 밖에서
  호출.
- `services/order/OrderService.kt`: `createOrder()`에서 `req.useStamp`가 true면 쿠폰 소비와 같은 자리에서
  `memberService.redeemStampReward(memberId)`를 호출해 `couponDiscount`와 함께 `totalPrice`에서 차감. 쿠폰의
  기존 동시 요청 경쟁 상태 처리(`ConflictException` 캐치 → `idempotencyKey`로 재조회해 먼저 커밋된 주문을
  멱등 반환)를 스탬프 소비에도 동일하게 확장 적용 — `STAMP_NOT_ENOUGH`도 `ConflictException`으로 던져 같은
  캐치 블록에서 처리되게 함.
- **검증**: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew test` 전체 통과(`BUILD SUCCESSFUL`, 86개
  테스트 전부 성공). 신규 테스트: `MemberServiceTest`(5건 — 적립, 목표치 채운 경우 차감+할인 반환, 목표치
  초과분은 목표치만큼만 차감, 목표치 미달 시 `ConflictException`, 존재하지 않는 회원), `PaymentServiceTest`
  (3건, 신규 파일 — 스탬프 적립 연동분만 커버), `OrderServiceTest`에 스탬프 소비 관련 4건 추가(미사용 시
  미호출, 사용 시 차감, 부족 시 예외 전파, 동시 요청 경쟁 상태 멱등 반환). 기존 테스트 회귀 없음.
- (참고) 이번 검증 과정에서 `/mnt/a`(WSL2 마운트) 위에서 `./gradlew clean test`가 간헐적으로
  `NoSuchFileException(.../in-progress-results-generic.bin)`로 실패하는 것을 관찰함 — 실제 테스트 실패가
  아니라 Gradle 테스트 결과 파일 원자적 rename이 이 마운트에서 가끔 꼬이는 환경 이슈로 보임(재시도하면
  통과). 재현되면 `clean` 없이 `./gradlew test`로 재시도해볼 것.
- **커밋/push/PR 생성 완료**: 커밋(`a93107b` feat, `6b0b462` docs)까지는 이미 이 브랜치 시작 시점에 반영돼
  있었고, 이번에 `gh pr create`로 PR 생성까지 완료함(base `develop` ← head `feature/gy/member-reward`) —
  **[PR #13](https://github.com/codes-gy/smart-order/pull/13)**.

## 3. 다음에 할 일 (우선순위 순, `BACKEND_ROADMAP.md` 기준)

### [x] 0. Coupon 도메인 — 완료 (PR #6, `develop`에 병합됨)
- 상세 이력은 `git log`/PR #6 참고.

### [x] 1. Member 적립/스탬프 도메인 — 완료, 2026-09-07 (이 브랜치)
- 2.1절 참고. PR #13 생성 완료(2026-09-08), 리뷰/머지 대기 중.

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
  `member.stamp_count` 컬럼까지 추가돼 관리 대상 스키마 변경분이 더 늘어남.
- CI에서 `./gradlew build` 자동 검증.

## 4. 리스크 / 확인 필요 항목
- **쿠폰 발급 경로가 웰컴 쿠폰뿐**: 관리자가 프로모션 쿠폰을 임의로 발급하는 기능은 아직 없음. 필요해지면
  별도 우선순위로(관리자 발급 API 신설 또는 `POST /stores/{storeId}/coupons` 등).
- **스탬프 적립 트리거가 결제 승인 1건당 1개 고정**: 주문 금액/수량과 무관하게 결제 1건 = 스탬프 1개로
  단순화했음. PRD상 금액 비례 적립 등 다른 정책이 필요하면 `PaymentService.confirmPayment()`의
  `memberService.earnStamp()` 호출부만 바꾸면 됨(다른 도메인에 영향 없음).
- **DB 마이그레이션 도구 부재**: `coupons` 테이블, `member.stamp_count` 컬럼 포함, prod 스키마 스크립트가
  여전히 없음(3절 6번 참고).
