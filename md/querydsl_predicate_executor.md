- 공식 : https://docs.spring.io/spring-data/jpa/docs/2.2.3.RELEASE/reference/html/#core.extensions.querydsl

```java
@DisplayName("QuerydslPredicateExecutor")
@Test
void querydslPredicateExecutorTest() {
    Team teamA = new Team("teamA");
    Team teamB = new Team("teamB");
    em.persist(teamA);
    em.persist(teamB);

    Member member1 = new Member("member1", 10, teamA);
    Member member2 = new Member("member2", 20, teamA);
    Member member3 = new Member("member3", 30, teamB);
    Member member4 = new Member("member4", 40, teamB);
    em.persist(member1);
    em.persist(member2);
    em.persist(member3);
    em.persist(member4);

    Iterable<Member> result = memberRepository.findAll(member.age.between(10, 40).and(member.username.eq("member1")));

    for (Member findMember : result) {
        System.out.println("findMember = " + findMember);
    }
}
```

##### 한계
- 조인이 안됨
- 묵시적 조인은 되는데, left join이 안됨
- 클라이언트 코드가 Querydsl에 의존해야함
    - `Repository`를 호출할텐데, findAll에서 넘기는 부분이 바로 querydsl의 `predicate` 잖아요.
    - 서비스 클래스가 `querydsl`이라는 구현 기술에 의존해야 한다.
    - 리파지토리를 만드는 이유는
        - 그 하부에 querydsl과 같은 구체적인 기술을 숨기는 것
        - 나중에 바꿀 때 이 부분만 고치면 되니까..
        - 그런데 이런 방식으로는 의존 관계가 생겨버려서 문제임
- 복잡한 실무 환경(지금처럼 한개의 엔티티만 사용하는 방식은 실무에서 거의 사용하지 않음)에서는 사용하기 어려움

> 참고 : `QuerydslPredicateExecutor`는 Pageable, Sort 모두 지원/정상 동작