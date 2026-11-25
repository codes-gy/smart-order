# Flyway 마이그레이션은 prod에만 쓰고, local · test는 H2 ddl-auto로 만든다

prod는 `ddl-auto: validate`인데 스키마를 만드는 스크립트가 없었다. Flyway를 도입하면서, 엔티티를 자주 바꾸는 개발 단계에서 매번 마이그레이션을 쓰는 비용을 피하려고 prod 프로필에만 `spring.flyway.enabled: true`를 켜고 local(`ddl-auto: update`)과 test(H2 `create-drop`, PostgreSQL 호환 모드)는 엔티티에서 스키마를 만든다.

근거: 5cf1435, PR #14, `V1__init.sql`, `V2__orders_idempotency_key_per_member.sql`.

## Consequences

- 엔티티를 바꾸면 `backend/src/main/resources/db/migration/Vn__*.sql`을 직접 추가해야 한다. 테스트는 Flyway를 거치지 않으므로 마이그레이션이 빠지거나 틀려도 CI는 통과하고, prod 기동 때 `validate`에서야 드러난다.
- H2와 PostgreSQL의 차이(예약어 인용 등)는 테스트에서 일부만 잡힌다. `Category.order` 예약어 컬럼은 H2에서 테이블이 안 만들어지는 형태로 뒤늦게 발견됐다(9f64faa).
