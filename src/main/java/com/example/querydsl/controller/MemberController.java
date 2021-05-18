package com.example.querydsl.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.querydsl.dto.MemberSearchCondition;
import com.example.querydsl.dto.MemberTeamDto;
import com.example.querydsl.repository.MemberJpaRepository;
import com.example.querydsl.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class MemberController {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberRepository memberRepository;

    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition) { // query string으로 받는다. ex) http://localhost:8080/v1/members?teamName=teamB&ageGoe=31&ageLoe=35
        return memberJpaRepository.search(condition);
    }

    @GetMapping("/v2/members")
    public Page<MemberTeamDto> searchMemberV2(MemberSearchCondition condition,
                                              Pageable pageable) { //스프링 데이터가 Pageable을 바로 받을 수 있게 해준다.
        return memberRepository.searchPageSimple(condition, pageable);
    }

    @GetMapping("/v3/members")//컨텐츠와 카운트 쿼리 분리
    public Page<MemberTeamDto> searchMemberV3(MemberSearchCondition condition,
                                              Pageable pageable) { //스프링 데이터가 Pageable을 바로 받을 수 있게 해준다.
        return memberRepository.searchPageComplex(condition, pageable);
    }

    @GetMapping("/v4/members")//극한의 카운트 쿼리
    public Page<MemberTeamDto> searchMemberV4(MemberSearchCondition condition,
                                              Pageable pageable) { //스프링 데이터가 Pageable을 바로 받을 수 있게 해준다.
        return memberRepository.searchPageExtremeCountQuery(condition, pageable);
    }
}
