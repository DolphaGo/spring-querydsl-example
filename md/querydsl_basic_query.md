# Querydsl 기본 쿼리

## 검색

#### 자주 사용하는 검색 쿼리 모음
```java
member.username.eq("member1") // username = 'member1'
member.username.ne("member1") //username != 'member1'
member.username.eq("member1").not() // username != 'member1'

member.username.isNotNull() //이름이 is not null
member.age.in(10, 20) // age in (10,20)
member.age.notIn(10, 20) // age not in (10, 20)
member.age.between(10,30) //between 10, 30

member.age.goe(30) // age >= 30
member.age.gt(30) // age > 30
member.age.loe(30) // age <= 30
member.age.lt(30) // age < 30

member.username.like("member%") // like 검색, 원하는 곳에 %를 입력해주면 됨
member.username.contains("member") // like ‘%member%’ 검색 
member.username.startsWith("member") //like ‘member%’ 검색
```


#### 기본 검색 쿼리
```java
@DisplayName("검색 쿼리")
@Test
void search() {
    Member findMember = queryFactory
    .selectFrom(member)
    .where(member.username.eq("member1")
    .and(member.age.eq(10))) // chaining으로 and/or 등으로 검색 조건을 쭉쭉 이어나갈 수 있다.
    .fetchOne();

    assertEquals("member1", findMember.getUsername());
    assertEquals(10, findMember.getAge());
}
```
- 검색조건은 `.and()`,`.or()`로 메서드체인 방식으로 연결할 수 있다!!
- `selectFrom` : `select` , `from` 을 하나로 합친 형태



#### 파라미터를 넘겨주는 쿼리
```java
@Test
void searchAndParam() {
    Member findMember = queryFactory
            .selectFrom(member)
            .where(
                    member.username.eq("member1"),
                    //                        member.age.eq(10),// ,로 파라미터식으로 여러개로 넘겨도 and로 인식한다.
                    null, // 조건 중 null이 있으면 무시함(동적쿼리에서 아주아주 강력한 기능을 자랑한다.)
                    member.age.in(10, 20, 30, 40))
            .fetchOne();

    assertEquals("member1", findMember.getUsername());
    assertEquals(10, findMember.getAge());
}
```
- `where()`에 파라미터로 검색조건을 추가하면 `AND` 조건이 추가된다.
- 이 경우 `null`값은 무시한다!
  - 메서드추출을 활용해서 동적쿼리를 깔끔하게 만들수있다.
    
- 결과 조회
    - fetch() : 리스트 조회, 데이터 없으면 빈 리스트 반환 
    - fetchOne()
        - 단 건 조회, 결과가 없으면 : null 
        - 결과가 둘 이상이면 : com.querydsl.core.NonUniqueResultException 
    - fetchFirst() : limit(1).fetchOne()
    - fetchResults() : 페이징 정보 포함, total count 쿼리 추가 실행 
    - fetchCount() : count 쿼리로 변경해서 count 수 조회

```java
@DisplayName("resultFetch")
@Test
void resultFetch() {
    // 리스트 조회
    List<Member> fetch = queryFactory
            .selectFrom(member)
            .fetch();

    // 단건 조회
    Member fetchOne = queryFactory
            .selectFrom(member)
            .where(member.age.lt(15))
            .fetchOne();

    // 처음 한 건 조회
    Member fetchFirst = queryFactory
            .selectFrom(member)
            .where(member.age.gt(100))
            .fetchFirst();

    // 페이징
    QueryResults<Member> results = queryFactory
            .selectFrom(member)
            .offset(1)
            .limit(3)
            .fetchResults();
    long total = results.getTotal();
    long offset = results.getOffset();
    long limit = results.getLimit();
    List<Member> content = results.getResults();
    content.forEach(System.out::println);

    // 카운트 쿼리
    long count = queryFactory
            .selectFrom(member)
            .fetchCount();
}
```

## 정렬
- desc() , asc() : 일반 정렬
- nullsLast() , nullsFirst() : null 데이터 순서 부여

```java

    @DisplayName("정렬")
    @Test
    void sort() {
        em.persist(new Member(null, 99));
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 99));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.between(99, 100))
                .orderBy(member.username.asc().nullsLast(), member.age.desc())
                .fetch();

        System.out.println(result.toString());
    }
```

분석을 해보자
1. 99세~100세 사이의 member 중
2. 이름으로 오름차순으로 정렬을 하자
    - 그 중, 이름이 null인 것은 맨 뒤로 빼자.
    - 이러한 것들이 여러개라면, 나이를 내림차순으로 정렬한다.
3. 따라서 위의 결과로 정렬된 순서는 다음과 같다.
```java
1. Member(id=10, username=member5, age=100) 
2. Member(id=9, username=member5, age=99)
3. Member(id=11, username=member6, age=100)
4. Member(id=8, username=null, age=100)
5. Member(id=7, username=null, age=99)
```


## 페이징

조회 건수를 제한할 때
```java
@DisplayName("페이징")
@Test
void paging1() {
    List<Member> result = queryFactory
            .selectFrom(member)
            .orderBy(member.username.desc())
            .offset(0) // 0부터 시작임
            .limit(2)
            .fetch();

    assertEquals(2, result.size());
}
```

전체 조회가 필요할 때
```java
@DisplayName("전체 조회수가 필요할 때")
@Test
void paging2() {
    QueryResults<Member> queryResults = queryFactory
            .selectFrom(member)
            .orderBy(member.username.desc())
            .offset(1) // 0부터 시작임
            .limit(2)
            .fetchResults();

    assertEquals(4, queryResults.getTotal()); // 쿼리가 복잡해지면 count query를 분리해라.
    assertEquals(2, queryResults.getLimit());
    assertEquals(1, queryResults.getOffset());
    assertEquals(2, queryResults.getResults().size()); // content
}
```
- 주의할 점이 있다. count 쿼리가 실행되니 성능상 주의한다!
> 참고: 실무에서 페이징 쿼리를 작성 시, 데이터를 조회하는 쿼리는 여러 테이블을 조인해야 하지만, count 쿼리는 조인이 필요 없는 경우도 있다.
> 그런데 이렇게 자동화된 count 쿼리는 원본 쿼리와 같이 모두 조인을 해버리기 때문에 성능이 안나올 수 있다. 
> count 쿼리에 조인이 필요없는 성능 최적화가 필요하다면, count 전용 쿼리를 별도로 작성해야 한다.


## 집합
- JPQL이 제공하는 모든 집합 함수를 제공한다. 
- tuple은 프로젝션과 결과반환에 사용한다.

```java
@DisplayName("집합")
@Test
void aggregation() {
    List<Tuple> result = queryFactory
            .select(
                    member.count(),
                    member.age.sum(),
                    member.age.avg(),
                    member.age.max(),
                    member.age.min()
            ).from(member)
            .fetch(); // querydsl이 제공하는 tuple형을 반환한다.

    // 데이터 타입이 각각 다름 -> 튜플 사용. 실무에서는 DTO로 뽑아서 사용.
    Tuple tuple = result.get(0);
    assertEquals(4, tuple.get(member.count())); // 튜플 사용법 : select에 적은 표현식 그대로
    assertEquals(100, tuple.get(member.age.sum()));
    assertEquals(25, tuple.get(member.age.avg()));
    assertEquals(40, tuple.get(member.age.max()));
    assertEquals(10, tuple.get(member.age.min()));
}
```

## groupBy

```java
@DisplayName("팀의 이름과 각 팀의 평균 연령을 구해라")
@Test
void groupby() {
    List<Tuple> result = queryFactory
            .select(team.name, member.age.avg())
            .from(member)
            .join(member.team, team)
            .groupBy(team.name)
            .fetch();

    Tuple teamA = result.get(0);
    Tuple teamB = result.get(1);

    assertEquals("teamA", teamA.get(team.name));
    assertEquals(15, teamA.get(member.age.avg()));

    assertEquals("teamB", teamB.get(team.name));
    assertEquals(35, teamB.get(member.age.avg()));
}
```
분석해보자
- `member.team`과 `team` 의 연관관계를 가지고 조인을 시도한다.
- `join`은 inner join이므로, 연관관계가 없는 것들은 매핑되지 않는다.
- 연관관계가 묶은 것들은 `groupBy`로 팀의 이름으로 그룹을 짓는다.
- 팀의 이름과 그룹 내 멤버의 나이 평균을 출력한다.

이때 나가는 쿼리는 다음과 같다.
```h2
select
    team1_.name as col_0_0_, avg(cast(member0_.age as double)) as col_1_0_ 
from
    member member0_
inner join
    team team1_ on member0_.team_id=team1_.team_id
group by
    team1_.name
```
- groupBy , 그룹화된 결과를 제한하려면 having을 사용하자.

## having

```java
@DisplayName("각 팀의 평균 연령이 20살이 넘는 팀을 구해라")
@Test
void having() {
    List<Tuple> result = queryFactory
            .select(team.name, member.age.avg())
            .from(member)
            .join(member.team, team)
            .groupBy(team.name)
            .having(member.age.avg().gt(20))
            .fetch();

    Tuple resultTeam = result.get(0);

    assertEquals("teamB", resultTeam.get(team.name));
    assertEquals(35, resultTeam.get(member.age.avg()));
}
```
분석해보자
- `member.team`과 `team`의 연관관계로 조인을 한다.
- 이때, inner join이다.
- 팀의 이름으로 그룹을 생성한다.
- 이렇게 만든 팀 그룹에서 멤버 나이의 평균이 20살보다 큰 그룹만을 걸러낸다.
- 그 중 팀의 이름과 멤버 나이의 평균을 가져온다.

실행되는 쿼리는 다음과 같다.
```h2
    select
        team1_.name as col_0_0_,
        avg(cast(member0_.age as double)) as col_1_0_ 
    from
        member member0_ 
    inner join
        team team1_ 
            on member0_.team_id=team1_.team_id 
    group by
        team1_.name 
    having
        avg(cast(member0_.age as double))>?
```


## join

- join() , innerJoin() : 내부 조인(inner join)
- leftJoin() : left 외부 조인(left outer join)
- rightJoin() : rigth 외부 조인(rigth outer join)
- JPQL의 on과 성능 최적화를 위한 fetch 조인 제공한다.


### 기본 조인
- 조인의 기본 문법은 첫 번째 파라미터에 조인 대상을 지정하고, 두 번째 파라미터에 별칭(alias)으로 사용할 Q 타입을 지정하면 된다.

> Inner join
```java
List<Member> result = queryFactory
        .selectFrom(member)
        .join(member.team, team) // inner join
        .where(team.name.eq("teamA"))
        .fetch();
```
실행된 쿼리는 다음과 같다.
```h2
select
    member0_.member_id as member_i1_1_,
    member0_.age as age2_1_,
    member0_.team_id as team_id4_1_,
    member0_.username as username3_1_ 
from
    member member0_ 
inner join
    team team1_ 
        on member0_.team_id=team1_.team_id 
where
    team1_.name=?
```

---
### OuterJoin

> leftJoin
```java
List<Member> result2 = queryFactory
        .selectFrom(member)
        .leftJoin(member.team, team) // left join
        .where(team.name.eq("teamA"))
        .fetch();
```
실행된 쿼리
```h2
select
    member0_.member_id as member_i1_1_,
    member0_.age as age2_1_,
    member0_.team_id as team_id4_1_,
    member0_.username as username3_1_ 
from
    member member0_ 
left outer join
    team team1_ 
        on member0_.team_id=team1_.team_id 
where
    team1_.name=?
```

> rightJoin
```java
List<Member> result3 = queryFactory
        .selectFrom(member)
        .rightJoin(member.team, team) // right join
        .where(team.name.eq("teamA"))
        .fetch();
}
```
실행된 쿼리
```h2
select
    member0_.member_id as member_i1_1_,
    member0_.age as age2_1_,
    member0_.team_id as team_id4_1_,
    member0_.username as username3_1_ 
from
    member member0_ 
right outer join
    team team1_ 
        on member0_.team_id=team1_.team_id 
where
    team1_.name=?
```



## theta join
연관관계가 없는 필드로 조인하는 것을 말한다.
```java
/**
 * 세타 조인
 * 회원의 이름이 팀 이름과 같은 회원 조회 (억지성 예제이긴 하지만, 진짜 연관관계가 없는 것들)
 */
@DisplayName("연관관계가 없어도 조인이 가능한 것")
@Test
void theta_join() {
    /**
     * from 절에 여러 엔티티를 선택해서 세타 조인을 하는 방
     * 단점 : 외부 조인(outer join)은 불가능하다.
     * 그런데 최신 버전으로 넘어오면서, on을 이용하면 외부 조인 가능
     */
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));

    // 물론 내부적으로 DB가 성능 최적화를 하겠지만요..
    List<Member> result = queryFactory
            .select(member)
            .from(member, team)
            .where(member.username.eq(team.name))
            .fetch();

    assertThat(result)
            .extracting("username")
            .contains("teamA", "teamB");
}
```
- from 절에 여러 엔티티를 선택해서 theta join
- 기본적으로는 외부 조인이 불가능하다.
- 그러나 `on`을 사용하면 외부 조인을 가능하게 할 수 있다.

## join `on` 으로 필터링하기
JPA 2.1부터 지원

`on` 으로 할 수 있는 일
1. 조인 대상 필터링 <- 이건 `where`로도 같은 결과를 낼 수 있다.
2. 연관관계가 없는 엔티티의 외부조인 <- 이 기능으로 많이 사용한다.

```java
@DisplayName("On절 - 조인대상 필터링")
@Test
void join_on_filtering() {
    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL : select m, t, from Member m left join m.team t on t.name = 'teamA'
     */
    List<Tuple> result = queryFactory
            .select(member, team)
            .from(member)
            .join(member.team, team)
//                .on(team.name.eq("teamA"))
            .where(team.name.eq("teamA")) // inner join을 사용할 것이라면, on절보단 익숙한 where로 걸러라. 결과는 똑같다.
            .fetch();

    for (Tuple tuple : result) {
        System.out.println("tuple = " + tuple);
    }

    List<Tuple> result2 = queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(member.team, team)
            .on(team.name.eq("teamA")) // 외부 조인은 on으로 해결하자
            .fetch();

    for (Tuple tuple : result2) {
        System.out.println("tuple = " + tuple);
    }
}
```

> 참고: on 절을 활용해 조인 대상을 필터링 할 때, 외부조인이 아니라 내부조인(inner join)을 사용하면, where 절에서 필터링 하는 것과 기능이 동일하다. 따라서 on 절을 활용한 조인 대상 필터링을 사용할 때, 내부조인 이면 익숙한 where 절로 해결하고, 정말 외부조인이 필요한 경우에만 이 기능을 사용하자.


## 연관관계 없어도 조인이 가능하다(막조인)
```java
/**
 * 연관관계 없는 엔티티 외부 조인
 * 회원의 이름이 팀 이름과 같은 대상 외부 조인
 */
@DisplayName("연관관계가 없어도 조인이 가능한 것")
@Test
void join_on_no_relation() {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));
    em.persist(new Member("teamC"));

    List<Tuple> result = queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(team)
            .on(member.username.eq(team.name))
            .fetch();

    for (Tuple tuple : result) {
        System.out.println("tuple = " + tuple);
    }
}
```
- `on`을 사용해서 서로 관계가 없는 필드로 외부 조인하는 기능이 추가되었다.
- leftJoin() 부분에 일반 조인과 다르게 엔티티 하나만 들어간다.
    - 일반 조인 : leftJoin(member.team, team)
    - on 조인 : from(member).leftJoin(team).on(xxx)


```java
@DisplayName("연관관계가 있을 때의 Simple 조인 쿼리")
@Test
void simple_join() {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));
    em.persist(new Member("teamC"));

    List<Tuple> result = queryFactory
            .select(member, team)
            .from(member)
            .join(member.team, team) // inner join
            .fetch();

    for (Tuple tuple : result) {
        System.out.println("tuple = " + tuple);
    }
}

```

## fetch join
* 페치 조인은 SQL에서 제공하는 기능은 아님.
* SQL 조인을 활용해서 연관된 엔티티를 SQL 한번에 조회하는 기능.
* 주로 성능 최적화에 사용
- 사용방법
    - join(), leftJoin() 등 조인 기능 뒤에 fetchJoin() 이라고 추가하면 된다.


```java
@PersistenceUnit
EntityManagerFactory emf;

@DisplayName("페치 조인이 없을 때")
@Test
void no_fetch_join() {
    /**
     * 페치 조인시에는 영속성 컨텍스트를 제대로 비워주지 않으면 결과를 제대로 보기 어려움
     * 그래서 비우고 ㄱ ㄱ
     */
    em.flush();
    em.clear();

    /**
     * Member와 Team은 현재 Lazy
     */
    Member findMember = queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1"))
            .fetchOne(); // Team은 조회하지 않음

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); // emf에서 team이 영속성 컨텍스트에 로딩되었는지 판단할 수 있음
    assertFalse(loaded, "페치 조인 미적용");
}
```

```java
@DisplayName("페치 조인이 있을 때")
@Test
void fetch_join() {
    /**
     * 페치 조인시에는 영속성 컨텍스트를 제대로 비워주지 않으면 결과를 제대로 보기 어려움
     * 그래서 비우고 ㄱ ㄱ
     */
    em.flush();
    em.clear();

    /**
     * Member와 Team은 현재 Lazy
     */
    Member findMember = queryFactory
            .selectFrom(member)
            .join(member.team, team).fetchJoin()
            .where(member.username.eq("member1"))
            .fetchOne(); // Team은 조회하지 않음

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); // emf에서 team이 영속성 컨텍스트에 로딩되었는지 판단할 수 있음
    assertTrue(loaded, "페치 조인 적용");
}
```


## subQuery
```java
/**
 * 서브 쿼리
 * com.querydsl.jpa.JPAExpressions 사용
 */

@DisplayName("나이가 가장 많은 회원 조회")
@Test
void subQuery_eq() {
    /**
     * 서브 쿼리이기 때문에 바깥의 member와 겹치면 안됨
     * 그래서 QMember를 직접 생성하여 alias를 다르게 합니다.
     */
    QMember memberSub = new QMember("memberSub");

    List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.eq(
                    JPAExpressions
                            .select(memberSub.age.max())
                            .from(memberSub)
            )).fetch();

    assertThat(result).extracting("age")
                      .containsExactly(40);
}
```

## subQuery2
```java
@DisplayName("나이가 가장 평균 이상인 회원 조회")
@Test
void subQuery_goe() {
    /**
     * 서브 쿼리이기 때문에 바깥의 member와 겹치면 안됨
     * 그래서 QMember를 직접 생성하여 alias를 다르게 합니다.
     */
    QMember memberSub = new QMember("memberSub");

    List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.goe(
                    JPAExpressions
                            .select(memberSub.age.avg())
                            .from(memberSub)
            )).fetch();

    assertThat(result).extracting("age")
                      .containsExactly(30, 40);
}

@DisplayName("나이가 10살 보다 많은 회원 조회")
@Test
void subQuery_in() {
    /**
     * 서브 쿼리이기 때문에 바깥의 member와 겹치면 안됨
     * 그래서 QMember를 직접 생성하여 alias를 다르게 합니다.
     */
    QMember memberSub = new QMember("memberSub");

    List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.in(
                    JPAExpressions
                            .select(memberSub.age)
                            .from(memberSub)
                            .where(memberSub.age.gt(10))
            )).fetch();

    assertThat(result).extracting("age")
                      .containsExactly(20, 30, 40);
}

@DisplayName("select 절에도 서브쿼리가 됩니다.")
@Test
void selectSubQuery() {
    /**
     * 서브 쿼리이기 때문에 바깥의 member와 겹치면 안됨
     * 그래서 QMember를 직접 생성하여 alias를 다르게 합니다.
     */
    QMember memberSub = new QMember("memberSub");

    List<Tuple> result = queryFactory
            .select(member.username,
                    JPAExpressions
                            .select(memberSub.age.avg())
                            .from(memberSub))
            .from(member)
            .fetch();

    for (Tuple tuple : result) {
        System.out.println("tuple = " + tuple);
    }
}
```
### from 절의 서브 쿼리 한계
- JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않음
- 당연히 Querydsl도 지원하지 않음
- 하이버네이트 구현체를 사용하면 select 절의 서브쿼리는 지원함.
- Querydsl도 하이버네이트 구현체를 사용하면 select 절의 서브쿼리를 지원한다.


### from 절의 서브 쿼리 해결 방안
1. 서브쿼리를 Join으로 변경한다. (가능할 수도, 불가능할 수도.)
2. 애플리케이션에서 쿼리를 2번 분리해서 실행하기
3. nativeSQL을 사용하기

- from 절에 서브쿼리를 사용하는 것은 안좋은 영향이 훨씬 많다..
- 화면에 꽉꽉 채워서 가져오려다보니까 from절안에 From절 들어가는 경우가 은근 생긴다.
- 그러니 잘 고려해보도록

## Case
- select, 조건절(where), order by에서 사용 가능하다.

### SimpleCase 문
```java
@DisplayName("케이스 문")
@Test
void basic_case() {
    List<Tuple> result = queryFactory
            .select(member.username, member.age
                    .when(10).then("열살")
                    .when(20).then("스무살")
                    .otherwise("늙어쪙"))
            .from(member)
            .fetch();

    for (Tuple s : result) {
        System.out.println("Tuple = " + s);
    }
}
```

### 복잡한 Case문
- CaseBuilder()를 사용한다.

```java
@DisplayName("복잡한 Case --> CaseBuilder()를 사용한다.")
@Test
void complexCase() {
    List<String> result = queryFactory
            .select(new CaseBuilder()
                            .when(member.age.between(0, 20)).then("0~20살")
                            .when(member.age.between(21, 30)).then("21~30살")
                            .otherwise("기타"))
            .from(member)
            .fetch();

    for (String s : result) {
        System.out.println("s = " + s);
    }
    // 근데 가급적이면 DB에서 이런 처리를 하지 않음
    // 최소한의 필터링은 DB에서 하지만, 위와 같이 값을 전환하는 경우는 애플리케이션단에서 처리하는 것을 권장한다.
}
```


문제) 다음과 같은 임의의 순서로 회원을 출력하고 싶을 때, Case문을 이용하여 querydsl로 표현해보기
1. 0 ~ 30살이 아닌 회원을 가장 먼저 출력
2. 0 ~ 20살 회원 출력
3. 21 ~ 30살 회원 출력
```java
@DisplayName("Case 활용 예제")
@Test
void caseProblem() {
    NumberExpression<Integer> rankPath = new CaseBuilder()
            .when(member.age.notBetween(0, 30)).then(1)
            .when(member.age.between(0, 20)).then(2)
            .otherwise(3);

    List<Tuple> result = queryFactory
            .select(member.username, member.age, rankPath)
            .from(member)
            .orderBy(rankPath.asc())
            .fetch();

    for (Tuple tuple : result) {
        String username = tuple.get(member.username);
        Integer age = tuple.get(member.age);

        Integer rank = tuple.get(rankPath);
        System.out.println("username = " + username + " age = " + age + " rank = " + rank);
    }
}
```
실행된 쿼리
```h2
select
    member0_.username as col_0_0_,
    member0_.age as col_1_0_,
    case 
        when member0_.age not between 0 and 30 then 1 
        when member0_.age between 0 and 20 then 2 
        else 3 
    end as col_2_0_ 
from
    member member0_ 
order by
    case 
        when member0_.age not between 0 and 30 then 1 
        when member0_.age between 0 and 20 then 2 
        else 3 
    end asc
```
- Querydsl은 자바 코드로 작성하기 때문에 rankPath 처럼 복잡한 조건을 변수로 선언해서 select 절, orderBy 절에서 함께 사용할 수 있다.



## 상수와 문자
### 상수 추가하기
- 상수가 필요하면 `Expressions.constant(xxx)` 사용한다.

```java
@DisplayName("상수 추가")
@Test
void constant() {
    List<Tuple> result = queryFactory
            .select(member.username, Expressions.constant("A")) // JPQL로는 상수 쿼리가 안나감, 결과로만 나감
            .from(member)
            .fetch();

    for (Tuple tuple : result) {
        System.out.println("tuple = " + tuple);
    }
}
```
실행된 쿼리
```h2
select
    member0_.username as col_0_0_ 
from
    member member0_
```
> 참고: 위와 같이 최적화가 가능하면 SQL에 constant 값을 넘기지 않는다. 상수를 더하는 것 처럼 최적화가 어려우면 SQL에 constant 값을 넘긴다.


### 문자 추가하기
```java
@DisplayName("문자 더하기")
@Test
void concat() {
    // {username}_{age} 이렇게 만들려고 한다.
    List<String> result = queryFactory
            .select(member.username.concat("_").concat(member.age.stringValue())) // .stringValue()가 생각보다 쓸일이 많습니다. enum같은 경우에 쓸일이 많아요.
            .from(member)
//                .where(member.username.eq("member1"))
            .fetch();

    for (String s : result) {
        System.out.println("s = " + s);
    }
}
```
실행된 쿼리
```h2
select
    ((member0_.username||?)||cast(member0_.age as char)) as col_0_0_ 
from
    member member0_ 
where
    member0_.username=?
```

> 참고: member.age.stringValue() 부분이 중요한데, 문자가 아닌 다른 타입들은 stringValue() 로
문자로 변환할 수 있다. 이 방법은 ENUM을 처리할 때도 자주 사용한다