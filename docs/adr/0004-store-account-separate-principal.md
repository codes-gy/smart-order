# 매장 관리자는 회원과 분리된 매장 계정으로 인증하고, 토큰 주체는 storeId다

매장 POS·태블릿 로그인은 `Member`의 역할로 두지 않고 별도 `StoreAccount`(매장 코드 + 비밀번호, 매장당 1개)로 분리했다. 같은 `JwtTokenProvider`가 두 종류를 함께 발급하므로, 토큰의 `subject`는 회원 토큰이면 memberId, `STORE_ADMIN` 토큰이면 **storeId**이고 어느 쪽인지는 `role` 클레임으로 구분한다. 컨트롤러의 `@AuthenticationPrincipal`도 역할에 따라 memberId 또는 storeId가 된다. `StoreAccount.storeId`는 관계가 아닌 단순 유니크 FK라, 나중에 직원별 계정으로 늘릴 때 유니크 제약만 풀면 된다.

근거: c187e84(매장 관리자 로그인), `StoreAccount` KDoc, `JwtTokenProvider` 주석, e09863e · PR #17(주문·결제·메뉴 소유권 검증).

## Consequences

- `STORE_ADMIN` 역할 검사만으로는 다른 매장의 리소스를 막지 못한다. 매장 리소스를 바꾸는 서비스는 principal(storeId)과 리소스의 매장을 직접 비교해야 한다(PR #17에서 카테고리·메뉴·옵션에 추가).
- `Member.Role.ADMIN`은 매장 관리자와 무관한 별개 역할이다.
- 매장 계정을 만드는 API는 없고 사전에 DB로 넣는다고 가정한다.
