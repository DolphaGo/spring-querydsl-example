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

## Count Query 최적화

- 카운트 쿼리가 생략가능한 경우 생략해서 처리
    - 페이지 시작이면서 컨텐츠 사이즈가 페이지 사이즈보다 작을 때
    - 마지막 페이지 일 때 (offset + 컨텐츠 사이즈를 더해서 전체 사이즈를 구함!)
  
```java
// 극한의 카운트 쿼리
public Page<MemberTeamDto> enhancedPagingQuery(final MemberSearchCondition condition, final Pageable pageable) {
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
            .fetch();

    JPQLQuery<Member> countQuery = queryFactory
            .select(member)
            .from(member)
            .leftJoin(member.team, team)
            .where(
                    usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe())
            );

    return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount); // 마지막 페이지에서는 카운트 쿼리를 날리지 않는다.
}
```

이 `PageableExecutionUtils.getPage()` 로 리턴하는데, 내부 코드를 보면 다음과 같다.
```java
public static <T> Page<T> getPage(List<T> content, Pageable pageable, LongSupplier totalSupplier) {

    Assert.notNull(content, "Content must not be null!");
    Assert.notNull(pageable, "Pageable must not be null!");
    Assert.notNull(totalSupplier, "TotalSupplier must not be null!");

    if (pageable.isUnpaged() || pageable.getOffset() == 0) {

        if (pageable.isUnpaged() || pageable.getPageSize() > content.size()) {
            return new PageImpl<>(content, pageable, content.size());
        }

        return new PageImpl<>(content, pageable, totalSupplier.getAsLong());
    }

    if (content.size() != 0 && pageable.getPageSize() > content.size()) {
        return new PageImpl<>(content, pageable, pageable.getOffset() + content.size());
    }

    return new PageImpl<>(content, pageable, totalSupplier.getAsLong());
}
```

1. 페이징 정보가 없거나, 데이터 시작 지점(offset=0)이라면,
  - 그 중 페이징 정보가 없거나, 페이징 정보로 가져온 개수가 한 페이지 사이즈 크기보다 작다면
    - 그대로 `content.size()`를 `total Count`로 리턴한다.
  - 그 외는 페이징 쿼리(Supplier)를 실행시켜서 Long으로 변환하여 total 개수를 반환한다.

2. 페이징 정보 중 데이터 시작 지점이 아니면서, 한 페이지 사이즈 크기보다 쿼리로 퍼올린 개수가 작다면
  - 쿼리를 직접 날리지 않고도 `offset` + `content.size()`가 곧 total 개수가 된다.

3. 그 외는 카운트 쿼리로 넘겨받은 supplier를 실행시켜 total 개수를 얻어낸다.