package com.baedalondo.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 SQL 로깅이 운영에 새지 않는지 확인한다.

 application.yaml은 프로필을 지정하지 않았을 때의 값이라 곧 운영 설정이다.
 로컬에서 쿼리를 보려고 여기에 show-sql을 켜 두면 서버에도 그대로 적용된다.
 실제로 그렇게 되어 있었고 배포 전 필수 항목으로 잡혀 있던 문제다.

 설정 파일을 직접 읽는다. 테스트 클래스패스가 main보다 앞서므로
 스프링에 올려서는 운영 설정을 볼 수 없다.
 **/
class SqlLoggingProfileTest {

    @Test
    @DisplayName("운영 기본 설정에는 SQL 로깅이 없다")
    void keepsSqlLoggingOutOfDefaultConfig() {
        String config = read("application.yaml");

        assertFalse(config.contains("show-sql: true"),
                "application.yaml에 show-sql이 켜져 있다. 로컬용이면 application-local.yaml로 옮길 것");
        assertFalse(config.contains("org.hibernate.SQL:"),
                "application.yaml에 Hibernate SQL 로그 레벨이 있다. 로컬용이면 application-local.yaml로 옮길 것");
    }

    @Test
    @DisplayName("로컬 프로필에는 SQL 로깅이 켜져 있다")
    void enablesSqlLoggingInLocalProfile() {
        String config = read("application-local.yaml");

        assertTrue(config.contains("show-sql: true"));
        assertTrue(config.contains("org.hibernate.SQL: debug"));
        // 파라미터 바인딩까지 있어야 물음표 자리의 값을 볼 수 있다.
        assertTrue(config.contains("org.hibernate.orm.jdbc.bind: trace"));
    }

    private String read(String name) {
        Path path = Path.of("src", "main", "resources", name);

        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    name + "을 읽지 못했습니다. path=" + path.toAbsolutePath(), e);
        }
    }
}
