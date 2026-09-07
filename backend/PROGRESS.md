# 스마트오더 백엔드 진행 상황 (로컬 세션용)

> 최종 갱신: 2026-09-07 (2.16절 — 커밋 8개로 분리 완료 + `origin/feature/gy/auth`로 push 완료, PR 생성만 남음)
> 기준 브랜치: `feature/gy/auth`
> 이 문서는 로컬 Claude Code CLI 세션이 관리합니다. `BACKEND_ROADMAP.md`는 Cowork 세션이 별도로 관리하는
> 문서라 이 branch(`feature/gy/auth`)에서 진행된 최신 작업이 아직 반영돼 있지 않습니다(거기엔 "Auth ❌ 미구현"으로
> 적혀 있으나 실제로는 아래처럼 이미 상당 부분 구현됨). 두 문서가 서로 다른 이유이니 혼동하지 말 것.

## 1. 현재 구현 상태 요약

| 도메인 | 상태 | 비고 |
| :--- | :--- | :--- |
| Store / Category / Menu / Order / Payment | 🔶 `BACKEND_ROADMAP.md` 기준대로 (별도 변경 없음) | 이번 브랜치는 건드리지 않음 |
| Auth (이메일/비밀번호) | 🔶 구현됨, 단 **프론트 계약과 불일치** | 아래 2절 참고 — 프론트는 이 플로우를 쓰지 않음 |
| Auth (소셜 로그인) | ✅ 구현됨 (토큰 서버 검증 포함) | `AuthService.socialLogin()` + `POST /auth/social-login`. 카카오/애플 토큰을 각 제공자 서버에 실제 검증(2.12절) — 5절 리스크 항목 해소 |
| Auth (SMS 인증) | ✅ 구현됨 | `AuthService.sendSms()`/`verifySms()` + `POST /auth/sms/send`, `/auth/sms/verify`. 실제 발송 벤더 연동은 아직 TODO (5절 리스크 참고) |
| Member | ✅ 구현됨 | `GET/PATCH/DELETE /members/me` — 리워드는 아직 0값 placeholder (5절 항목과 연결) |
| Security (JWT 인프라) | ✅ 구현됨 | 필터체인, 토큰 발급/검증 인프라는 정상 동작. role을 `Member.Role`이 아닌 원시 문자열로 다루도록 일반화(2.7절) |
| Auth (매장 관리자 로그인) | ✅ 구현됨 | `AuthService.storeLogin()` + `POST /auth/store-login`. 계정은 `POST /stores`에서 함께 프로비저닝(2.8절 참고) |
| 매장 관리자 인가 (`hasRole("STORE_ADMIN")`) | ✅ 구현됨 | `PATCH /stores/**` 3종 + 소유 매장 검증. 2.8절 참고 |
| Rewards / Favorites / Coupon 응답 | 🔶 placeholder | `GET /members/rewards`(0값)·`/members/favorites`·`/members/coupons`(빈 배열). 2.9절 참고 — 실제 도메인 미구현 |

## 2. 지금까지 한 일 (이 브랜치, 커밋 전 작업 트리 기준)

### 2.1 JWT 인증 인프라 — 구현 완료
- `config/passport/JwtTokenProvider.kt`: access(1h)/refresh(14d) 토큰 발급·검증, `jwt.secret`은 `.env`의 `JWT_SECRET` 참조(하드코딩 없음, 컨벤션 준수).
- `config/passport/JwtAuthenticationFilter.kt`: `Authorization: Bearer` 헤더 파싱 → `SecurityContext`에 `memberId`/`ROLE_*` 주입. 토큰 없거나 무효하면 그냥 통과시키고 이후 `authorizeHttpRequests`가 401 처리.
- `config/passport/JwtAuthenticationEntryPoint.kt`: 인증 실패 시 `ErrorResponse` 규격으로 401 응답(신규 파일, 기존 `common.exception` 관례 재사용).
- `config/SecurityConfig.kt` → `config/security/SecurityConfig.kt`로 이동(디렉터리 컨벤션 반영). `STATELESS` 세션, `/auth/**`·swagger·h2-console·actuator만 `permitAll`, `/admin/**`은 `hasRole("ADMIN")`, 나머지는 `authenticated`. CORS는 `allowedOriginPatterns = ["*"]`로 전체 허용(운영 전 도메인 명시 필요, 코드에 주석으로 명시돼 있음).
- **TODO 주석 남아있음**: 매장 관리자 전용 엔드포인트(매장 정보 변경 등)에 대한 `hasRole("STORE_ADMIN")` 인가가 아직 안 걸림 — 지금은 로그인 여부만 검사.

### 2.2 Member 엔티티 — 구현 완료 (이동 + 확장)
- `entities/auth/Member.kt` → `entities/member/Member.kt`로 이동.
- 이메일/비밀번호, 소셜 로그인(`SocialProvider`: KAKAO/APPLE/NAVER/GOOGLE + `socialId`), SMS(휴대폰 번호) 세 가지 인증 수단을 한 엔티티에 모두 수용하는 구조로 확장 — 각 필드는 해당 로그인 수단에서만 값이 채워지고 나머지는 null.
- `role`(USER/ADMIN), `status`(ACTIVE/INACTIVE/BLOCKED) 필드로 향후 매장 관리자 인가·탈퇴/정지 처리 대비.
- `(social_provider, social_id)` 유니크 제약, email/phoneNumber 인덱스 추가.

### 2.3 이메일/비밀번호 Auth 흐름 — 구현 완료 (단, 2.4의 계약 문제 있음)
- `POST /auth/signup`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`(스텁, 204만 반환) 구현.
- `AuthService`: BCrypt 비밀번호 해싱, 이메일 중복 시 409(`EMAIL_ALREADY_EXISTS`), 로그인 실패 시 401(`INVALID_CREDENTIALS`), 리프레시 토큰 검증 시 타입(`refresh`)·회원 상태(`ACTIVE`) 확인.
- 소셜/SMS 전용 회원(`password == null`)은 이메일 로그인 시도 시 자연스럽게 401 처리되도록 방어 로직 있음.
- `AuthDto`에 `SocialLoginRequest` DTO는 이미 정의돼 있으나 **`AuthService`/`AuthController`에 소셜 로그인 처리 로직은 아직 없음** (DTO만 있고 미구현).

### 2.4 소셜 로그인 — 구현 완료
- `AuthService.socialLogin()`: `(socialProvider, socialId)`로 `MemberRepository` 조회, 없으면 `Member`를 자동 생성(provision)한 뒤 토큰 발급. `POST /auth/social-login` 연결 완료.
- `AuthDto.SocialLoginResponse`가 프론트 `authApi.socialLogin()`의 `Promise<{ user: AuthUser; tokens: AuthTokens }>`와 1:1 매칭되도록 `AuthUserResponse.from(member, isGuest)` 매퍼 추가.
- **(해결됨, 2.12절)** ~~카카오/애플 서버에 `accessToken`을 실제로 검증하는 절차가 없음~~ — `services/auth/oauth`의
  `KakaoTokenVerifier`/`AppleTokenVerifier`로 실제 검증 후 그 결과를 socialId로 사용하도록 교체.

### 2.6 Member 도메인 채우기 — 구현 완료
- `entities/member/Member.kt`: `isGuest` computed property 추가 (`email == null && socialProvider == null` —
  email/social이 둘 다 없다는 것은 SMS 인증만으로 provision된 게스트 계정이라는 뜻; `AuthService.verifySms()`가
  하드코딩해서 내려주던 `isGuest = true`와 동일한 판정 로직을 엔티티 레벨로 옮겨 재사용 가능하게 함).
- `dtos/member/MemberDto.kt`: `MeResponse`(`{ user, rewards }`, frontend `MeResponse` 1:1), `RewardsSummaryResponse`,
  `UpdateProfileRequest`(닉네임/전화번호, 기존 `Member.updateProfile()` 시그니처에 맞춤) 추가.
- `services/member/MemberService.kt`: `getMe`/`updateProfile`/`deleteAccount` 구현. 셋 다 memberId로 회원 조회 후
  못 찾으면 `NotFoundException("MEMBER_NOT_FOUND")`. `deleteAccount`는 실제 삭제 대신
  `MemberStatus.INACTIVE`로 soft-delete(4절 3번 항목에서 논의된 정책 결정 — 되돌릴 수 있는 쪽 선택).
- `controllers/member/MemberController.kt`: `GET/PATCH/DELETE /members/me` 3개 엔드포인트, `@AuthenticationPrincipal`로
  `JwtAuthenticationFilter`가 심어둔 `memberId`(`Long`)를 그대로 주입받음(별도 `UserDetails` 불필요 — principal
  자체가 `Long`이라 캐스팅만으로 동작).
- **경로를 `/auth/me`가 아닌 `/members/me`로 결정한 이유**: `SecurityConfig`가 `/auth/**` 전체를 `permitAll`로
  열어두고 있어서(2.1절), 인증이 필요한 이 엔드포인트를 그 아래 두면 인증 우회가 가능해짐. `/members/**`는
  `anyRequest → authenticated` 규칙에 자연히 걸리므로 이쪽으로 결정. PROGRESS 4절 3번 항목의 "`/auth/me` (or
  `/members/me`)" 중 후자를 채택한 것.
- **`rewards`는 0값 placeholder**: `stampCount: 0, stampGoal: 10, availableCouponCount: 0`. 적립/쿠폰 도메인이
  아직 없어서(로드맵 미구현) 실제 값을 채울 수 없음 — 4절 5번 항목(Rewards/Favorites/Coupon)에서 해당 도메인과
  함께 교체 필요.
- **검증**: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew compileKotlin compileTestKotlin` 통과.
  `./gradlew test`는 30건 중 11건 실패했으나 전부 이번 작업과 무관 — 10건은 기존에 알려진
  `StoreControllerTest` 이슈(2.5절 "미해결 발견"과 동일), 나머지 1건(`SmartOrderApplicationTests.contextLoads`)은
  이번에 처음 관측됨: 로컬에 Postgres가 없어 `local` 프로필의 `datasource` 빈 생성이 실패
  (`Cannot load driver class: ${DB_DRIVER_CLASS_NAME}` — `.env`의 값이 Gradle `test` 태스크 실행 시점에
  해석되지 않는 것으로 보임, `spring-dotenv`가 테스트 JVM에서 `.env`를 못 찾는 문제로 추정). Member 관련 신규
  단위/통합 테스트는 아직 없어(6절 참고) 직접적인 회귀 검증 대상은 없음.

### 2.7 매장(Store) 관리자 로그인 — 구현 완료
- 사용자 확인: Member 엔티티와 완전히 분리된 별도 `StoreAccount` 엔티티 신설로 결정(PROGRESS 4절 4번 항목의
  3가지 선택지 중 세 번째 옵션 채택 — Store 1:1이지만 `@OneToOne` 관계가 아닌 단순 `storeId: Long` +
  unique 제약으로만 연결, 나중에 매장당 여러 계정(직원별)으로 확장 시 unique 제약만 풀면 되도록 설계).
- `entities/store/StoreAccount.kt`(신규): `id`, `storeId`(unique FK 컬럼), `storeCode`(unique), `password`(BCrypt
  해시). `repositories/store/StoreAccountRepository.kt`: `findByStoreCode()`.
- **JWT 인프라를 `Member.Role`에서 분리**(`JwtTokenProvider`/`JwtAuthenticationFilter`): `generateAccessToken(id,
  role)`의 `role` 파라미터를 `Member.Role` enum → 원시 `String`으로 변경, `getRole()`도 `String?` 반환으로 변경.
  `getMemberId()` → `getSubjectId()`로 이름 변경(더 이상 memberId 전용이 아님 — Member 토큰은 memberId, 매장
  토큰은 storeId를 subject로 담음, 어떤 principal인지는 role 클레임으로 구분). `AuthService`의 기존 5개 토큰
  발급 호출부는 `member.role.name`으로, `getMemberId(token)` 호출부는 `getSubjectId(token)`으로 갱신.
  `JwtAuthenticationFilter`도 `Role` import 제거하고 문자열 role로 `SimpleGrantedAuthority` 생성하도록 수정.
- `AuthDto.StoreLoginRequest`(`storeCode`, `password`)/`StoreSessionResponse`(`storeId`, `storeName`,
  `accessToken`) 추가 — frontend `StoreLoginRequest`/`StoreSession`과 1:1(`StoreSession`엔 `refreshToken`이
  없음 — 매장 세션은 재로그인 방식, mock 계약 그대로 반영).
- `AuthService.storeLogin()`: `storeCode`로 `StoreAccount` 조회 → 없으면 401 `INVALID_CREDENTIALS` → 비밀번호
  불일치도 동일 401 → `storeRepository.findById(storeAccount.storeId)`로 `storeName` 조회 → role
  `"STORE_ADMIN"`로 액세스 토큰만 발급(리프레시 토큰 없음, 프론트 계약과 일치). `POST /auth/store-login`
  (`AuthController`, `/auth/**`이 이미 `permitAll`이라 그대로 노출).
- **미해결로 남긴 것(범위 밖)**: (1) `StoreAccount`를 생성하는 API/시딩 절차가 아직 없음 — 지금은 로그인만
  구현했고 계정은 DB에 수동으로 넣어야 함. (2) `SecurityConfig`의 기존 TODO(매장 정보 변경 PATCH 엔드포인트에
  `hasRole("STORE_ADMIN")` 인가 걸기)는 이번 작업에서 건드리지 않음 — 지금 매장 로그인 토큰을 발급할 수는
  있지만 그 토큰이 실제로 뭔가를 잠그지는 않는 상태(기존과 동일하게 `anyRequest → authenticated`만 적용).
  다음 우선순위 후보로 남겨둠(4절 참고로 이동).
- **검증**: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew compileKotlin compileTestKotlin` 통과.
  `./gradlew test`는 이전과 동일하게 30건 중 11건 실패(10건 `StoreControllerTest` + 1건 `contextLoads`,
  2.5/2.6절에 이미 기록된 기존 이슈와 완전히 동일 — 이번 작업으로 인한 신규 회귀 없음). `StoreAccount`/
  `storeLogin` 관련 신규 테스트는 아직 없음(6절 참고).

### 2.8 매장 관리자 인가 마무리 — 구현 완료
- **사용자 확인**: StoreAccount 프로비저닝 방식은 "`POST /stores` 확장(매장 생성과 같은 트랜잭션에서 계정도 함께
  발급)"으로 결정 — 별도 프로비저닝 API 신설안 대신 채택(4절 5번 항목, PROGRESS 사용자 확인 완료).
- `StoreDto.StoreCreateRequest`에 `storeCode`/`storeAccountPassword` 필드 추가(`@NotBlank`). 프론트에
  대응하는 관리자용 매장 생성 화면/mock이 아직 없어 별도 계약 동기화 대상은 없음.
- `StoreService.createStore()`: 사업자번호 중복 체크와 동일한 패턴으로 `storeCode` 중복 시 409
  `STORE_CODE_ALREADY_EXISTS` — `Store` 저장 후 같은 `@Transactional` 안에서 `StoreAccount`(BCrypt 해시된
  비밀번호)도 저장.
- `SecurityConfig`: `authorize(HttpMethod.PATCH, "/stores/**", hasRole("STORE_ADMIN"))` 추가(기존
  TODO 주석 제거). `POST /stores`(생성)·`GET /stores`(조회)는 범위 밖으로 두고 그대로 `anyRequest → authenticated`.
- **소유권 검증 추가(hasRole만으로는 못 막는 구멍)**: `hasRole("STORE_ADMIN")`은 "매장 관리자냐 아니냐"만
  구분하고 "어느 매장의 관리자냐"는 구분하지 못해서, 다른 매장 계정으로 로그인해도 아무 `storeId`나 PATCH할
  수 있는 문제가 남아있었음. `StoreService.requireOwnStore()`를 추가해 JWT subject(로그인 시 발급된
  `storeAccount.storeId`, `JwtAuthenticationFilter`가 `@AuthenticationPrincipal`로 주입)와 경로의
  `storeId`가 일치하는지 확인, 불일치 시 403 `STORE_ACCESS_DENIED`(신규 `ForbiddenException` 타입 추가,
  `common/exception/ApiException.kt`). `updateStore`/`updateStoreStatus`/`updatePreparationTime` 3곳 모두
  적용, `StoreController`에서 `@AuthenticationPrincipal authenticatedStoreId: Long`로 받아 전달.
- **검증**: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew compileKotlin compileTestKotlin` 통과.
  `./gradlew test`는 32건 중 11건 실패(기존 30건 중 11건과 동일한 이슈 — 10건 `StoreControllerTest`
  `@WebMvcTest` 컨텍스트 로딩 실패 + 1건 `contextLoads`, 이번 작업으로 인한 신규 회귀 없음). `StoreServiceTest`에
  신규 테스트 3건(매장 코드 중복 409, 소유권 불일치 403, 매장 생성 시 계정 함께 생성 검증) 추가, 전부 통과.
  `StoreControllerTest`는 시그니처 변경(`updateStoreStatus` 파라미터 추가)에 맞춰 컴파일만 맞춰뒀고, 원래
  실패하던 컨텍스트 로딩 이슈는 이번 작업 범위 밖이라 그대로 둠(5절 리스크 항목, 아래로 이월).
- **여전히 남은 것**: `StoreAccount` 비밀번호를 변경/재발급하는 API는 없음(최초 생성 시점 값 고정) — 필요해지면
  별도 우선순위로.

### 2.9 Rewards / Favorites / Coupon placeholder 응답 — 구현 완료
- 프론트 `authApi.rewards()`/`favorites()`/`coupons()`가 호출하는 세 응답을 채움. Coupon/적립/즐겨찾기 도메인
  자체가 아직 없어서(`BACKEND_ROADMAP.md` 기준 미구현) 4절 6번 항목에 적힌 권장 방식대로 "임시로 빈 배열/0 값
  응답부터 시작"을 채택 — 실제 도메인이 생기면 `MemberService`의 세 메서드 내부만 교체하면 되도록 시그니처를
  먼저 맞춰둠.
- **경로를 `/auth/**`가 아닌 `/members/**`로 결정한 이유**: 2.6절과 동일한 이유 — `/auth/**`는 전체
  `permitAll`이라 인증이 필요한 엔드포인트를 그 아래 두면 인증 우회가 됨. `GET /members/rewards`,
  `GET /members/favorites`, `GET /members/coupons` 3개 신규 엔드포인트 추가.
- `MemberDto.FavoriteStoreResponse`(`storeId`, `storeName`, `lastOrderedAt`)/`CouponResponse`(`id`, `name`,
  `discountAmount`, `expiresAt`) 추가 — 프론트 `FavoriteStore`/`Coupon` 타입과 1:1.
- `MemberService`: 기존 `getMe()`의 rewards placeholder 로직을 `getRewards(memberId)`로 분리해 재사용.
  `getFavorites()`/`getCoupons()`는 멤버 존재 여부만 확인(`getMember()`, 404 `MEMBER_NOT_FOUND` 유지)하고
  항상 빈 리스트 반환.
- Member 도메인에 기존에도 단위/통합 테스트가 없었던 관례(2.6절)를 따라 이번 placeholder 엔드포인트에도
  별도 테스트는 추가하지 않음.
- **검증**: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew compileKotlin compileTestKotlin` 통과.
  `./gradlew test`는 기존과 동일하게 32건 중 11건 실패(10건 `StoreControllerTest` + 1건 `contextLoads`,
  이번 작업으로 인한 신규 회귀 없음).

### 2.10 마무리(4절 7번) — 완료
- **로그아웃 리프레시 토큰 무효화**: 프론트 `authApi.logout()`이 인자를 받지 않아(재로그인 방식 재확인) 특정
  리프레시 토큰 문자열을 지목해 블랙리스트에 올릴 수 없음 — 대신 `memberId` 기준으로 "이 시점 이전에 발급된
  리프레시 토큰은 모두 무효" 마커를 Redis에 남기는 방식 채택. `AuthService.logout(memberId)`가
  `auth:logout:{memberId}` 키에 로그아웃 시각(epoch millis)을 리프레시 토큰 TTL(14일)과 동일한 TTL로 저장하고,
  `refresh()`에서 토큰의 `issuedAt`이 그 시각 이전이면 401 `INVALID_REFRESH_TOKEN`으로 거부.
  `JwtTokenProvider`에 `getIssuedAt()`/`refreshTokenTtlMillis()` 추가. `AuthController.logout()`은
  `@AuthenticationPrincipal memberId: Long?`로 받음(`/auth/**`가 permitAll이라 토큰 없이도 호출 가능하므로
  nullable — 토큰이 없으면 무효화할 대상이 없어 아무 것도 안 함).
- **AuthServiceTest 신규(13건, 전부 통과)**: Spring 컨텍스트 없이 Mockito로만 검증하는 순수 단위 테스트
  (`JwtTokenProvider`는 `@Value` 설정 의존이라 인스턴스화 대신 mock 사용). signup 해피패스/이메일 중복,
  login 해피패스/비밀번호 불일치/소셜·SMS 전용 계정 로그인 시도/존재하지 않는 이메일, refresh
  해피패스/무효 토큰/access 토큰 오용/로그아웃 이후 발급된 토큰 거부/탈퇴 회원 거부, logout 정상 동작/principal
  없을 때 무동작까지 커버.
- **AuthControllerTest 신규(9건, 전부 통과)**: `@WebMvcTest(AuthController::class)` +
  `@Import(SecurityConfig::class, JwtAuthenticationEntryPoint::class)` + `@MockitoBean AuthService`/
  `JwtTokenProvider`. signup/login/refresh 해피패스 + 실패 시 상태코드·`ErrorResponse.code` 검증,
  유효성 검증 실패 400, logout이 Authorization 헤더 유무에 따라 memberId를 올바르게 principal로 넘기는지 검증.
  - **StoreControllerTest와 동일한 함정을 만남**: `@WebMvcTest` 슬라이스는 `Filter` 타입만 자동 스캔 대상이라
    `JwtAuthenticationFilter`는 잡히지만 그 생성자 의존성인 `JwtTokenProvider`(평범한 `@Component`)는 안
    잡혀서 `NoSuchBeanDefinitionException`(→ `@MockitoBean`으로 해결). `JwtAuthenticationEntryPoint`도
    `AuthenticationEntryPoint` 구현체일 뿐 슬라이스 화이트리스트 타입이 아니라 별도로 안 잡혀서 여긴
    `@MockitoBean` 대신 `@Import`로 실제 빈을 등록해 해결(의존성이 없는 단순 컴포넌트라 mock할 필요 없음).
- **BACKEND_ROADMAP.md(Cowork 세션 문서) 동기화**: 1절 Auth 행과 3절 "인증(Auth) 도메인" 항목을 실제 구현
  상태로 갱신(체크 완료로 변경, 이 브랜치 미병합 상태임을 명시). 문서 상단 경고대로 로컬 세션과 Cowork 세션이
  동시에 이 파일을 건드릴 수 있는 위험이 있으므로, 이후 이 문서를 다시 열었을 때 이번 수정과 다른 내용이 보이면
  Cowork 세션이 그 사이 다시 갱신한 것일 수 있음 — 덮어쓰지 말고 병합 검토할 것.
- **검증**: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew compileKotlin compileTestKotlin` 통과.
  `./gradlew test` 전체 54건 중 11건 실패(기존과 동일한 10건 `StoreControllerTest` + 1건 `contextLoads`,
  신규 회귀 없음 — `AuthServiceTest` 13건 + `AuthControllerTest` 9건 전부 통과).
- **범위 밖으로 남긴 것**: `StoreControllerTest` 컨텍스트 로딩 실패는 여전히 미해결(5절 리스크, 여러 세션째
  이월 중). 소셜 토큰 서버 검증, SMS 실발송 벤더 연동도 그대로 TODO.

### 2.11 `StoreControllerTest` 컨텍스트 로딩 실패 수정 — 완료
- **원인**: `@WebMvcTest(StoreController::class)` 슬라이스가 자동 스캔하는 `JwtAuthenticationFilter`(`@Component`)의
  생성자 의존성 `JwtTokenProvider`가 슬라이스에 없어 `NoSuchBeanDefinitionException` → 컨텍스트 로딩 자체가
  실패. `JwtAuthenticationEntryPoint`도 화이트리스트 타입이 아니라 `@Import` 누락 시 동일하게 실패. 정확히
  `AuthControllerTest`(2.10절)에서 이미 겪고 해결했던 것과 같은 함정.
- **수정**: `StoreControllerTest`에 `@Import(SecurityConfig::class, JwtAuthenticationEntryPoint::class)` +
  `@MockitoBean JwtTokenProvider` 추가(`AuthControllerTest`와 동일 패턴).
- **부수 발견 — 실제 버그**: 컨텍스트가 로딩되자 이번엔 `GET /stores`, `GET /stores/{id}`가 401을 반환하는 게
  드러남. `SecurityConfig`가 `/stores/**`에 대해 `PATCH`만 명시적으로 다루고 나머지는 `anyRequest → authenticated`에
  걸려서, 로그인 없이 매장을 둘러봐야 하는 고객용 조회 엔드포인트(PRD B)까지 인증을 요구하고 있었음 — 지금까지
  테스트가 완전히 깨져 있어서 아무도 눈치채지 못한 것으로 보임. `SecurityConfig`에
  `authorize(HttpMethod.GET, "/stores/**", permitAll)` 추가로 수정(매장 생성 `POST`는 기존 결정대로 범위 밖,
  `anyRequest → authenticated` 유지).
- PATCH 4개 테스트는 `SecurityMockMvcRequestPostProcessors.authentication(...)`으로 `JwtAuthenticationFilter`가
  실제 토큰 검증 성공 시 심는 것과 동일한 모양(Long subject + `ROLE_STORE_ADMIN`)의 인증 정보를 주입하도록 수정
  (`storeAdminAuth(storeId)` 헬퍼) — `JwtTokenProvider`가 mock이라 실제 필터가 토큰을 검증해줄 수 없어서 필요.
- **검증**: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew test` 전체 54건 중 1건만 실패(기존에 이미
  알려진, 이번 작업과 무관한 `SmartOrderApplicationTests.contextLoads` — 로컬 Postgres 없음/`.env` 플레이스홀더
  미해석 이슈, 5절 리스크 항목 참고). `StoreControllerTest` 10건 전부 통과. 새 회귀 없음.
- **범위 밖으로 남긴 것**: `SmartOrderApplicationTests.contextLoads` 실패는 그대로 미해결(로컬 인프라 이슈, 별도
  우선순위).

### 2.12 소셜 토큰 서버 검증 (카카오/애플) — 완료
- 신규 패키지 `services/auth/oauth`: `SocialTokenVerifier` 인터페이스(`provider`, `verify(token): socialId`)를
  `KakaoTokenVerifier`/`AppleTokenVerifier`가 구현. `AuthService`는 `List<SocialTokenVerifier>`를 provider로
  매핑해 주입받아 위임(생성자 파라미터 추가).
- **카카오**: `GET https://kapi.kakao.com/v1/user/access_token_info`에 `Authorization: Bearer {accessToken}`으로
  조회, 응답 `id`(카카오 회원번호)를 socialId로 사용. 4xx/네트워크 오류는 401 `INVALID_SOCIAL_TOKEN`으로 변환.
- **애플**: `identityToken`(JWT)을 공개 JWKS(`https://appleid.apple.com/auth/keys`)로 서명 검증 — `jjwt` 0.12.x의
  `Jwks.setParser()`/`JwtParserBuilder.keyLocator(Locator<Key>)` API로 토큰 헤더의 `kid`에 맞는 RSA 공개키를
  찾아 서명 검증, `iss`(`https://appleid.apple.com`)/`aud`(신규 설정 `oauth.apple.client-id`, env
  `APPLE_CLIENT_ID`)까지 확인 후 `sub`를 socialId로 사용. JWKS는 인스턴스 메모리에 1시간 캐시(캐시에 없는 kid면
  애플이 키를 순환한 것으로 보고 강제 재조회 1회 후 재시도).
- `AuthService.socialLogin()`의 기존 "클라이언트가 보낸 토큰을 그대로 socialId로 신뢰" 임시 구현 제거(위조
  가능했던 부분, 5절 리스크 항목 해소). 지원하지 않는 provider(NAVER/GOOGLE — 프론트 계약 밖, verifier 미구현)로
  요청하면 400 `UNSUPPORTED_SOCIAL_PROVIDER`.
- `application.yaml`에 `oauth.apple.client-id: ${APPLE_CLIENT_ID:}` 추가, `.env`에 `APPLE_CLIENT_ID=`(빈 값 —
  애플 Services ID 계정이 아직 없어 실값 미설정, 아래 "범위 밖" 참고).
- `AuthServiceTest`에 소셜 로그인 검증 성공/검증 실패 전파/미지원 provider 3건 추가.
- **검증**: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew compileKotlin compileTestKotlin` 통과.
  `./gradlew test` 57건 중 1건만 실패(기존 `SmartOrderApplicationTests.contextLoads`, 로컬 Postgres 없음 —
  이번 작업과 무관, 5절 리스크 항목으로 여전히 미해결). 나머지 56건(신규 3건 포함) 전부 통과.
- **범위 밖으로 남긴 것**: 카카오 앱/애플 Services ID 실제 발급 및 `APPLE_CLIENT_ID` 실값 설정은 각 사 콘솔에서
  계정을 만들어야 해서 미해결 — 코드 경로는 준비됐으나 로컬에서 실제 토큰으로 엔드투엔드 검증은 못했음(다음
  세션에서 실계정 발급 후 재확인 권장). Naver/Google 소셜 로그인은 프론트가 쓰지 않아 verifier 없음.

### 2.13 `SmartOrderApplicationTests` 로컬 인프라 이슈 수정 — 완료
- **원인 1**: 로컬 `.env`의 `DB_DRIVER_CLASS_NAME` 등 플레이스홀더가 Gradle `test` 태스크 JVM에서 해석되지
  않아(`spring-dotenv`가 테스트 실행 시점엔 적용되지 않는 것으로 추정, 원인 미확정) `Cannot load driver class:
  ${DB_DRIVER_CLASS_NAME}`로 컨텍스트 로딩 자체가 실패.
- **수정 1**: `src/test/resources/application-test.yaml` 신규 — `test` 프로필 전용으로 H2 인메모리 DB
  (`jdbc:h2:mem:smartorder-test;MODE=PostgreSQL`), `ddl-auto: create-drop`, 그리고 `.env` 플레이스홀더에
  기대던 `spring.data.redis.*`/`spring.kafka.bootstrap-servers`/`jwt.secret`/`oauth.apple.client-id`까지
  전부 테스트 전용 고정값으로 직접 명시(프로필별 설정 파일이 공통 설정보다 우선 적용되는 Spring Boot 규칙
  이용). `SmartOrderApplicationTests`에 `@ActiveProfiles("test")` 추가. 다른 테스트(`AuthServiceTest` 등
  순수 Mockito 단위 테스트, `@WebMvcTest` 슬라이스)는 `@ActiveProfiles`를 쓰지 않으므로 이 신규 프로필의
  영향을 받지 않음 — 풀 컨텍스트가 필요한 이 테스트에만 국한된 수정.
- **원인 2(수정 1 적용 후 새로 드러난 별개 버그)**: `datasource` 문제가 풀리자 이번엔
  `NoSuchBeanDefinitionException: RestClient.Builder`로 컨텍스트 로딩 실패. 2.12절에서 추가한
  `KakaoTokenVerifier`/`AppleTokenVerifier`가 생성자에서 `RestClient.Builder`를 주입받는데, 이 빈을
  자동 구성해주는 `org.springframework.boot:spring-boot-restclient` 모듈이 의존성에 없었음
  (`spring-boot-starter-webmvc`는 서버 사이드 MVC만 제공하고 HTTP 클라이언트 자동구성은 Boot 4.1에서
  별도 모듈로 분리돼 있음). **테스트뿐 아니라 실제 `bootRun` 환경에서도 소셜 로그인(카카오/애플) 토큰
  검증이 애초에 기동 자체가 안 됐을 잠재적 실제 버그**였음 — 지금까지 아무도 눈치채지 못한 이유는
  `bootRun` 자체를 로컬 Postgres/Redis 없이 아직 한 번도 못 띄워봤기 때문(2.5/2.12절 참고).
- **수정 2**: `build.gradle.kts`에 `implementation("org.springframework.boot:spring-boot-restclient")` 추가.
- **검증**: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew test` 전체 통과(`BUILD SUCCESSFUL`,
  회귀 없음). `SmartOrderApplicationTests.contextLoads`를 포함해 로컬 Postgres/Redis/Kafka 없이도 전체
  스위트가 통과하는 것을 확인.
- **범위 밖으로 남긴 것**: `bootRun`으로 실제 로컬 기동(수동 curl 검증)은 여전히 안 해봄 — 로컬 Postgres/Redis
  인프라를 띄운 뒤 다음 세션에서 확인 권장.

### 2.14 PR 준비 — 코드 리뷰(`/code-review medium`) 발견 버그 2건 수정 — 완료
- **배경**: 4절 항목이 전부 완료돼 PR/머지 준비 단계로 넘어가면서, 커밋 전 `/code-review medium`으로 이 브랜치
  전체 워킹 트리 diff를 리뷰함. 이 브랜치는 이 시점까지 **커밋이 하나도 없이** 전부 워킹 트리에 쌓여 있는
  상태였음(`git log --oneline`에 이 브랜치 관련 커밋 없음, `origin`에도 브랜치 자체가 없음) — 커밋 이력이
  없어 리뷰는 `git diff HEAD` 기준 워킹 트리 diff로 진행.
- **수정 1 — `AuthService.login()`이 탈퇴/정지 계정도 로그인시킴**: `socialLogin`/`verifySms`/`refresh`는 전부
  `member.status != ACTIVE` 체크가 있는데 `login()`만 빠져 있었음. `DELETE /members/me`로 soft-delete(INACTIVE)한
  계정이 이메일/비밀번호로 재로그인해 정상 토큰을 받을 수 있는 실제 버그. 비밀번호 검증 성공 이후에
  `MEMBER_NOT_ACTIVE` 401 체크 추가(다른 메서드와 동일한 에러 코드/메시지). `AuthServiceTest`에 회귀 테스트 추가.
- **수정 2 — `/auth/logout`의 Redis 무효화 키가 Member/StoreAccount id 공간을 공유**: `AuthController.logout()`이
  `@AuthenticationPrincipal memberId: Long?`로 role 구분 없이 JWT subject를 그대로 받았는데, `STORE_ADMIN` 토큰의
  subject는 `storeAccount.storeId`라서 storeId와 memberId가 같은 숫자면 매장 관리자의 로그아웃이 무관한 회원의
  리프레시 토큰을 무효화시키는 실제 버그였음(매장 세션은 애초에 리프레시 토큰이 없어 무효화할 대상도 없는데
  잘못된 키에 마커만 남기는 부작용). `ROLE_STORE_ADMIN` 상수를 `AuthService` 내부 private에서
  `JwtTokenProvider` companion(public)으로 옮기고, `AuthController.logout()`을 `Authentication?` 파라미터로 바꿔
  role이 `STORE_ADMIN`이면 `memberId`를 `null`로 넘기도록 수정. `AuthControllerTest`에 회귀 테스트 추가.
- **보류 → 해결(2.15절)**: 같은 리뷰에서 나온 `.env`의 실제 값 `JWT_SECRET`이 git에 트래킹되는 문제는 처음엔
  이번 범위에서 제외하기로 했으나, 사용자가 바로 이어서 gitignore 처리를 요청해 2.15절에서 해결함. 회원가입
  동시성 레이스(`DataIntegrityViolationException` 미처리), JWT 4중 파싱 비효율, 빈 `AuthRepository` 인터페이스,
  토큰 발급 코드 중복은 경미한 수준으로 판단해 이번 범위에서 제외(필요시 후속 세션에서 처리).
- **`BACKEND_ROADMAP.md` 동기화**: Auth 행을 "대부분 구현됨" → "구현 완료"로 갱신, 소셜 토큰 서버 검증이 이미
  완료됐다는 내용(2.12절) 반영 — 기존 로드맵 문서가 그 부분을 반영 못 하고 있던 것을 바로잡음.
- **검증**: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew test` 전체 통과(`BUILD SUCCESSFUL`, 회귀 없음).
  신규 회귀 테스트 2건 포함 전부 통과.
- **다음 단계**: 아직 이 브랜치는 커밋이 하나도 없는 상태 — 커밋 그룹핑 후 커밋, 원격 push, PR 생성이 남음
  (사용자 확인 후 진행 예정).

### 2.15 `.env` 실제 비밀키 git 트래킹 문제 해결 — 완료
- **배경**: 2.14절에서 발견하고 일단 보류했던 항목. 사용자가 이어서 "`.env`는 gitignore 처리해줘"라고 명시적으로
  요청해 바로 처리함. 이 브랜치에 아직 커밋이 하나도 없는 상태라(2.14절 참고) `JWT_SECRET` 실제 값은 다행히
  아직 git 히스토리(과거 커밋)에는 없었음 — `git diff -- .env` 확인 결과 이번 브랜치에서 추가된 줄이었음. 즉
  이번 조치로 앞으로도 커밋에 들어갈 일이 없게 됨(과거 커밋 히스토리 재작성/시크릿 로테이션은 불필요).
- **조치**:
  - 루트 `.gitignore`에 `.env`/`.env.*` 무시 + `.env.example`만 예외로 남기는 규칙 추가(프론트엔드
    `frontend/.gitignore`의 `.env*` 관례와 동일한 방향).
  - `git rm --cached backend/.env`로 git 인덱스에서만 제거(로컬 워킹 디렉토리의 실제 `.env` 파일은 그대로 유지 —
    로컬 개발 환경 깨지지 않음, `test -f backend/.env`로 확인).
  - `backend/.env.example` 신규 생성 — 기존 `.env`와 동일한 키 목록에 `DB_PASSWORD`/`JWT_SECRET`만 `changeme`
    플레이스홀더로 교체해 온보딩 시 필요한 환경 변수 목록을 문서화(신규 개발자가 무엇을 채워야 하는지 알 수
    있도록).
- **검증**: `git check-ignore -v backend/.env` → 무시됨 확인, `backend/.env.example`은 무시되지 않고
  `git status`에 `??`로 정상 노출되는 것 확인. 로컬 `.env` 파일 자체는 삭제되지 않고 그대로 존재.
- **5절 리스크 항목 해소**: 아래 리스크 목록의 `.env`/`JWT_SECRET` 항목을 해결 완료로 갱신.

### 2.16 커밋 분리 + origin push — 완료
- **배경**: 사용자가 "커밋하고 push해줘"로 명시 요청. 이 시점까지 이 브랜치는 커밋이 하나도 없었음(2.14절 참고).
- **커밋 8개로 분리**(논리 단위별): JWT 인증 인프라 → Member 도메인 → Auth API(소셜/SMS/이메일 + 버그
  수정분) → 매장 관리자 로그인/인가 → 빌드/설정/테스트 프로필 → `.env` 시크릿 정리 → Claude Code 툴링 설정 →
  문서. 커밋 해시: `5e7686f`, `908127a`, `a6b3821`, `770b562`, `3927d02`, `aa0572f`, `362cd97`, `7265205`.
- **커밋 중 발견한 함정**: 일부 파일이 이전 세션에서 `git add`만 해두고 커밋하지 않은 채로 계속 수정돼 있어서
  (`git status` 접두사 `AM`/`RM`), 첫 커밋에서 `git add <경로들>` 후 `git commit`(pathspec 없이)을 실행했더니
  내가 add하지 않은 다른 파일들(인덱스에 미리 stage돼 있던 것들, 예: `AuthService.kt`)까지 그 시점의 **오래된
  인덱스 내용**으로 함께 커밋돼버림 — 워킹 트리엔 최신 내용(로그인 status 체크 수정분 등)이 그대로 있어서
  데이터 손실은 없었지만, 첫 커밋에는 수정 전 코드가 들어감. 이후 커밋부터는 `git commit -- <경로들>`(파일을
  pathspec으로 직접 지정, 인덱스 상태와 무관하게 현재 워킹 트리 내용만 커밋) 방식으로 바꿔서 해결 — 3번째
  커밋(Auth API)에서 `AuthService.kt`가 최신(버그 수정 반영) 내용으로 올바르게 커밋된 것을 `git show`로 확인함.
  최종적으로 8개 커밋 다 합친 결과물은 워킹 트리 최종 상태와 100% 일치(`git status` clean 확인).
- **push 인증 이슈**: `git push`가 `could not read Username for 'https://github.com'`로 실패(HTTPS 인증
  정보 없음). `gh auth login` 시도했으나 이 환경엔 `gh` CLI 자체가 미설치. `~/.ssh/id_ed25519`가 이미
  GitHub 계정(`codes-gy`)에 등록돼 있는 것을 `ssh -T git@github.com`으로 확인하고, `origin` 리모트 URL을
  HTTPS → SSH(`git@github.com:codes-gy/smart-order.git`)로 변경해 push 성공.
- **검증**: 커밋 완료 후 `./gradlew clean test` 전체 통과(`BUILD SUCCESSFUL`, 회귀 없음). `git push -u origin
  feature/gy/auth` 성공, 업스트림 추적 설정됨. GitHub이 PR 생성 링크를 안내함
  (`https://github.com/codes-gy/smart-order/pull/new/feature/gy/auth`).
- **다음 단계**: PR 생성만 남음(제목/본문 작성, `pr-generate` 에이전트 활용 가능) — 사용자 확인 후 진행.

## 4-1. 4절 항목 상태 — 전체 완료
0~7번 전 항목 완료, 2.11절 `StoreControllerTest` 수정, 2.12절 소셜 토큰 서버 검증, 2.13절
`SmartOrderApplicationTests` 로컬 인프라 이슈 수정까지 완료. 이 브랜치(`feature/gy/auth`)의 "다음 할 일"
목록은 모두 소진됨 — 남은 5절 리스크 항목은 SMS 벤더 연동, 그리고 카카오/애플 실계정 발급(2.12절)뿐. 이후
작업은 이 중 우선순위를 다시 정하거나, PR/머지 준비(리뷰, `BACKEND_ROADMAP.md`와의 최종 정합성 확인)로
넘어가면 됨.

## ⚠️ 3. 가장 중요한 발견 — 프론트 계약과 백엔드 구현 불일치

`frontend/src/types/auth.types.ts` + `frontend/src/api/authApi.ts` (+ `authMock.ts`)를 확인한 결과, **프론트는 이메일/비밀번호
회원가입·로그인 플로우 자체가 없음**. 실제 프론트 계약은:

- **고객 인증**: `socialLogin(provider: "kakao"|"apple", accessToken)` 또는 `sendSms`/`verifySms(phoneNumber, code)` 뿐.
  둘 다 성공 시 `{ user: AuthUser, tokens: AuthTokens }`를 바로 반환 — **별도 회원가입 스텝이 없고, 최초 로그인 시점에
  자동으로 회원이 생성(provision)되는 구조**로 보임.
- **매장(POS/태블릿) 로그인**: `storeLogin(storeCode, password)` → `StoreSession` 반환. 고객 세션과 완전히 분리된
  별도 스코프(현재 `Member`/`Role`로는 표현 안 됨. `Store` 엔티티 쪽 인증 정보가 필요해 보임).
- `AuthUser`는 `{ id, name, phoneNumber, isGuest }` 형태 — 백엔드 `Member`의 `nickname`과 이름이 다르고(`name` vs
  `nickname`), `isGuest` 개념이 백엔드에 없음.
- `GET /auth/me`(유저+리워드 요약), `GET /auth/rewards`, `GET /auth/favorites`, `DELETE /auth/account`(회원 탈퇴),
  `GET /auth/coupons` 도 프론트가 이미 호출하는 엔드포인트인데 백엔드에 전혀 없음.

**즉, 지금 구현된 `/auth/signup`, `/auth/login`(이메일/비밀번호)은 프론트가 실제로 쓰지 않는 플로우일 가능성이 높음.**
계속 이 방향으로 만들지, 소셜+SMS 중심으로 다시 짤지 사용자 확인이 필요함 (PRD 재확인 대상, `BACKEND_ROADMAP.md`도
"소셜 로그인, SMS 인증, 매장 관리자 로그인, 리프레시 토큰, 회원 탈퇴"를 필요 기능으로 이미 명시해뒀음 — 이메일/비밀번호는
로드맵에도 언급 없음).

### 2.5 SMS 인증 — 구현 완료
- `POST /auth/sms/send`, `POST /auth/sms/verify` 신규 구현 (`AuthController`/`AuthService`).
- `sendSms()`: 휴대폰 번호 형식 검증(`^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$`, 실패 시 400 `INVALID_PHONE`) →
  4자리 숫자 코드 생성 → `sms:auth:{phoneNumber}` 키로 Redis에 TTL 3분 저장 → `{ success: true }` 응답.
  실제 발송 벤더 연동 전이라 코드는 콘솔 로그(`[SMS mock] phoneNumber=... code=...`)로만 노출하는 mock.
- `verifySms()`: Redis에 저장된 코드와 대조, 불일치/만료 시 400 `INVALID_OTP`. 일치하면 코드 삭제(1회성) 후
  `phoneNumber` 기준 `Member` find-or-create(닉네임 "게스트")하고 `AuthDto.SocialLoginResponse`
  (`{ user, tokens }`, 소셜 로그인과 동일 쉐이프)를 반환 — `isGuest`는 재방문 여부와 무관하게 항상 `true`
  (프론트 계약, `useLoginForm.ts` 주석 "비회원 주문용" 플로우 반영).
- `MemberRepository.findByPhoneNumber()` 추가.
- `.env`에 로컬 개발용 `REDIS_HOST=localhost`/`REDIS_PORT=6379` 추가 — 기존엔 값이 없어 `bootRun`/`test` 시
  `${REDIS_HOST}` 플레이스홀더 해석 실패로 컨텍스트 로딩 자체가 안 되는 상태였음(이번에 같이 고침).
- **검증**: `./gradlew compileKotlin compileTestKotlin` 통과, `./gradlew test` 실행 결과 SMS/Auth 관련 테스트
  실패 없음(애초에 `AuthServiceTest`/`AuthControllerTest` 자체가 아직 없음 — 6절 참고). 로컬에 실제 Postgres/Redis
  서버가 없어 `bootRun`으로 엔드투엔드 수동 호출 검증은 못함 — 다음 세션에서 로컬 인프라 띄운 뒤 curl로 재확인 권장.
- **미해결 발견(이번 작업과 무관, 기존 이슈)**: `StoreControllerTest`(`@WebMvcTest`) 10건 전부 실패 —
  `SecurityConfig`가 전역 빈으로 걸리면서 `JwtAuthenticationFilter`가 `JwtTokenProvider`를 요구하는데
  `@WebMvcTest` 슬라이스엔 그 빈이 없어 컨텍스트 로딩 자체가 실패함. 이 브랜치의 2.1(JWT 인프라) 작업 때부터
  이미 깨져 있던 것으로 보임(이번 세션 시작 시점에 이미 `M` 상태였음). SMS 작업 범위 밖이라 손대지 않음 —
  별도 우선순위로 처리 필요(`@WebMvcTest(controllers = [StoreController::class])`에
  `@MockBean JwtTokenProvider`/`JwtAuthenticationFilter` 추가하거나 `SecurityConfig`를
  `@Import`에서 빼는 방향 검토).

## 4. 다음에 할 일 (우선순위 순)

### [x] 0. 프론트 계약 재확인 (착수 전 필수) — 완료, 2026-08-31
- 위 3절 불일치를 사용자에게 확인 완료: 소셜(카카오/애플)+SMS 중심으로 진행하기로 결정. 이메일/비밀번호 플로우는
  이미 구현된 채로 남겨두되(3절 참고) 프론트에 맞추는 작업은 별도 우선순위로 미룸.

### [x] 1. 소셜 로그인 구현 — 완료, 2026-08-31
**이유**:
- 프론트에 `SocialLoginButtons.tsx`가 이미 준비돼 있어 1차 로그인 수단으로 보임 (SMS는 `PhoneOtpForm.tsx`로 보조 수단에 가까움).
- 카카오/애플 토큰 검증은 각 사가 제공하는 공개 API(카카오 `/v1/user/access_token_info`) 또는 공개 JWKS(애플)로
  검증 가능 — 별도 벤더 계약/과금 없이 바로 구현 착수 가능.
- SMS는 실제 발송을 위해 외부 SMS 게이트웨이(NHN Cloud SENS, Twilio 등) 계정 개설·과금·발신번호 사전신고가
  선행돼야 해서 로컬 개발/테스트 착수가 상대적으로 더 늦어짐. `build.gradle.kts`의 `firebase-admin` 의존성이
  Firebase Phone Auth 활용을 염두에 둔 것인지도 확인 필요(현재는 확인 안 됨) — 확인되면 SMS 착수 속도가 달라질 수 있음.
- `AuthDto.SocialLoginRequest` DTO가 이미 정의돼 있어 바로 서비스 로직만 붙이면 됨(SMS는 DTO도 아직 없음).

**작업 내용 — 완료**: 2.4절 참고. `AuthService.socialLogin()`/`POST /auth/social-login` 구현 완료. 카카오/애플
토큰 서버 검증만 TODO로 남음(별도 후속 작업, 5절 리스크 참고).

### [x] 2. SMS 인증 구현 — 완료, 2026-08-31
- 작업 내용 — 2.5절 참고. 실제 발송 벤더(NHN Cloud SENS 등) 연동은 후속 작업으로 분리(콘솔 로그 mock으로 대체).

### [x] 3. Member 도메인 채우기 — 완료, 2026-08-31
**작업 내용 — 2.6절 참고.** `GET/PATCH/DELETE /members/me` 구현 완료 (`/auth/me`가 아닌 `/members/me` 채택 —
이유는 2.6절). `deleteAccount`는 `MemberStatus.INACTIVE` soft-delete로 결정. `rewards`는 0값 placeholder(5번
항목에서 실제 값으로 교체 예정).

### [x] 4. 매장(Store) 관리자 로그인 — 별도 인증 스코프 — 완료, 2026-08-31
- 작업 내용 — 2.7절 참고. `StoreAccount` 엔티티 신설로 Member와 완전 분리, `POST /auth/store-login` 구현 완료.

### [x] 5. 매장 관리자 인가 마무리 — 완료, 2026-08-31
- 작업 내용 — 2.8절 참고. `PATCH /stores/**`에 `hasRole("STORE_ADMIN")` 적용 + 소유 매장 검증
  (`requireOwnStore`) 추가. `StoreAccount` 프로비저닝은 `POST /stores` 확장으로 해결.

### [x] 6. Rewards / Favorites / Coupon 응답 연결 — placeholder로 완료, 2026-08-31
- 작업 내용 — 2.9절 참고. `GET /members/rewards`(0값)/`/members/favorites`/`/members/coupons`(빈 배열) 구현.
  실제 Coupon/적립/즐겨찾기 도메인이 생기면 `MemberService`의 세 메서드 내부만 교체 필요(후속 작업으로 이월).

### [x] 7. 마무리 — 완료, 2026-08-31
- 작업 내용 — 2.10절 참고. `/auth/logout`이 memberId 기준 Redis 마커로 리프레시 토큰을 무효화하도록 구현,
  `AuthServiceTest`(13건)/`AuthControllerTest`(9건) 추가, `BACKEND_ROADMAP.md` Auth 행 갱신까지 완료.

## 5. 리스크 / 확인 필요 항목
- **(해결됨, 2.15절)** ~~`.env`에 실제 값으로 보이는 `JWT_SECRET`이 git에 트래킹됨~~ — `.env`를 gitignore 처리하고
  `git rm --cached`로 인덱스에서 제거, `.env.example`(플레이스홀더만)로 분리 완료. 커밋 이력 자체가 아직
  없었던 브랜치라 과거 커밋에 시크릿이 남아있지 않음 — 로테이션 불필요.
- **이메일/비밀번호 vs 소셜/SMS**: 3절 참고, 가장 시급.
- **매장 관리자 인증 스코프 미정**: `Member`(고객)과 완전히 분리된 개념인지, 같은 테이블에 role만 다르게 둘지 결정 안 됨.
- **(해결됨, 2.12절)** ~~소셜 토큰 서버 검증 없음~~ — 카카오/애플 토큰을 각 제공자 서버(access_token_info,
  공개 JWKS)에 검증하도록 구현. 단 카카오 앱/애플 Services ID 실계정이 아직 없어 실토큰 엔드투엔드 검증은
  못함(계정 발급은 별도 우선순위).
- **SMS 실제 발송 벤더 미연동**: 지금은 코드를 콘솔 로그로만 노출하는 mock(2.5절) — 실제 서비스 전엔 NHN Cloud
  SENS 등으로 교체 필요. `build.gradle.kts`의 `firebase-admin` 의존성이 Firebase Phone Auth 대체 후보인지도
  미확인.
- **(해결됨, 2.11절)** ~~`StoreControllerTest` 10건 전부 실패~~ — `@Import`/`@MockitoBean` 보강으로 해결.
  부수적으로 `GET /stores/**`가 의도치 않게 인증을 요구하던 실제 버그도 함께 발견·수정(`permitAll`로 공개).
- **(해결됨, 2.13절)** ~~`SmartOrderApplicationTests` 실패~~ — 테스트 전용 `test` 프로필(H2) 분리 +
  `spring-boot-restclient` 의존성 추가로 해결.
- **(해결됨, 2.8절)** ~~`StoreAccount` 프로비저닝 API 없음~~ — `POST /stores` 확장으로 해결.
- **(해결됨, 2.8절)** ~~매장 관리자 토큰이 아직 아무것도 잠그지 않음~~ — `hasRole("STORE_ADMIN")` +
  `requireOwnStore()` 소유권 검증으로 해결.
- **(해결됨, 2.11절)** ~~`StoreControllerTest` 컨텍스트 로딩 실패~~ — 2.11절 참고.
