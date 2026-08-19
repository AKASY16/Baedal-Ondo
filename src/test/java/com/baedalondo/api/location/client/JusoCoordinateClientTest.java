package com.baedalondo.api.location.client;

import com.baedalondo.api.location.dto.EntCoordinateResult;
import com.baedalondo.api.location.dto.JusoAddressRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 좌표 응답 검증을 확인한다.

 이 클라이언트가 좌표를 잘못 통과시키면 기상청 격자와 상권 판정이 통째로 어긋난다.
 그런데 기상청은 엉뚱한 격자에도 응답을 주기 때문에, 화면에는 아무 문제 없이
 틀린 날씨가 뜬다. 여기서 막지 못하면 뒤에서 알아챌 방법이 없다.
 **/
class JusoCoordinateClientTest {

    private static final String SEOUL_CITY_HALL_X = "953700";
    private static final String SEOUL_CITY_HALL_Y = "1952200";

    @Test
    @DisplayName("정상 응답이면 좌표를 그대로 돌려준다")
    void returnsCoordinateOnValidResponse() {
        EntCoordinateResult result = call(body(SEOUL_CITY_HALL_X, SEOUL_CITY_HALL_Y));

        assertEquals(953700.0, result.getEntX());
        assertEquals(1952200.0, result.getEntY());
    }

    @Test
    @DisplayName("entX와 entY가 없으면 거부한다")
    void rejectsMissingCoordinate() {
        // asDouble이 0.0을 돌려주므로 검증이 없으면 0.0 좌표가 그대로 흘러간다.
        String body = """
                {"results":{"common":{"errorCode":"0"},"juso":[{"admCd":"1114010300"}]}}
                """;

        assertThrows(IllegalStateException.class, () -> call(body));
    }

    @Test
    @DisplayName("좌표가 국내 범위를 벗어나면 거부한다")
    void rejectsCoordinateOutOfKorea() {
        // 0.0은 아니지만 좌표가 아니다. 이런 값도 격자로 변환되면 기상청이 응답을 준다.
        assertThrows(IllegalStateException.class, () -> call(body("1", "1")));
        assertThrows(IllegalStateException.class, () -> call(body("99999999", "99999999")));
        assertThrows(IllegalStateException.class, () -> call(body("-953700", "-1952200")));
    }

    @Test
    @DisplayName("API가 오류 코드를 주면 사유를 남기고 거부한다")
    void rejectsErrorCodeWithReason() {
        String body = """
                {"results":{"common":{"errorCode":"E0005","errorMessage":"승인되지 않은 KEY 입니다."},"juso":[]}}
                """;

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> call(body));

        // 사유가 메시지에 남아야 인증키 문제인지 주소 문제인지 구분된다.
        assertTrue(e.getMessage().contains("E0005"), e.getMessage());
    }

    @Test
    @DisplayName("juso 배열이 비면 거부한다")
    void rejectsEmptyJusoArray() {
        String body = """
                {"results":{"common":{"errorCode":"0"},"juso":[]}}
                """;

        assertThrows(IllegalStateException.class, () -> call(body));
    }

    @Test
    @DisplayName("호출 자체가 실패해도 IllegalStateException으로 감싼다")
    void wrapsTransportFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withServerError());

        JusoCoordinateClient client = new JusoCoordinateClient(builder, "test-key", "http://localhost");

        assertThrows(IllegalStateException.class, () -> client.getCoordinate(request()));
    }

    private EntCoordinateResult call(String responseBody) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(anything())
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        JusoCoordinateClient client = new JusoCoordinateClient(builder, "test-key", "http://localhost");

        return client.getCoordinate(request());
    }

    private String body(String entX, String entY) {
        return """
                {"results":{"common":{"errorCode":"0"},"juso":[{"entX":"%s","entY":"%s"}]}}
                """.formatted(entX, entY);
    }

    private JusoAddressRequest request() {
        JusoAddressRequest request = new JusoAddressRequest();
        request.setAdmCd("1114010300");
        request.setRnMgtSn("111404101021");
        request.setUdrtYn("0");
        request.setBuldMnnm("31");
        request.setBuldSlno("0");

        return request;
    }
}
