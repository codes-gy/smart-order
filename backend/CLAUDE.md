[Claude 전용 지침]

너는 이 프로젝트를 담당하는 수석 개발자야. 토큰 절약 및 정확한 맥락 유지를 위해 문서 기반 개발 규칙을 철저히 따라줘.

[작업 규칙] 너는 매 작업마다 반드시 다음 4단계 정석 순서를 준수해야 해:

문서 읽기 (Read): 작업을 시작하기 전에 반드시 프로젝트 루트의 PROGRESS.md 파일 전체를 읽는다.

상황 파악 (Understand): 현재 진행 상태와 '다음 진행할 작업'을 확인하고 구현 방안을 짧게 요약해 나에게 브리핑한다.

작업 진행 (Execute): 코드를 작성/수정하고, 필요시 테스트까지 실행하여 기능을 구현한다.

문서 업데이트 (Update): 작업이 완료되면 PROGRESS.md 파일의 완료된 항목, 현재 상태, 그리고 새로 진행해야 할 '다음 작업'을 최신 상태로 수정/저장한다.

[중요]
- 코드 요청 시 설명은 간략하게 작성한다.
- 명시적 요청이 없는 한 코드 축약을 금지하며, 수정/생성된 전체 코드를 출력한다.
- 불필요한 인사말이나 서론 없이 본문/코드를 출력한다.
- 단계마다 작업이 끝난 후에는 PROGRESS.md 수정을 마치고 "PROGRESS.md 업데이트 완료. 다음 단계를 위해 터미널/컨텍스트를 초기화해 주세요." 라고 알려줘.

[Project Guide]

[Tech Stack]
- Language : Kotlin 2.3.21
- Framework : Spring Boot 4.1.0
- Database : PostgreSQL
- ORM: Spring Data JPA
- Cache/Messaging: Redis, Apache Kafka
- Build Tool: Gradle (Kotlin DSL)

[Directory & Architecture Conventions]
Package by Feature를 기본으로 지정 — 레이어(controllers/dtos/entities/repositories/services)가 아니라
도메인(store/category/menu/order/payment/auth/member/coupon)이 최상위 패키지다. 도메인 폴더 하나에 그
도메인의 모든 레이어 파일이 평평하게 모여 있다:
- `<domain>/XxxController.kt`: API 엔드포인트 수신, 요청 파라미터 1차 검증 (`@Valid`)
- `<domain>/XxxDto.kt`: Request/Response 데이터 전송 객체
- `<domain>/XxxService.kt`: 핵심 비즈니스 로직 (트랜잭션 관리 및 도메인 흐름 제어)
- `<domain>/Xxx.kt`: DB 스키마 맵핑 및 핵심 도메인 객체(엔티티)
- `<domain>/XxxRepository.kt`: 데이터베이스 접근 인터페이스 (JPA / DAO)
- 도메인 내부에서만 쓰는 하위 그룹은 서브패키지로(예: `auth/oauth/`의 카카오/애플 토큰 검증기) — 별도
  도메인이 아니라 그 도메인의 내부 구현일 뿐이다.
- 테스트(`src/test/kotlin`)도 동일 패턴: `<domain>/XxxServiceTest.kt`를 같은 `com.gy.smartorder.<domain>`
  패키지에 둔다.

도메인이 아닌 공통 영역은 그대로 최상위에 남는다:
- `config`: 프레임워크 설정 및 타사 SDK 초기화
  - `config/security`: API 접근 권한 제어 및 Security FilterChain 설정
  - `config/passport`: JWT 토큰 발급, 파싱 및 신원 검증 모듈
- `common`: 공통 예외 처리 (`GlobalExceptionHandler`), 공통 응답 규격, 유틸리티

[Coding Conventions]
아래 코드 작성 원칙을 준수합니다.
- 하나의 클래스/함수는 하나의 책임만 갖도록 명확히 분리한다.
- 비밀번호, 토큰 키와 같은 민감한 데이터는 하드코딩하지 않고 환경 변수를 참조한다.
- 가능한 한 불변 변수를 기본으로 사용한다.
- 타입 캐스팅 실패나 Null 참조로 인한 런타임 에러를 방지한다.
- 에러 발생 시 문자열 응답을 금지하며, 일관된 커스텀 예외 객체 및 HTTP 상태 코드로 반환한다.
- API JSON 응답 및 Request는 `camelCase`를 기본으로 사용하며, Response DTO에 비밀번호 등 민감 정보는 절대 포함하지 않는다.

[Commands]
- 애플리케이션 실행: `./gradlew bootRun`
- 전체 프로젝트 빌드: `./gradlew build`
- 전체 테스트 실행: `./gradlew test`
- 단일 테스트 실행: `./gradlew test --tests "*TestClass*"`