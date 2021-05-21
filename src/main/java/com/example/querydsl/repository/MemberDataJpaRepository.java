package com.example.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.querydsl.entity.Member;

public interface MemberDataJpaRepository extends JpaRepository<Member, Long> {
}
