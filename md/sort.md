## 스프링 데이터 정렬(Sort)
- 스프링 데이터 JPA는 자신의 정렬(Sort)을 Querydsl의 정렬(OrderSpecifier)로 편리하게 변경하는
  기능을 제공한다.
- 이 부분은 뒤에 스프링 데이터 JPA가 제공하는 Querydsl 기능에서 살펴보겠다. 스프링 데이터의 정렬을 Querydsl의 정렬로 직접 전환하는 방법은 다음 코드를 참고하자.

> 스프링 데이터 Sort를 Querydsl의 OrderSpecifier로 변환
```java
JPAQuery<Member> query = queryFactory
          .selectFrom(member);

  for (Sort.Order o : pageable.getSort()) {
  
         PathBuilder pathBuilder = new PathBuilder(member.getType(),
    member.getMetadata());
        query.orderBy(new OrderSpecifier(o.isAscending() ? Order.ASC : Order.DESC,
                pathBuilder.get(o.getProperty())));
    }
  
List<Member> result = query.fetch();
```