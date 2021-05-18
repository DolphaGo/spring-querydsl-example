package com.example.querydsl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.querydsl.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    // method 이름으로 query : select m from Member m where m.username = :username
    List<Member> findByUsername(String username);
}
