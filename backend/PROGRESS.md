# 스마트오더 백엔드 진행 상황 (로컬 세션용)

> 최종 갱신: 2026-09-08 (메뉴 옵션 관리자 CRUD 구현 완료 — 백엔드만 우선 구현)
> 기준 브랜치: `feature/gy/member-reward` (base: `develop`, merge-base에 Coupon PR #6이 이미 병합돼 있음)
> 이 문서는 로컬 Claude Code CLI 세션이 관리합니다. `BACKEND_ROADMAP.md`는 Cowork 세션이 별도로 관리하는
> 문서이니 혼동하지 말 것. 이전 브랜치(`feature/gy/coupon`)의 작업 기록은 PR #6으로 병합 완료돼 이 문서에서는
> 정리했다 — 상세 이력은 `git log`/PR #6 참고.

## 1. 현재 구현 상태 요약

| 도메인 | 상태 | 비고 |
| :--- | :--- | :--- |
| Auth / Member / Store / Category / Menu / Order / Payment / Coupon | ✅ `develop` 기준 구현됨 | 이번 브랜치 시작 시점에 이미 병합돼 있음 |
| Member 적립/스탬프 | ✅ 구현 완료 (이 브랜치) | 결제 승인 시 자동 적립 + 주문 시 소비(할인 반영) |
| Notification | ➖ 백엔드 구현 불필요 (확인 완료, 2026-09-08) | 프론트가 이미 있는 Order SSE + 브라우저 `Notification` API로 클라이언트 단에서만 처리(서버발 푸시 계약 없음) — 2.2절 참고 |
| Event | ➖ 현재로선 백엔드 구현 불필요 (확인 완료, 2026-09-08) | 프론트 라우트/타입/API/mock 어디에도 대응 화면·계약이 없음(홈/배너 화면 자체가 없음) — 2.3절 참고 |
| Cart | ➖ 백엔드 구현 불필요 (확인 완료, 2026-09-08) | Zustand `persist`로 로컬스토리지에만 보관, 체크아웃 시 Order 검증/생성 API로 바로 변환돼 서버 저장 필요 없음 — 2.4절 참고 |
| 메뉴 옵션 관리자 CRUD | ✅ 백엔드만 구현 완료 (이 브랜치, 2026-09-08) | 프론트 대응 화면/계약 없음(관리자 대시보드는 메뉴 전체 품절 토글만 존재) — 사용자 확인 하에 백엔드 먼저 구현, 프론트 연동은 추후 별도 — 2.5절 참고 |

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

### 2.2 Notification 도메인 — 필요 여부 확인 완료: 백엔드 구현 불필요
- 프론트 계약(`frontend/src/api/`, `frontend/src/types/`)에 `notificationApi`/`notification.types`/
  `notificationMock`이 전혀 없음을 확인.
- 유일한 관련 코드는 `frontend/src/hooks/usePushNotificationPermission.ts` + `OrderTrackingRouter.tsx`:
  브라우저 `Notification` Web API를 그대로 쓰는 클라이언트 전용 데모이며, FCM/APNs 토큰 등록이나 Web Push
  구독 로직이 없음. 이미 존재하는 Order SSE(`GET /orders/{orderId}/events`)의 새 상태 이벤트를 받아 탭이
  백그라운드일 때 `new Notification(...)`으로 로컬 알림만 띄움(코드 주석: "실제 푸시 서버(Web Push) 없이도
  픽업 준비 완료 시 포그라운드 알림을 데모할 수 있도록 한다").
- **결론**: 서버발 푸시 트리거/발송 API, FCM 연동 등 별도 Notification 도메인/엔드포인트 불필요. 이미
  구현된 Order SSE만으로 프론트 요구사항이 충족됨. PRD 문서가 저장소에 없어 교차 검증은 못 했으나, 프론트
  mock/타입이 소스 오브 트루스라는 원칙(`CLAUDE.md`)에 따라 확정.
- 후속 조치 없음(코드 변경 없음). `BACKEND_ROADMAP.md`도 병행 동기화 필요(Cowork 세션 관리 문서라 여기서는
  기록만 남김).

### 2.3 Event 도메인 — 필요 여부 확인 완료: 현재로선 백엔드 구현 불필요
- 프론트 라우트 전체(`frontend/src/routers/`, `frontend/src/app/`)를 확인: 스토어 찾기/상세, 카트, 체크아웃,
  로그인, 마이페이지, 주문내역/추적, 매장관리자 로그인/대시보드뿐 — 이벤트/프로모션/배너 관련 페이지·
  컴포넌트가 전혀 없고, 배너를 얹을 홈/메인 화면 자체가 없음(Store Finder가 사실상 진입점).
- `frontend/src/api/`, `frontend/src/types/`에도 `eventApi`/`event.types`/`eventMock` 등 대응 계약이 없음.
  "이벤트"라는 단어가 걸리는 곳은 전부 JS/DOM 이벤트나 SSE `OrderTrackingEvent`뿐, 마케팅성 Event 도메인과
  무관함을 확인.
- `BACKEND_ROADMAP.md`도 "PRD상 정확한 용도 확인 필요"로만 적혀 있어 로드맵 작성자 본인도 실체를 특정하지
  못한 상태. 저장소에 PRD 원본이 없어 완전한 교차검증은 불가하지만, 프론트가 소스 오브 트루스라는 원칙
  (`CLAUDE.md`)상 지금 구현할 근거 자체가 없음.
- **결론**: 현재 프론트 계약 기준으로는 Event 도메인 신규 구현 불필요. 추후 PRD가 확보되거나 프론트에 관련
  화면/타입이 추가되면 그때 재검토.
- 후속 조치 없음(코드 변경 없음).

### 2.4 Cart 도메인 — 필요 여부 확인 완료: 백엔드 구현 불필요
- `frontend/src/stores/cartStore.ts`: Zustand + `persist` 미들웨어로 로컬스토리지(`CART_STORAGE_KEY`)에만
  보관. 서버 API 호출이 전혀 없음. "한 번에 한 매장 주문만 지원"이라는 PRD 규칙도 클라이언트 로직
  (`addItem`에서 다른 매장 상품이 들어오면 기존 목록을 비움)으로만 처리.
- `frontend/src/api/`에 `cartApi.ts` 자체가 없음 — 있는 건 `cartStore.ts`/`useCart.ts`/`cart.types.ts`(순수
  클라이언트 상태·타입)뿐.
- `useCheckout.ts` 확인: 장바구니 항목을 결제 시점에 바로 `orderApi.validate()` → `orderApi.create()` 요청
  바디로 변환해서 보냄 — 장바구니를 서버에 저장/조회하는 과정 자체가 없고, 이미 구현된 Order 검증/생성
  2단계 API로 완전히 커버됨.
- **결론**: Cart는 임시 UI 상태일 뿐 서버가 관여할 지점이 없음. 신규 구현 불필요.
- 후속 조치 없음(코드 변경 없음).

### 2.5 메뉴 옵션 관리자 CRUD — 백엔드만 구현 완료
- **범위 확인**: 착수 전 프론트 계약을 확인한 결과, `store-admin` 대시보드(`useStoreAdminMenu.ts`,
  `MenuSoldOutRow.tsx`)에는 메뉴 통째 품절 토글만 있고 옵션 그룹/선택지 단위 CRUD 화면·API 계약
  (`menuApi`/`menu.types.ts`)이 없음을 확인. `BACKEND_ROADMAP.md`에도 "(필요해지면)"으로 조건부 표기돼
  있어 Notification/Event/Cart처럼 "보류"로 남길지 사용자에게 확인 → **"백엔드만 먼저 구현"**으로 결정
  (Category/Menu CRUD와 동일 패턴, 프론트 연동은 화면이 생기면 별도 진행).
- `entities/menu/MenuOptionGroup.kt`: `MenuOptionType`에 `fromApiValue(value: String)` companion 메서드
  추가(프론트가 보낼 소문자 "single"/"multiple" 문자열 → enum 변환, 기존 응답측 변환 로직과 대칭).
  `MenuOptionGroup`에 `updateInfo(name, type, required, displayOrder)` 추가.
- `entities/menu/MenuOptionChoice.kt`: `updateInfo(label, priceDelta, displayOrder)`, `updateSoldOut(isSoldOut)`
  추가.
- `repositories/menu/MenuOptionGroupRepository.kt`, `MenuOptionChoiceRepository.kt` 신설(단순
  `JpaRepository<Entity, Long>`, 파생 쿼리 불필요).
- `dtos/menu/MenuDto.kt`: `MenuOptionGroupCreateRequest`/`UpdateRequest`,
  `MenuOptionChoiceCreateRequest`/`UpdateRequest`/`SoldOutUpdateRequest` 추가(기존 `MenuOptionGroupResponse`/
  `MenuOptionChoiceResponse`는 그대로 재사용).
- `services/menu/MenuOptionService.kt` 신설(기존 `MenuService`에 얹지 않고 별도 서비스로 분리 — 옵션
  그룹/선택지 CRUD라는 별개 책임이라 `backend/CLAUDE.md`의 "하나의 클래스는 하나의 책임" 원칙에 맞춤):
  `createOptionGroup`/`updateOptionGroup`/`deleteOptionGroup`,
  `createOptionChoice`/`updateOptionChoice`/`updateOptionChoiceSoldOut`/`deleteOptionChoice`. 존재하지
  않는 메뉴/그룹/선택지는 각각 `MENU_NOT_FOUND`/`MENU_OPTION_GROUP_NOT_FOUND`/`MENU_OPTION_CHOICE_NOT_FOUND`
  `NotFoundException`으로 처리.
- `controllers/menu/MenuOptionController.kt` 신설. 라우트는 Category 컨트롤러 패턴(생성은 부모 리소스
  아래 nested, 수정/삭제/품절처리는 자원 id로 flat)을 그대로 따름:
  - `POST /menus/{menuId}/option-groups`, `PUT /option-groups/{groupId}`, `DELETE /option-groups/{groupId}`
  - `POST /option-groups/{groupId}/choices`, `PUT /option-choices/{choiceId}`,
    `PATCH /option-choices/{choiceId}/sold-out`, `DELETE /option-choices/{choiceId}`
- **검증**: `MenuOptionServiceTest` 신규 작성(6건 — 생성 시 소문자 type→enum 변환, 존재하지 않는 메뉴로
  생성 시 예외, 그룹 수정, 그룹 삭제, 선택지 품절 처리, 존재하지 않는 선택지 수정 시 예외).
  `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew test` 전체 통과(`BUILD SUCCESSFUL`, 92개 테스트
  전부 성공, 기존 테스트 회귀 없음).
- 후속 조치: 프론트에 옵션 관리자 화면/`menuApi` 계약이 추가되면 그때 연동. 아직 커밋/PR 생성 전 —
  다음 세션에서 이어서 진행하거나 바로 커밋해도 됨.

## 3. 다음에 할 일 (우선순위 순, `BACKEND_ROADMAP.md` 기준)

### [x] 0. Coupon 도메인 — 완료 (PR #6, `develop`에 병합됨)
- 상세 이력은 `git log`/PR #6 참고.

### [x] 1. Member 적립/스탬프 도메인 — 완료, 2026-09-07 (이 브랜치)
- 2.1절 참고. PR #13 생성 완료(2026-09-08), 리뷰/머지 대기 중.

### [x] 2. Notification 도메인 — 확인 완료, 2026-09-08: 백엔드 구현 불필요
- 2.2절 참고. 코드 변경 없음, 다음 우선순위(Event)로 진행.

### [x] 3. Event 도메인 — 확인 완료, 2026-09-08: 현재로선 백엔드 구현 불필요
- 2.3절 참고. 코드 변경 없음, 다음 우선순위(Cart)로 진행.

### [x] 4. Cart 도메인 — 확인 완료, 2026-09-08: 백엔드 구현 불필요
- 2.4절 참고. 코드 변경 없음, 다음 우선순위(메뉴 옵션 관리자 CRUD)로 진행.

### [x] 5. 메뉴 옵션 관리자 CRUD — 백엔드만 구현 완료, 2026-09-08 (이 브랜치)
- 2.5절 참고. 커밋/PR은 아직 안 함. 프론트 연동은 화면이 생기면 별도 진행.

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
