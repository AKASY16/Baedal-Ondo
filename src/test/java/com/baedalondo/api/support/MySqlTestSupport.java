package com.baedalondo.api.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 DB를 쓰는 테스트가 상속한다. 운영과 같은 MySQL에서 마이그레이션을 실제로 실행한다.

 컨테이너를 static으로 두고 직접 start한다. @TestConfiguration 빈으로 두면 스프링 컨텍스트마다
 컨테이너가 하나씩 뜨는데, 이 프로젝트는 @SpringBootTest와 @DataJpaTest가 서로 다른 컨텍스트라
 그렇게 하면 컨테이너가 여러 개 뜬다. static이면 테스트 JVM 전체에서 하나만 쓴다.

 종료는 Testcontainers의 Ryuk 컨테이너가 맡는다. JVM이 죽으면 같이 정리된다.

 문자셋을 docker-compose와 맞춘다. 기본 latin1로 뜨면 한글이 들어간 테스트가
 실제 운영과 다르게 동작한다.
 **/
public abstract class MySqlTestSupport {

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("baedalondo")
            .withCommand(
                    "--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_unicode_ci"
            );

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}
