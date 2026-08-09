package com.baedalondo.api.store.dto;

import com.baedalondo.api.store.domain.BusinessType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 매장 등록 요청 JSON이 BusinessType으로 바인딩되는지 확인한다.
 지원하지 않는 업종은 컨트롤러에 닿기 전 역직렬화 단계에서 거부되어야 한다.
 **/
class StoreRegisterRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Enum 이름을 보내면 BusinessType으로 바인딩된다")
    void bindsEnumName() {

        StoreRegisterRequest request = objectMapper.readValue(
                """
                {"name":"온도치킨","businessType":"CHICKEN"}
                """,
                StoreRegisterRequest.class);

        assertEquals(BusinessType.CHICKEN, request.getBusinessType());
    }

    @Test
    @DisplayName("지원하지 않는 업종 이름은 요청 단계에서 거부된다")
    void rejectsUnknownEnumName() {

        assertThrows(JacksonException.class, () -> objectMapper.readValue(
                """
                {"name":"온도피자","businessType":"PIZZA_ONLY"}
                """,
                StoreRegisterRequest.class));
    }

    @Test
    @DisplayName("한글 자유 문자열은 businessType으로 바인딩되지 않는다")
    void rejectsFreeTextKorean() {

        assertThrows(JacksonException.class, () -> objectMapper.readValue(
                """
                {"name":"온도치킨","businessType":"치킨집"}
                """,
                StoreRegisterRequest.class));
    }
}
