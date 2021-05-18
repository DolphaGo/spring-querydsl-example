package com.example.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberTeamDto {
    private Long memberId;
    private String username;
    private int age;
    private Long teamId;
    private String teamName;

    @QueryProjection // 단점 : DTO가 순수하진 않고, Querydsl에 종속적임. 이게 싫으면 Projection.constructor, bean, field 방식 고!
    public MemberTeamDto(final Long memberId, final String username, final int age, final Long teamId, final String teamName) {
        this.memberId = memberId;
        this.username = username;
        this.age = age;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}
