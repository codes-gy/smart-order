# 쿠폰·스탬프 소비는 조건부 UPDATE 한 번으로만 한다

쿠폰 사용과 스탬프 리워드 사용은 "조회 → 미사용 확인 → 엔티티 수정"이라 동시 요청 두 건이 둘 다 성공할 수 있었고, 스탬프 적립(`stampCount += 1`)은 동시에 픽업 완료된 주문의 적립 하나가 사라졌다. 비관적 락(`SELECT ... FOR UPDATE`)이나 `@Version` 낙관적 락 대신, 검사와 변경을 UPDATE 한 문장에 넣는 조건부 UPDATE를 골랐다. 쿠폰·스탬프는 "한 번만 바뀌면 되는" 전이라 재시도 로직이 필요 없고 락을 오래 쥐지도 않기 때문이다. 갱신된 행이 0이면 이미 쓴 것으로 보고 409(`COUPON_ALREADY_USED`, `STAMP_NOT_ENOUGH`)를 던진다.

- 쿠폰: `CouponRepository.markUsedIfAvailable` (`used_at IS NULL AND expires_at > now`)
- 스탬프 리워드: `MemberRepository.resetStampIfReached` (`stamp_count >= 10`일 때만 0으로)
- 스탬프 적립: `MemberRepository.incrementStamp` (`stamp_count = stamp_count + 1`)

근거: 6a7ea04, PR #20, `OrderConcurrencyIntegrationTest`(16스레드 동시 요청).

## Consequences

- 서비스의 사전 조회(`isUsed`, `stampCount < 10`)는 에러 구분과 빠른 실패용일 뿐이다. 소비 판정은 반드시 UPDATE의 갱신 행 수로 한다.
- 벌크 UPDATE는 `@LastModifiedDate`를 거치지 않으므로 수정 시각을 쿼리에서 직접 넣는다.
