# Querydsl web
- 공식 문서 : https://docs.spring.io/spring-data/jpa/docs/2.2.3.RELEASE/reference/html/#core.web.type-safe

- 단점이 명확하다.
- 단순한 조건만 가능하다.
- `left join`이 안되고, 복잡해지면 사용하기가 힘듬
- `eq`이나 `contains`, `in` 정도만 됨.
- 기능도 별로 없고..
- 또 Controller가 Querydsl에 의존하고 있음