# 수정, 삭제 배치 쿼리

## 쿼리 한번으로 대량 데이터를 수정해야 할 때
- 더티체킹은 건건이임
- 쿼리 한방에 여러개의 업데이트를 쳐야하는 상황일 때
```java
@DisplayName("벌크 업데이트")
@Test
void bulkUpdate() {
    //===========벌크 연산은 영속 컨텍스트 무시하고 바로 DB로 간다==============
    // member1 = 10살 => "member1"(영속성컨텍스트), "member1""(DB)
    // member2 = 20살 => "member2"(영속성컨텍스트), "member2"(DB)
    // member3 = 30살 => "member3"(영속성컨텍스트), "member3"(DB)
    // member4 = 40살 => "member4"(영속성컨텍스트), "member4"(DB)

    long count = queryFactory
            .update(member)
            .set(member.username, "비회원")
            .where(member.age.lt(28))
            .execute();

    // member1 = 10살 => "member1"(영속성컨텍스트), "비회원""(DB)
    // member2 = 20살 => "member2"(영속성컨텍스트), "비회원"(DB)
    // member3 = 30살 => "member3"(영속성컨텍스트), "member3"(DB)
    // member4 = 40살 => "member4"(영속성컨텍스트), "member4"(DB)

    // DB에서 가져온 결과를 영속 컨텍스트에 넣으려고 하지만, 동일성을 위해서 유지가 된다. (Repeatable-Read)
    List<Member> result = queryFactory
            .selectFrom(member)
            .fetch();
    for (Member member1 : result) {
        System.out.println("member1 = " + member1);
    }
}
```
- 그래서 싱크를 맞추기 위해서는 영속성 컨텍스트를 한 번 비워주는 작업이 필요하다
    - `em.flush();` `em.clear();`

## 벌크 연산
```java
@DisplayName("벌크 연산")
@Test
void bulkAdd() {
    long count = queryFactory
            .update(member)
            .set(member.age, member.age.add(1)) // 더하기
            .execute();

    long count2 = queryFactory
            .update(member)
            .set(member.age, member.age.add(-1)) // 빼기
            .execute();

    long count3 = queryFactory
            .update(member)
            .set(member.age, member.age.multiply(2)) // 곱하기
            .execute();
}
```


## 삭제 벌크 연산
```java
@DisplayName("삭제 벌크 연산")
@Test
void bulkDelete() {
    long execute = queryFactory
            .delete(member)
            .where(member.age.gt(18))
            .execute();
}
```

> 주의: JPQL 배치와 마찬가지로, 영속성 컨텍스트에 있는 엔티티를 무시하고 실행되기 때문에 배치 쿼리를 실행하고 나면 영속성 컨텍스트를 초기화 하는 것이 안전하다.