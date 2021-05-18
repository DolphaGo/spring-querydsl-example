# Querydsl 지원 클래스 직접 만들기

스프링 데이터가 제공하는 `QuerydslRepositorySupport` 한계 극복이 목적

- 스프링 데이터가 제공하는 페이징을 편리하게 변환 페이징과 카운트 쿼리 분리 가능하도록.
- 스프링 데이터 `Sort` 지원
- `select()` , `selectFrom()` 으로 시작 가능하도록
- `EntityManager` , `QueryFactory` 제공

```java
@Repository
public abstract class Querydsl4RepositorySupport {
    private final Class domainClass;
    private Querydsl querydsl;
    private EntityManager entityManager;
    private JPAQueryFactory queryFactory;

    public Querydsl4RepositorySupport(Class<?> domainClass) {
        Assert.notNull(domainClass, "Domain class must not be null!");
        this.domainClass = domainClass;
    }

    @Autowired
    public void setEntityManager(EntityManager entityManager) {
        Assert.notNull(entityManager, "EntityManager must not be null!");
        JpaEntityInformation entityInformation = JpaEntityInformationSupport.getEntityInformation(domainClass, entityManager);
        SimpleEntityPathResolver resolver = SimpleEntityPathResolver.INSTANCE;
        EntityPath path = resolver.createPath(entityInformation.getJavaType());
        this.entityManager = entityManager;
        this.querydsl = new Querydsl(entityManager, new PathBuilder<>(path.getType(), path.getMetadata())); // querydsl에 path를 제대로 주어야 sort가 동작해서, sort 이슈를 해결하기 위한 것임
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @PostConstruct
    public void validate() {
        Assert.notNull(entityManager, "EntityManager must not be null!");
        Assert.notNull(querydsl, "Querydsl must not be null!");
        Assert.notNull(queryFactory, "QueryFactory must not be null!");
    }

    protected JPAQueryFactory getQueryFactory() {
        return queryFactory;
    }

    protected Querydsl getQuerydsl() {
        return querydsl;
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    protected <T> JPAQuery<T> select(Expression<T> expr) {
        return getQueryFactory().select(expr);
    }

    protected <T> JPAQuery<T> selectFrom(EntityPath<T> from) {
        return getQueryFactory().selectFrom(from);
    }

    protected <T> Page<T> applyPagination(Pageable pageable, Function<JPAQueryFactory, JPAQuery> contentQuery) {
        JPAQuery jpaQuery = contentQuery.apply(getQueryFactory());
        List<T> content = getQuerydsl().applyPagination(pageable, jpaQuery).fetch();
        return PageableExecutionUtils.getPage(content, pageable, jpaQuery::fetchCount);
    }

    protected <T> Page<T> applyPagination(Pageable pageable, Function<JPAQueryFactory, JPAQuery> contentQuery, Function<JPAQueryFactory, JPAQuery> countQuery) {
        JPAQuery jpaContentQuery = contentQuery.apply(getQueryFactory());
        List<T> content = getQuerydsl().applyPagination(pageable, jpaContentQuery).fetch();
        JPAQuery countResult = countQuery.apply(getQueryFactory());
        return PageableExecutionUtils.getPage(content, pageable, countResult::fetchCount);
    }
}
```

페이징 쿼리 구현

```java
public Page<Member> searchPageByApplyPage(MemberSearchCondition condition,Pageable pageable){
        JPAQuery<Member> query=selectFrom(member)
        .leftJoin(member.team,team)
        .where(usernameEq(condition.getUsername()),
        teamNameEq(condition.getTeamName()),
        ageGoe(condition.getAgeGoe()),
        ageLoe(condition.getAgeLoe())
        );

        List<Member> content=getQuerydsl().applyPagination(pageable,query).fetch();

        return PageableExecutionUtils.getPage(content,pageable,query::fetchCount);
        }
```

기존 `QuerydslRepositorySupport` 방식을 다음과 같이 람다로 깔끔하게 풀어냄

```java
public Page<Member> applyPagination(MemberSearchCondition condition,Pageable pageable){
        return applyPagination(pageable,query->query
        .selectFrom(member)
        .leftJoin(member.team,team)
        .where(usernameEq(condition.getUsername()),
        teamNameEq(condition.getTeamName()),
        ageGoe(condition.getAgeGoe()),
        ageLoe(condition.getAgeLoe())
        )
        );
        }
```

카운트 쿼리를 분리했을 때

기존 방법

```java
@Override
public Page<MemberTeamDto> searchPageComplex(final MemberSearchCondition condition,final Pageable pageable){
        List<MemberTeamDto> content=getContent(condition,pageable); // 카운트 쿼리 안가져옴(최적화를 위해)
        long total=getTotal(condition);        // select와 카운트 쿼리 분리
        return new PageImpl<>(content,pageable,total);
        }

private long getTotal(final MemberSearchCondition condition){
        long total=queryFactory
        .select(member)
        .from(member)
        .leftJoin(member.team,team)
        .where(
        usernameEq(condition.getUsername()),
        teamNameEq(condition.getTeamName()),
        ageGoe(condition.getAgeGoe()),
        ageLoe(condition.getAgeLoe())
        ).fetchCount();
        return total;
        }

private List<MemberTeamDto> getContent(final MemberSearchCondition condition,final Pageable pageable){
        return queryFactory
        .select(new QMemberTeamDto(
        member.id,
        member.username,
        member.age,
        team.id,
        team.name))
        .from(member)
        .leftJoin(member.team,team)
        .where(
        usernameEq(condition.getUsername()),
        teamNameEq(condition.getTeamName()),
        ageGoe(condition.getAgeGoe()),
        ageLoe(condition.getAgeLoe())
        )
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize()) // 한 페이지에 몇개까지?
        .fetch();
        }
```

람다를 이용하여 한 번에 처리

```java
public Page<Member> applyPagination2(MemberSearchCondition condition,Pageable pageable){
        return applyPagination(pageable,
        contentQuery->contentQuery
        .selectFrom(member)
        .leftJoin(member.team,team)
        .where(usernameEq(condition.getUsername()),
        teamNameEq(condition.getTeamName()),
        ageGoe(condition.getAgeGoe()),
        ageLoe(condition.getAgeLoe())
        ),
        countQuery->countQuery
        .select(member.id)
        .from(member)
        .leftJoin(member.team,team)
        .where(usernameEq(condition.getUsername()),
        teamNameEq(condition.getTeamName()),
        ageGoe(condition.getAgeGoe()),
        ageLoe(condition.getAgeLoe()))
        );
        }
```