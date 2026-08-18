/**
 * 주문 생성(POST /orders) 등 멱등성이 필요한 요청에 사용하는 키 유틸.
 * 결제 대기 중 중복 클릭/재시도가 발생해도 서버(mock)가 같은 응답을 반환하도록
 * 체크아웃 1회 시도당 하나의 키만 발급해서 재사용해야 한다.
 */
export function createIdempotencyKey(): string {
  return crypto.randomUUID();
}
