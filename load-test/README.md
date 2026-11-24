# 부하 테스트 (k6) — 출근 시간대 주문 폭주

`morning-peak-order.js`는 아침 피크(07:30~08:30)를 몇 분으로 압축해 `POST /orders`에 주문을 몰아넣는 시나리오다.

| 구간 | 시간 | 초당 주문 시도(RPS) |
| :--- | :--- | :--- |
| 워밍업 | 1분 | `PEAK_RPS / 10` → `PEAK_RPS / 5` |
| 출근 직전 급증 | 30초 | → `PEAK_RPS` |
| 피크 유지 | 3분 | `PEAK_RPS` |
| 해소 | 1분 | → `PEAK_RPS / 5` |

- 요청의 `RETRY_RATIO`(기본 10%)는 **같은 `X-Idempotency-Key`로 즉시 재전송**한다(더블 탭·네트워크 재시도 재현).
  재전송 응답의 `orderId`가 원래 응답과 다르면 `idempotent_mismatch` 카운터가 올라간다 → 0이어야 정상.
- `thresholds`(에러율 < 1%, p95 < 500ms, p99 < 1s)는 **목표치(가설)** 이다. 아직 측정한 값이 아니다.
- SSE(`GET /orders/{id}/events`) 동시 구독 부하는 k6 기본 기능으로 다루기 어려워 이 시나리오에 넣지 않았다.

## 사전 준비

1. 백엔드를 띄운다 (PostgreSQL + Redis 필요, `backend/.env.example` 참고).
   ```bash
   cd backend && ./gradlew bootRun
   ```
2. 주문할 매장 1개와 판매 중(`ON_SALE`) 메뉴 1개를 만들어 두고 그 ID를 확인한다(Swagger `/swagger-ui.html` 또는 SQL).
3. [k6 설치](https://grafana.com/docs/k6/latest/set-up/install-k6/).

`setup()` 단계에서 `loadtest-<RUN_ID>-<n>@example.com` 회원을 `USERS`명 가입시키고(이미 있으면 로그인) 토큰을 나눠 쓴다.
**운영 DB에는 절대 실행하지 말 것** — 테스트 회원과 주문이 실제로 쌓인다.

## 실행

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e STORE_ID=1 -e MENU_ID=1 \
  -e USERS=100 -e PEAK_RPS=100 -e RETRY_RATIO=0.1 \
  --summary-export=load-test/results/$(date +%Y%m%d-%H%M)-summary.json \
  load-test/morning-peak-order.js
```

| 환경변수 | 기본값 | 의미 |
| :--- | :--- | :--- |
| `BASE_URL` | `http://localhost:8080` | 백엔드 주소 |
| `STORE_ID` / `MENU_ID` | `1` / `1` | 주문 대상 매장·메뉴 |
| `USERS` | `100` | 주문하는 회원(토큰) 수 |
| `PEAK_RPS` | `100` | 피크 구간 초당 주문 시도 수 |
| `RETRY_RATIO` | `0.1` | 같은 멱등성 키로 재전송하는 비율 |
| `RUN_ID` | 현재 시각(ms) | 회원 이메일·멱등성 키 접두사 |

## 결과

> **측정 예정** — 아직 실측하지 않았다. 아래 표는 실행 후 실제 값으로만 채운다(추정치를 적지 않는다).

| 측정일 | 환경(인스턴스/DB/풀 크기) | PEAK_RPS | 실제 처리 RPS | p95 | p99 | 에러율 | idempotent_mismatch |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 측정 예정 | - | - | - | - | - | - | - |
