# 스프링 데이터 페이징 활용


- 스프링에서 제공해주는 PageImpl로 리턴

````java
public PageImpl(List<T> content, Pageable pageable, long total) {

    super(content, pageable);

    this.total = pageable.toOptional().filter(it -> !content.isEmpty())//
            .filter(it -> it.getOffset() + it.getPageSize() > total)//
            .map(it -> it.getOffset() + content.size())//
            .orElse(total);
}
````


```java
@DisplayName("페이징 simple 테스트")
@Test
void searchPageSimple() {
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
    PageRequest pageRequest = PageRequest.of(0, 3); // 페이지가 0일땐 offset 안나가니까

    Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition, pageRequest);

    assertEquals(3, result.getSize());
    assertThat(result.getContent()).extracting("username").containsExactly("member1", "member2", "member3");
}
```


페이징에서 content와 count query의 분리
- 가끔 카운트 쿼리를 쉽게 구할 수 있음에도, join이 무식하게 나갈 수도 있음
- 그럴 때, 카운트 쿼리를 분리해서 최적화를 기대할 수 있음
```java
@Override
public Page<MemberTeamDto> searchPageComplex(final MemberSearchCondition condition, final Pageable pageable) {
    List<MemberTeamDto> content = queryFactory
            .select(new QMemberTeamDto(
                    member.id,
                    member.username,
                    member.age,
                    team.id,
                    team.name))
            .from(member)
            .leftJoin(member.team, team)
            .where(
                    usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe())
            )
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize()) // 한 페이지에 몇개까지?
            .fetch(); // 카운트 쿼리 안가져옴(최적화를 위해)

    // select와 카운트 쿼리 분리
    long total = queryFactory
            .select(member)
            .from(member)
            .leftJoin(member.team, team)
            .where(
                    usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe())
            ).fetchCount();

    return new PageImpl<>(content, pageable, total);
}

```