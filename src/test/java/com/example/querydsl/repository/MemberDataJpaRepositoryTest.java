package com.example.querydsl.repository;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.querydsl.entity.Member;

@Transactional
@DataJpaTest
public class MemberDataJpaRepositoryTest {

    @Autowired
    private MemberDataJpaRepository repository;

    @PersistenceContext
    private EntityManager em;

    @DisplayName("query 2번? => Id로 검색하는 것 제외는 모두 JPQL로 나간다.")
    @Test
    void query() {
        for (int i = 0; i < 3; i++) {
            repository.save(new Member("member" + i));
        }
        em.flush();
        em.clear();

        System.out.println("=======first=======");
        repository.findAll();
        System.out.println("=======second=======");
        repository.findAll();
    }
}
