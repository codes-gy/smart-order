// 출근 시간대(아침 피크) 주문 폭주 시나리오 — k6
//
// 무엇을 보나
//   1. POST /orders 가 피크 RPS에서 에러 없이(201) 버티는지, 지연(p95/p99)이 얼마인지
//   2. 같은 멱등성 키로 "따닥" 재전송(더블 탭 · 네트워크 재시도)이 섞여도 주문이 중복 생성되지 않는지
//      (재전송 응답의 orderId가 원래 응답과 같아야 한다 → idempotent_mismatch 카운터가 0이어야 한다)
//
// 실행 방법은 load-test/README.md 참고. 여기 적힌 thresholds는 "목표치(가설)"이지 측정 결과가 아니다.

import http from 'k6/http';
import { check, fail } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import exec from 'k6/execution';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const STORE_ID = Number(__ENV.STORE_ID || 1);
const MENU_ID = Number(__ENV.MENU_ID || 1);
const USERS = Number(__ENV.USERS || 100);           // 동시에 주문하는 회원 수(토큰 개수)
const PEAK_RPS = Number(__ENV.PEAK_RPS || 100);     // 피크 구간 초당 주문 시도 수
const RETRY_RATIO = Number(__ENV.RETRY_RATIO || 0.1); // 같은 키로 즉시 재전송하는 비율(0~1)
const RUN_ID = __ENV.RUN_ID || `${Date.now()}`;
const PASSWORD = 'loadtest1234';

const idempotentMismatch = new Counter('idempotent_mismatch');
const orderCreateDuration = new Trend('order_create_duration', true);

export const options = {
  setupTimeout: '5m',
  scenarios: {
    // 07:30~08:30 한 시간을 압축: 워밍업 → 08:00 전후 급증 → 피크 유지 → 해소
    morning_peak: {
      executor: 'ramping-arrival-rate',
      startRate: Math.max(1, Math.floor(PEAK_RPS / 10)),
      timeUnit: '1s',
      preAllocatedVUs: Math.max(50, PEAK_RPS),
      maxVUs: Math.max(200, PEAK_RPS * 4),
      stages: [
        { target: Math.floor(PEAK_RPS / 5), duration: '1m' }, // 워밍업
        { target: PEAK_RPS, duration: '30s' },               // 출근 직전 급증
        { target: PEAK_RPS, duration: '3m' },                // 피크 유지
        { target: Math.floor(PEAK_RPS / 5), duration: '1m' }, // 해소
      ],
    },
  },
  thresholds: {
    // 목표치(가설). 실측 후 README 결과 표에 실제 값을 기록한다.
    http_req_failed: ['rate<0.01'],
    order_create_duration: ['p(95)<500', 'p(99)<1000'],
    idempotent_mismatch: ['count==0'],
  },
};

const jsonHeaders = { 'Content-Type': 'application/json' };

// 회원 USERS명을 만들고(이미 있으면 로그인) access token 목록을 VU들에 넘긴다.
export function setup() {
  const tokens = [];
  for (let i = 0; i < USERS; i++) {
    const email = `loadtest-${RUN_ID}-${i}@example.com`;
    let res = http.post(
      `${BASE_URL}/auth/signup`,
      JSON.stringify({ email, password: PASSWORD, nickname: `부하테스트${i}` }),
      { headers: jsonHeaders, tags: { name: 'setup_signup' } },
    );
    if (res.status === 409) {
      res = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({ email, password: PASSWORD }),
        { headers: jsonHeaders, tags: { name: 'setup_login' } },
      );
    }
    if (res.status !== 200 && res.status !== 201) {
      fail(`회원 준비 실패 (i=${i}, status=${res.status}): ${res.body}`);
    }
    tokens.push(res.json('accessToken'));
  }
  return { tokens };
}

function createOrder(token, idempotencyKey) {
  const body = JSON.stringify({
    storeId: STORE_ID,
    items: [{ menuId: MENU_ID, quantity: 1, optionChoiceIds: [] }],
    packagingType: 'TAKE_OUT',
    useStamp: false,
  });
  return http.post(`${BASE_URL}/orders`, body, {
    headers: {
      ...jsonHeaders,
      Authorization: `Bearer ${token}`,
      'X-Idempotency-Key': idempotencyKey,
    },
    tags: { name: 'POST /orders' },
  });
}

export default function (data) {
  const token = data.tokens[exec.scenario.iterationInTest % data.tokens.length];
  const key = `k6-${RUN_ID}-${exec.scenario.iterationInTest}`;

  const res = createOrder(token, key);
  orderCreateDuration.add(res.timings.duration);
  const ok = check(res, {
    'order created (201)': (r) => r.status === 201,
    'has orderId': (r) => r.status === 201 && !!r.json('orderId'),
  });

  // 일부 요청은 같은 키로 즉시 재전송 — 같은 orderId가 돌아와야 한다(중복 주문 금지).
  if (ok && Math.random() < RETRY_RATIO) {
    const retry = createOrder(token, key);
    const same = retry.status === 201 && retry.json('orderId') === res.json('orderId');
    check(retry, { 'retry returns same orderId': () => same });
    if (!same) idempotentMismatch.add(1);
  }
}
