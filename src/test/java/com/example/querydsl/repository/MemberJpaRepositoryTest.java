package com.example.querydsl.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.querydsl.entity.Member;

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

}