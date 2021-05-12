package com.example.querydsl;

import static com.example.querydsl.entity.QMember.member;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.QMember;
import com.example.querydsl.entity.Team;
import com.querydsl.core.QueryResults;
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
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//
////        // 단건 조회
//        Member fetchOne = queryFactory
//                .selectFrom(member)
//                .where(member.age.lt(15))
//                .fetchOne();
////
//        Member fetchFirst = queryFactory
//                .selectFrom(member)
//                .where(member.age.gt(100))
//                .fetchFirst();
//
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

//        long count = queryFactory
//                .selectFrom(member)
//                .fetchCount();
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
}
