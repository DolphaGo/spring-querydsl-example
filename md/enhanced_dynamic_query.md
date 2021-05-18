# 동적 쿼리와 성능 최적화 조회

```java
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
```

## Builder 사용

```java
public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {

    BooleanBuilder builder = new BooleanBuilder();
    if (hasText(condition.getUsername())) {
        builder.and(member.username.eq(condition.getUsername()));
    }

    if (hasText(condition.getTeamName())) {
        builder.and(team.name.eq(condition.getTeamName()));
    }

    if (condition.getAgeGoe() != null) {
        builder.and(member.age.goe(condition.getAgeGoe()));
    }

    if (condition.getAgeLoe() != null) {
        builder.and(member.age.loe(condition.getAgeLoe()));
    }

    return queryFactory
            .select(new QMemberTeamDto(
                    member.id,
                    member.username,
                    member.age,
                    team.id,
                    team.name))
            .from(member)
            .leftJoin(member.team, team)
            .where(builder)
            .fetch();
}
```


```java
@DisplayName("조건 성능 최적화")
@Test
void searchTest() {
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

    List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
    assertThat(result).extracting("username").containsExactly("member4");
}

```

근데, 조건이 만약에 다 빠졌다면 쿼리가 어떻게 나갈까?
```java
@DisplayName("조건 성능 최적화")
@Test
void searchTest() {
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

    List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
}
```

```h2
select
    member0_.member_id as col_0_0_,
    member0_.username as col_1_0_,
    member0_.age as col_2_0_,
    team1_.team_id as col_3_0_,
    team1_.name as col_4_0_ 
from
    member member0_ 
left outer join
    team team1_ 
        on member0_.team_id=team1_.team_id
```
데이터를 다 불러오고 있다.

동적 쿼리를 짤 때는 기본 조건이 있으면 좋고, limit와 같은 페이징이 들어가야 안전하다.


## Where 절 파라미터 사용