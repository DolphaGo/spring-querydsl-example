# SQL function 호출
- SQL function은 Dialect에 등록된 내용만 호출할 수 있음.

replace 예제
```java
@DisplayName("SQL function 호출하기")
@Test
void sqlFunction() {
    List<String> result = queryFactory
            .select(Expressions.stringTemplate(
                    "function('replace', {0},{1},{2})",
                    member.username, "member", "M")) // member테이블의 username 중, "member"라는 단어를 M으로 바꿀 것이다.
            .from(member)
            .fetch();

    for (String s : result) {
        System.out.println("s = " + s);
    }
}
```

모두 소문자로 만드는 예제
```java
@DisplayName("SQL function 호출하기")
@Test
void sqlFunction2() {
    List<String> result = queryFactory
            .select(Expressions.stringTemplate(
                    "function('lower', {0})",
                    member.username)) // member테이블의 username을 모두 소문자로 바꾼다.
            .from(member)
            .fetch();

    for (String s : result) {
        System.out.println("s = " + s);
    }
}
```

```h2
select
    lower(member0_.username) as col_0_0_ 
from
    member member0_
```

그러나 위와 같이 굉장히 간단한 것들은 거의 모든 db에서 제공하는 ansi 표준적인 기능으로, querydsl에서도 제공을 하고 있다.
```java
@Test
void 표준적인기능은_querydsl에도많이있다() {
    List<String> result = queryFactory
            .select(member.username.lower())
            .from(member)
            .fetch();

    for (String s : result) {
        System.out.println("s = " + s);
    }
}
```