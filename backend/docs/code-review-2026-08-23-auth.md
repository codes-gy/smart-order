# 코드리뷰 결과 — feature/gy/auth (2026-08-23)

대상: `feature/gy/auth` 브랜치 diff (Auth/Member 도메인 추가, SecurityConfig 재구성)

## 1. [Critical] `local` 프로필 부팅 실패 — 환경변수 불일치
- **파일**: `src/main/resources/application.yaml:58`
- **문제**: `local` 프로필이 `DB_URL`, `REDIS_HOST`, `REDIS_PORT`, `KAFKA_SERVER` 환경변수를 요구하지만, `.env`에는 `DB_HOST`/`DB_PORT`/`DB_NAME`만 정의되어 있고 `DB_URL`과 Redis/Kafka 관련 값은 없음.
- **재현 시나리오**: `./gradlew bootRun` 실행 시 `PlaceholderResolutionException`으로 부팅 자체가 실패함.
- **조치**: `.env`에 `DB_URL`, `REDIS_HOST`, `REDIS_PORT`, `KAFKA_SERVER`를 추가하거나, `application.yaml`의 플레이스홀더를 기존 `.env` 키에 맞게 조정.

## 2. [Critical] `.env`에 JWT 시크릿 평문 커밋
- **파일**: `.env:8`
- **문제**: 실제 JWT 서명 시크릿이 git으로 추적되는 `.env` 파일에 평문으로 커밋됨 (`.gitignore` 예외 없음).
- **재현 시나리오**: 저장소(또는 과거 히스토리) 접근 권한이 있는 사람 누구나 시크릿을 읽어 임의의 memberId/role(ADMIN 포함)로 유효한 토큰을 위조할 수 있음. `backend/CLAUDE.md`의 "민감 데이터 하드코딩 금지" 원칙 위반.
- **조치**: 시크릿 로테이션 + git 히스토리에서 제거, `.env`는 `.gitignore`에 추가하고 `.env.example`만 커밋.

## 3. [High] 403 응답이 프로젝트 에러 규격을 따르지 않음
- **파일**: `src/main/kotlin/com/gy/smartorder/config/security/SecurityConfig.kt:43`
- **문제**: `exceptionHandling`에 `authenticationEntryPoint`(401)만 설정되어 있고 `accessDeniedHandler`(403)는 미설정. `/admin/**`는 `hasRole("ADMIN")`을 요구함에도 불구하고.
- **재현 시나리오**: 인증은 됐지만 권한 없는 사용자가 `/admin/**` 호출 시 Spring 기본 `AccessDeniedHandlerImpl`이 동작 → 프로젝트 표준 `ErrorResponse{code,message,details}`가 아닌 Whitelabel/기본 에러 바디 반환 → 프론트 `ApiErrorBody` 파싱 실패.
- **조치**: `JwtAuthenticationEntryPoint`와 짝을 이루는 `AccessDeniedHandler` 구현 후 등록.

## 4. [High] CORS: `allowedOriginPatterns("*")` + `allowCredentials(true)` 조합
- **파일**: `src/main/kotlin/com/gy/smartorder/config/security/SecurityConfig.kt:82`
- **문제**: 와일드카드 오리진 패턴과 credential 허용을 동시에 사용 — `allowedOrigins`와 달리 `allowedOriginPatterns`는 와일드카드+credentials 조합에서도 예외 없이 통과됨.
- **재현 시나리오**: 임의의 외부 사이트가 쿠키/Authorization 헤더를 포함한 크로스 오리진 요청을 보내도 브라우저가 이를 허용 — CORS 보호가 사실상 무력화됨. 코드 주석에도 "운영 환경에서는 허용 도메인 명시 권장"이라 적혀있지만 실제로는 프로필 구분 없이 무제한 적용됨.
- **조치**: 프로필별로 허용 오리진을 명시적으로 제한 (특히 `prod`).

## 5. [Medium] 회원가입 동시 요청 시 유니크 제약 위반이 500으로 새어나감
- **파일**: `src/main/kotlin/com/gy/smartorder/services/auth/AuthService.kt:24`
- **문제**: `signup()`이 `findByEmail`로 중복 체크 후 `save()`하는 check-then-act 방식만 사용하고, Order/Payment 도메인에 이미 있는 "조회 → 저장 → `DataIntegrityViolationException` catch 후 재조회" 패턴을 따르지 않음. `Member.phoneNumber`도 `unique = true`라 동일 취약점 존재.
- **재현 시나리오**: 동일 이메일(또는 전화번호)로 동시에 두 개의 가입 요청이 들어오면 둘 다 중복 체크를 통과한 뒤 두 번째 `save()`에서 제약 위반 → catch되지 않아 `handleUnexpected`로 떨어져 500 `INTERNAL_ERROR` 반환 (의도된 409 `EMAIL_ALREADY_EXISTS` 대신).
- **조치**: Order/Payment와 동일한 lookup-then-catch-and-refetch 패턴 적용.

## 6. [Low] `Member.email`/`phoneNumber` 인덱스 중복 생성
- **파일**: `src/main/kotlin/com/gy/smartorder/entities/member/Member.kt:45`
- **문제**: `@Column(unique = true)`가 이미 유니크 인덱스를 생성하는데, 테이블 레벨 `@Index(columnList = "email"/"phoneNumber")`가 동일 컬럼에 대해 별도 인덱스를 추가로 생성함.
- **영향**: INSERT/UPDATE마다 동일 데이터를 커버하는 인덱스 2개를 유지해야 하므로 쓰기 비용/스토리지 낭비.
- **조치**: 중복되는 `@Index` 선언 제거.

## 7. [Low] JWT 필터에서 서명 검증 반복 수행
- **파일**: `src/main/kotlin/com/gy/smartorder/config/passport/JwtAuthenticationFilter.kt:285`
- **문제**: 요청마다 `validateToken`, `getTokenType`, `getMemberId`, `getRole`을 각각 호출하는데, 각 함수가 독립적으로 `parseClaims`를 호출해 서명 검증을 최대 4번 반복함.
- **영향**: 모든 인증된 요청(핫 패스)에서 불필요한 HMAC 검증/JSON 파싱 비용 발생.
- **조치**: 토큰을 한 번만 파싱해 `Claims`를 재사용하도록 리팩터링.

## 8. [Low] 사용하지 않는 빈 `AuthRepository`
- **파일**: `src/main/kotlin/com/gy/smartorder/repositories/auth/AuthRepository.kt:3`
- **문제**: `JpaRepository`를 상속하지 않는 빈 인터페이스이며 어디서도 참조되지 않음 (`AuthService`는 `MemberRepository`를 사용).
- **조치**: 사용하지 않는 스캐폴딩이므로 삭제.

---
### 우선순위 요약
| # | 심각도   | 항목                                |
|---|----------|-------------------------------------|
| 1 | Critical | local 프로필 부팅 실패 (env 불일치) |
| 2 | Critical | JWT 시크릿 평문 커밋                |
| 3 | High     | 403 응답 규격 미준수                |
| 4 | High     | CORS 와일드카드+credentials         |
| 5 | Medium   | 회원가입 동시성 race → 500          |
| 6 | Low      | 중복 인덱스                         |
| 7 | Low      | JWT 파싱 중복 수행                  |
| 8 | Low      | 미사용 AuthRepository               |
