# 순수 JPA 레포지토리와 Querydsl

## 순수 JPA Repository
```java
public Optional<Member> findById(Long id) {
    Member findMember = em.find(Member.class, id);
    return Optional.ofNullable(findMember);
}

public List<Member> findAll() {
    return em.createQuery("select m from Member m", Member.class)
             .getResultList();
}

public List<Member> findByUsername(String username) {
    return em.createQuery("select m from Member m where m.username = :username", Member.class)
             .setParameter("username", username)
             .getResultList();
}
```

Test
```java
@DisplayName("순수 JPA Repository 테스트")
@Test
void basicTest() {
    Member member = new Member("member1", 10);
    memberJpaRepository.save(member);

    Member findMember = memberJpaRepository.findById(member.getId()).get();
    assertEquals(member, findMember); // 같은 영속성 컨텍스트 안에 있기 때문

    List<Member> result = memberJpaRepository.findAll();
    assertEquals(member, result.get(0));

    List<Member> result2 = memberJpaRepository.findByUsername("member1");
    assertEquals(member, result2.get(0));
}
```

## Querydsl Repository
```java
public Optional<Member> findById_querydsl(Long id) {
    return Optional.ofNullable(queryFactory.selectFrom(member)
                                           .where(member.id.eq(id))
                                           .fetchOne());
}

public List<Member> findAll_querydsl() {
    return queryFactory
            .selectFrom(member)
            .fetch();
}

public List<Member> findByUsername_querydsl(String username) {
    return queryFactory
            .selectFrom(member)
            .where(member.username.eq(username))
            .fetch();
}
```


Test
```java
@DisplayName("querydsl Repository 테스트")
@Test
void querydsl_Test() {
    Member member = new Member("member1", 10);
    memberJpaRepository.save(member);

    Member findMember = memberJpaRepository.findById_querydsl(member.getId()).get();
    assertEquals(member, findMember); // 같은 영속성 컨텍스트 안에 있기 때문

    List<Member> result = memberJpaRepository.findAll_querydsl();
    assertEquals(member, result.get(0));

    List<Member> result2 = memberJpaRepository.findByUsername_querydsl("member1");
    assertEquals(member, result2.get(0));
}
```


### 취향차이
JPAQueryFactory를 만들 때
`JPAQueryFactory queryFactory = new JPAQueryFactory(em);` 과 같은 방식을 하기 마련이다.

이런 방법도 존재한다.
```java
/**
 * Spring Bean으로 등록해놓고 다른 코드에서 DI 형태로 사용할 수도 있다.
 */
@Bean
JPAQueryFactory jpaQueryFactory(EntityManager em) {
    return new JPAQueryFactory(em);
}

....

@Autowired
JPAQueryFactory queryFactory;
```

- 2개를 주입 받았을 때(Bean 생성 후 갖다 쓰는 방식)
    - 장점 : 롬복 `RequiredArgumentConstructor`를 사용할 수 있음
    - 단점 : `EntityManager`와 `JPAQueryFactory`를 모두 등록해야함.
- 그 전 방식(레포지토리 생성자에서 QueryFactory에 EntityManager를 주입해주는 방법)
    - 장점 : 테스트 코드가 좀 더 작성하기가 편리해짐
    - 단점 : 롬복 `RequiredArgumentConstructor`를 사용할 수 없음
    

## 동시성 문제?
- 같은 객체를 멀티 스레드에서 같이 쓰는데 문제 없을까요?
    - 어차피 엔티티 매니져도 잘 보면 싱글톤이고 여러 곳에서 접근해도 문제가 없거든요.
    - 그 이유는 JPAQueryFactory의 동시성 문제는 EntityManager에 의존하게 되는데
    - 이 `EntityManager`는 Spring에 엮여서 쓰면 동시성과 상관없이 트랜잭션 단위로 따로따로 분리되어 동작하게 됨
    - 스프링에서는 EntityManager를 진짜 영속성 컨텍스트를 주는 것이 아니라, 프록시를 제공하는데
    - 이를 `트랜잭션 단위`로 다 다른 곳에 바인딩되도록 라우팅을 해줍니다.
    -> 문제 없다!
      