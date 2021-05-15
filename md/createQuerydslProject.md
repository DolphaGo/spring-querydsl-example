## gradle 전체 설정
- Java 11
- SpringBootVersion : 2.4.4
- SpringDependencyManagement : 1.0.11

```groovy
plugins {
    id 'org.springframework.boot' version '2.4.4'
    id 'io.spring.dependency-management' version '1.0.11.RELEASE'
    id 'java'
    //querydsl plugin 추가
    id "com.ewerk.gradle.plugins.querydsl" version "1.0.10"
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '11'

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    //querydsl library 추가
    implementation 'com.querydsl:querydsl-jpa'

    compileOnly 'org.projectlombok:lombok'
    runtimeOnly 'com.h2database:h2'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

test {
    useJUnitPlatform()
}

// querydsl 추가 시작
def querydslDir = "$buildDir/generated/querydsl"

querydsl{
    jpa = true
    querydslSourcesDir = querydslDir
}

sourceSets{
    main.java.srcDir querydslDir
}

configurations {
    querydsl.extendsFrom compileClasspath
}

compileQuerydsl{
    options.annotationProcessorPath = configurations.querydsl
}
// querydsl 추가 끝
```

> IntelliJ Gradle 대신 Java로 바로 실행하기
- 최근 IntelliJ 버전은 Gradle로 실행하는 것이 기본설정 -> 느림
- 자바로 바로 실행하여 속도를 좀 더 빠르게 할 수 있다!
    1. Build and run using: Gradle IntelliJ IDEA
    2. Run tests using: Gradle IntelliJ IDEA
    

> Q타입 생성하기

**Gradle IntelliJ 사용법**
- Gradle > Tasks > build > clean
- Gradle > Tasks > other > compileQuerydsl


**Gradle 콘솔 사용법**
- `./gradlew clean compileQuerydsl`
- build > generated > querydsl > study.querydsl.entity.QHello.java 파일이 생성되어 있어야한다.

> 참고: Q타입은 컴파일 시점에 **자동 생성**되므로 버전관리(GIT)에 포함하지 않는 것이 좋다. 앞서 설정에서 생성 위치를 gradle build 폴더 아래 생성되도록 했기 때문에 이 부분도 자연스럽게 해결된다. (대부분 gradle build 폴더를 git에 포함하지 않는다.)
