package com.example.querydsl;

import static com.example.querydsl.entity.QMember.member;
import static com.example.querydsl.entity.QTeam.team;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import com.example.querydsl.dto.MemberDto;
import com.example.querydsl.dto.QMemberDto;
import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.QMember;
import com.example.querydsl.entity.QTeam;
import com.example.querydsl.entity.Team;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

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

    @DisplayName("distinct test")
    @Test
    void distinct_test() {
        Team teamA = queryFactory
                .selectFrom(team)
                .where(team.name.eq("teamA"))
                .fetchOne();

        Member member2 = new Member("member2", 20, teamA);
        em.persist(member2);

        long l = queryFactory.select(member)
                             .from(member)
                             .fetchCount();
        System.out.println(l + "개 존재");

        List<Tuple> result = queryFactory.select(member.username, member.age).distinct()
                                         .from(member)
                                         .fetch();
        for (Tuple member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }// 결론 - Row가 완전히 일치하는 것들만 distinct 처리를 해준다!!

    @DisplayName("JPQL일 때")
    @Test
    void startJPQL() {
        // member1을 찾아라
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                              .setParameter("username", "member1")
                              .getSingleResult();

        assertEquals("member1", findMember.getUsername());
    }

    /**
     * JPQL에 문법 에러가 있으면 런타임에 오류가 발생함
     * Querydsl은 컴파일에 문법 에러가 잡힘
     */

    @DisplayName("Querydsl일 때")
    @Test
    void startQuerydsl() {
//        JPAQueryFactory queryFactory = new JPAQueryFactory(em); // 대신 필드로 가져가자.

//        같은 테이블을 조인하는 경우엔 alias가 달라야 하니 그때만 이렇게 선언해서 사용하면 된다.
//        QMember m = new QMember("m"); // 어떤 Qmember일지 구분하는 역할.
//
//        QType에 미리 만들어진 QMember.member를 사용할 수도 있다. 이 alias는 기본적으로 "member1"로 되어있다.
//        실행된 JPQL을 확인하면 alias를 기준으로 쿼리가 작성되는 것을 확인할 수 있다.
//
//        QMember m = QMember.member; // 대신 static import로 코드를 더 간략하고 명료하게 할 수도 있다.

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) // prepareStatement로 자동으로 Parameter binding이 가능하다.
                .fetchOne();

        assertEquals("member1", findMember.getUsername());
    }

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

        long count = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단, 2에서 회원 이름이 없으면 마지막에 null을 출력(nulls last)
     */
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
//
//        Member member5 = result.get(0);
//        Member member6 = result.get(1);
//        Member memberNull = result.get(2);
//
//        assertEquals("member5", member5.getUsername());
//        assertEquals("member6", member6.getUsername());
//        Assertions.assertNull(memberNull.getUsername());
    }

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

    @DisplayName("팀 A에 소속된 모든 회원을 찾아라")
    @Test
    void join() {
//        List<Member> result = queryFactory
//                .selectFrom(member)
//                .join(member.team, team) // inner join
//                .where(team.name.eq("teamA"))
//                .fetch();

//        assertThat(result)
//                .extracting("username")
//                .containsExactly("member1", "member2");
//
//        List<Member> result2 = queryFactory
//                .selectFrom(member)
//                .leftJoin(member.team, team) // left join
//                .where(team.name.eq("teamA"))
//                .fetch();
//
        List<Member> result3 = queryFactory
                .selectFrom(member)
                .rightJoin(member.team, team) // right join
                .where(team.name.eq("teamA"))
                .fetch();
    }

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

    /**
     * 1. 조인 대상 필터링
     * 2. 연관관계 없는 엔티티 외부 조인
     */
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
                .where(team.name.eq("teamA")) // inner join을 사용할 것이라면, on절보단 익숙한 where로 걸러라.
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

    /**
     * on을 사용해서 서로 관계가 없는 필드로 외부 조인하는 기능이 추가되었다.
     * leftJoin() 부분에 일반 조인과 다르게 엔티티 하나만 들어간다.
     * 일반 조인 : leftJoin(member.team, team)
     * on 조인 : from(member).leftJoin(team).on(xxx)
     */

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

    @PersistenceUnit
    EntityManagerFactory emf;

    /**
     * 페치 조인은 SQL에서 제공하는 기능은 아님.
     * SQL 조인을 활용해서 연관된 엔티티를 SQL 한번에 조회하는 기능.
     * 주로 성능 최적화에 사용
     */
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

    /**
     * 서브 쿼리
     * com.querydsl.jpa.JPAExpressions 사용
     */

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

    /**
     * from 절의 서브 쿼리 한계
     * JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않음
     * 당연히 Querydsl도 지원하지 않음
     * 하이버네이트 구현체를 사용하면 select 절의 서브쿼리는 지원함.
     * Querydsl도 하이버네이트 구현체를 사용하면 select 절의 서브쿼리를 지원한다.
     *
     *
     * from 절의 서브 쿼리 해결 방안
     * 1. 서브쿼리를 Join으로 변경한다. (가능할 수도, 불가능할 수도.)
     * 2. 애플리케이션에서 쿼리를 2번 분리해서 실행하기
     * 3. nativeSQL을 사용하기
     *
     * 근데 안좋은게 훨씬 많다..
     * 화면에 꽉꽉 채워서 가져오려다보니까 from절안에 From절 들어가는 경우가 은근 생긴다.
     *
     */

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

    /**
     * 문제) 다음과 같은 임의의 순서로 회원을 출력하고 싶을 때, Case문을 이용하여 querydsl로 표현해보기
     * 1. 0 ~ 30살이 아닌 회원을 가장 먼저 출력
     * 2. 0 ~ 20살 회원 출력
     * 3. 21 ~ 30살 회원 출력
     */
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

    @DisplayName("문자 더하기")
    @Test
    void concat() {
        // {username}_{age} 이렇게 만들려고 한다.
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue())) // .stringValue()가 생각보다 쓸일이 많습니다. enum같은 경우에 쓸일이 많아요.
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /////////////////////////////////////////////////////////중급/////////////////////////////////////////////////////////////

    @DisplayName("프로젝션 대상이 하나일 때")
    @Test
    void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 결과
     * s = member1
     * s = member2
     * s = member3
     * s = member4
     */

    @DisplayName("튜플로 프로젝션하기")
    @Test
    void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
        // 참고로 튜플은 com.querydsl.core 에 구현되어 있다.
        // 따라서 이 튜플을 레포지토리 계층까지는 괜찮은데
        // 리포지토리 계층을 넘어서서 서비스나 컨트롤러 계층에서 사용하는 것은 좋지 않다.
        // 그래야 나중에 하부 기술을 querydsl에서 다른 기술로 바꾸더라도 앞단을 고치지 않아도 되니까
    }

    @DisplayName("dto로 프로젝션하기 - jpql 버전")
    @Test
    void dtoProjectionByJQPL() {
        // new operation을 활용하는 방법(패키지를 다 쓰는 과정이 좀 별로다!)
        List<MemberDto> result = em.createQuery("select new com.example.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                                   .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @DisplayName("querydsl으로 프로젝션 하기 - Setter") // getter/setter로 값을 넣음 <- 실험해보니 Getter는 의미없고, Setter로 동작함
    @Test
    void dtoProjectionByQuerydslSetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class, member.username.as("name"), member.age)) // 필드명이 다르면 as로 해결. 여기서 as를 붙이지 않으면 name에 null이 출력된다.
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @DisplayName("querydsl으로 프로젝션 하기 - Field") // getter/setter 없이 값을 바로 필드에 넣음, 대신 이름이 같아야함. 필드명이 다르면 as로 해결
    @Test
    void dtoProjectionByQuerydslField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class, member.username.as("name"), member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @DisplayName("querydsl으로 프로젝션 하기 - Field") // getter/setter 없이 값을 바로 필드에 넣음, 대신 이름이 같아야함. 필드명이 다르면 as로 해결
    @Test
    void dtoProjectionByQuerydslField_ExpressionUtils() {
        QMember memberSub = new QMember("memberSub");
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                                           member.username.as("name"),
                                           ExpressionUtils.as(// ExpressionUtils를 이용하여 감싸면, 2번째 파라미터로 alias를 줄 수 있습니다.
                                                              JPAExpressions // subQuery
                                                                             .select(memberSub.age.max())
                                                                             .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @DisplayName("querydsl으로 프로젝션 하기 - Constructor")
    @Test
    void dtoProjectionByQuerydslConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @DisplayName("@QueryProjection")
    @Test
    void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age)) // 컴파일 시점에 타입이 맞지 않으면 오류를 내줍니다.
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @DisplayName("BooleanBuilder")
    @Test
    void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertEquals(1, result.size());
    }

    // 검색 조건 같은 것들 만들 때 null인 것들은 검색 쿼리에 처리하지 않도록 if 문을 추가했다.
    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder) // .and(member.age.lt(10)) 처럼 builder에도 and/or 조합이 가능하다
                .fetch();
    }

    @DisplayName("where로 동적 쿼리 만들기")
    @Test
    void dynamicQuery_where() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertEquals(1, result.size());
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond)) // where에 null이 들어가면 무시가 된다! 아무역할도 하지 않음
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) { // 그냥 Predicate말고, 조립을 위해 BooleanExpression을 사용하는 것이 좋다.
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    // where 동적 쿼리 방식의 엄청난 장점 : 조합을 할 수 있게 된다.
    private Predicate allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }
    // 예를 들어서, 광고 상태 isValid, 날짜가 특정 날짜 사이(between)에 있어야 서비스 가능
    // 이 두개의 조건이 합쳐져 있어야 isServiceable 조건을 만족하는데, 이러한 것들을 종합해서 찾아야 하는 컴포지션 상태에서 적용하기가 매우 좋다.

    
    @DisplayName("벌크 업데이트")
    @Test
//    @Commit
    void bulkUpdate() {
        //===========벌크 연산은 영속 컨텍스트 무시하고 바로 DB로 간다==============
        // member1 = 10살 => "member1"(영속성컨텍스트), "member1""(DB)
        // member2 = 20살 => "member2"(영속성컨텍스트), "member2"(DB)
        // member3 = 30살 => "member3"(영속성컨텍스트), "member3"(DB)
        // member4 = 40살 => "member4"(영속성컨텍스트), "member4"(DB)

        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        // member1 = 10살 => "member1"(영속성컨텍스트), "비회원""(DB)
        // member2 = 20살 => "member2"(영속성컨텍스트), "비회원"(DB)
        // member3 = 30살 => "member3"(영속성컨텍스트), "member3"(DB)
        // member4 = 40살 => "member4"(영속성컨텍스트), "member4"(DB)

        // DB에서 가져온 결과를 영속 컨텍스트에 넣으려고 하지만, 동일성을 위해서 유지가 된다. (Repeatable-Read)
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    @DisplayName("벌크 연산 실행 후에는 영속 켄텍스트를 초기화해주자")
    @Test
    void bulkUpdate_영속컨텍스트_DB_싱크맞추기() {
        //===========벌크 연산은 영속 컨텍스트 무시하고 바로 DB로 간다==============
        // member1 = 10살 => "member1"(영속성컨텍스트), "member1""(DB)
        // member2 = 20살 => "member2"(영속성컨텍스트), "member2"(DB)
        // member3 = 30살 => "member3"(영속성컨텍스트), "member3"(DB)
        // member4 = 40살 => "member4"(영속성컨텍스트), "member4"(DB)

        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();

        // member1 = 10살 => "member1"(영속성컨텍스트), "비회원""(DB)
        // member2 = 20살 => "member2"(영속성컨텍스트), "비회원"(DB)
        // member3 = 30살 => "member3"(영속성컨텍스트), "member3"(DB)
        // member4 = 40살 => "member4"(영속성컨텍스트), "member4"(DB)

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    @DisplayName("벌크 연산")
    @Test
    void bulkAdd() {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();

        long count2 = queryFactory
                .update(member)
                .set(member.age, member.age.add(-1))
                .execute();

        long count3 = queryFactory
                .update(member)
                .set(member.age, member.age.multiply(2))
                .execute();
    }

    @DisplayName("삭제 벌크 연산")
    @Test
    void bulkDelete() {
        long execute = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

}
