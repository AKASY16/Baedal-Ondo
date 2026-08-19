package com.baedalondo.api;

import com.baedalondo.api.support.MySqlTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 컨텍스트가 뜨면 다음이 모두 성립한 것이다.

 - Flyway 마이그레이션 V1부터 끝까지 실제 MySQL에서 실행됨
 - 그 결과 스키마와 엔티티 매핑이 일치함 (ddl-auto: validate)
 - 모든 빈이 문제없이 조립됨

 마이그레이션을 새로 추가할 때 이 테스트가 첫 관문이다.
 CD로 자동 배포하면 마이그레이션이 도는 걸 아무도 보지 않으므로,
 CI에서 실패해야 운영 DB에 적용되기 전에 막힌다.
 **/
@SpringBootTest
class BaedalOndoApiApplicationTests extends MySqlTestSupport {

    @Test
    @DisplayName("마이그레이션이 적용된 스키마 위에서 컨텍스트가 뜬다")
    void contextLoads() {
    }
}
