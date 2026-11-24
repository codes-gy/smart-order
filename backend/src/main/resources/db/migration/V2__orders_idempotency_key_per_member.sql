-- 멱등성 키 유니크 범위를 전역 -> 회원 단위로 좁힌다.
-- V1에서 컬럼 레벨 UNIQUE로 만든 제약은 PostgreSQL 기본 이름 규칙에 따라 orders_idempotency_key_key 이다.
-- 전역 유니크에서는 회원 A가 쓴 키를 회원 B가 우연히 다시 쓰면 B에게 A의 주문 ID가 반환됐다.
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_idempotency_key_key;
ALTER TABLE orders ADD CONSTRAINT uk_orders_member_idempotency_key UNIQUE (member_id, idempotency_key);
