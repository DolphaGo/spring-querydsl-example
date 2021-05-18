package com.example.querydsl.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.querydsl.dto.MemberSearchCondition;
import com.example.querydsl.dto.MemberTeamDto;
import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.Team;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

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

    @DisplayName("where절 동적 쿼리")
    @Test
    void searchTest_where절() {
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

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeGoe(40);
        condition.setTeamName("teamB");

        List<MemberTeamDto> result = memberJpaRepository.search(condition);
        assertThat(result).extracting("username").containsExactly("member4");
    }

}