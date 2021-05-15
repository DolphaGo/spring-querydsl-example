DEFAULT SETTING
```java
@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    private EntityManager em;

    private JPAQueryFactory queryFactory;

    @BeforeEach
    public void setup() {
        queryFactory = new JPAQueryFactory(em);

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
    }
}
```

Jpql
```java
@DisplayName("JPQL일 때")
@Test
void startJPQL() {
    // member1을 찾아라
    Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                          .setParameter("username", "member1")
                          .getSingleResult();

    assertEquals("member1", findMember.getUsername());
}
```

Querydsl
```java
@DisplayName("Querydsl일 때")
@Test
void startQuerydsl() {
/**
 JPAQueryFactory queryFactory = new JPAQueryFactory(em); // 대신 필드로 가져가자.
 같은 테이블을 조인하는 경우엔 alias가 달라야 하니 그때만 이렇게 선언해서 사용하면 된다.
 QMember m = new QMember("m"); // 어떤 Qmember일지 구분하는 역할.

 QType에 미리 만들어진 QMember.member를 사용할 수도 있다. 이 alias는 기본적으로 "member1"로 되어있다.
 실행된 JPQL을 확인하면 alias를 기준으로 쿼리가 작성되는 것을 확인할 수 있다.

 QMember m = QMember.member; // 대신 static import로 코드를 더 간략하고 명료하게 할 수도 있다.
 */

    Member findMember = queryFactory
            .select(member)
            .from(member)
            .where(member.username.eq("member1")) // prepareStatement로 자동으로 Parameter binding이 가능하다.
            .fetchOne();

    assertEquals("member1", findMember.getUsername());
}
```

- 차이점 분석
    - JPQL에 문법 에러가 있으면 런타임에 오류가 발생함(이거 엄청 안좋음)
    - Querydsl은 **컴파일시점**에 문법 에러가 잡힘