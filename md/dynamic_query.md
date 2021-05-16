# 동적 쿼리

## BooleanBuilder 사용
동적 쿼리를 해결하는 두가지 방식
- BooleanBuilder (Querydsl 문서에 있는 방식)
- Where 다중 파라미터 사용

```java
@DisplayName("BooleanBuilder")
@Test
void dynamicQuery_BooleanBuilder() {
    String usernameParam = "member1";
    Integer ageParam = null;

    List<Member> result = searchMember1(usernameParam, ageParam);
    assertEquals(1, result.size());
}

// 검색 조건 같은 것들 만들 때 null인 것들은 검색 쿼리에 처리하지 않도록 if 문을 추가했다.
private List<Member> searchMember1(String usernameCond, Integer ageCond) {
    BooleanBuilder builder = new BooleanBuilder();
    if (usernameCond != null) {
        builder.and(member.username.eq(usernameCond));
    }
    if (ageCond != null) {
        builder.and(member.age.eq(ageCond));
    }

    return queryFactory
            .selectFrom(member)
            .where(builder) // .and(member.age.lt(10)) 처럼 builder에도 and/or 조합이 가능하다
            .fetch();
}
```


## Where 다중 파라미터 사용
```java
@DisplayName("where로 동적 쿼리 만들기")
@Test
void dynamicQuery_where() {
    String usernameParam = "member1";
    Integer ageParam = 10;

    List<Member> result = searchMember2(usernameParam, ageParam);
    assertEquals(1, result.size());
}

private List<Member> searchMember2(String usernameCond, Integer ageCond) {
    return queryFactory
        .selectFrom(member)
        .where(usernameEq(usernameCond), ageEq(ageCond)) // where에 null이 들어가면 무시가 된다! 아무역할도 하지 않음
        .fetch();
}

private BooleanExpression usernameEq(String usernameCond) { // 그냥 Predicate말고, 조립을 위해 BooleanExpression을 사용하는 것이 좋다.
    return usernameCond != null ? member.username.eq(usernameCond) : null;
}

private BooleanExpression ageEq(Integer ageCond) {
    return ageCond != null ? member.age.eq(ageCond) : null;
}

// where 동적 쿼리 방식의 엄청난 장점 : 조합을 할 수 있게 된다.
private Predicate allEq(String usernameCond, Integer ageCond) {
    return usernameEq(usernameCond).and(ageEq(ageCond));
}
```

- where 조건에 null 값은 무시된다.
- 메서드를 다른 쿼리에서도 재활용 할 수 있다. (광고, 기간 유효 등 여러 조건들 조합 -> isServiceable)
- 쿼리 자체의 가독성이 높아진다. (쿼리 부분은 진짜 간결해진다)