package com.example.querydsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.querydsl.entity.Hello;
import com.example.querydsl.entity.QHello;
import com.querydsl.jpa.impl.JPAQueryFactory;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

    //@Autowired : 스프링 최신 버전에서 쓸 때
    @PersistenceContext // Java 표준
    EntityManager em;

    @DisplayName("queryDSL Setting Test")
    @Test
    void contextLoads() {
        Hello hello = new Hello();
        em.persist(hello);

        JPAQueryFactory query = new JPAQueryFactory(em);
//        QHello qHello = new QHello("h"); // 'h' : alias를 넣어줌
        QHello qHello = QHello.hello; //이렇게도 가능하다.

        Hello result = query
                .selectFrom(qHello)
                .fetchOne();

        assertEquals(hello, result);
        assertEquals(hello.getId(), result.getId());
    }

}
