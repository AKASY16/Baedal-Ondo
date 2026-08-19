package com.baedalondo.api.location.client;

import com.baedalondo.api.location.dto.JusoAddressRequest;
import com.baedalondo.api.location.dto.EntCoordinateResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;



@Component
public class JusoCoordinateClient {

    private final RestClient restClient;
    private final String confmKey;

    public JusoCoordinateClient(
            RestClient.Builder restClientBuilder,
            @Value("${jusogokr.api.coordinate-auth-key}") String confmKey,
            @Value("https://business.juso.go.kr/addrlink/addrCoordApi.do") String baseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
        this.confmKey = confmKey;
    }

    public EntCoordinateResult getCoordinate(JusoAddressRequest request) {

        String admCd = request.getAdmCd();
        String rnMgtSn = request.getRnMgtSn();
        String udrtYn = request.getUdrtYn();
        String buldMnnm = request.getBuldMnnm();
        String buldSlno = request.getBuldSlno();

        try {
            JsonNode root = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("confmKey", confmKey)
                            .queryParam("admCd", admCd)
                            .queryParam("rnMgtSn", rnMgtSn)
                            .queryParam("udrtYn", udrtYn)
                            .queryParam("buldMnnm", buldMnnm)
                            .queryParam("buldSlno", buldSlno)
                            .queryParam("resultType", "json")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null) {
                throw new IllegalStateException("행안부 좌표 API 응답이 비어 있습니다.");
            }

            // 오류 코드를 먼저 본다. juso 배열이 비는 것으로도 대부분 걸리지만,
            // 인증키 만료 같은 경우 사유를 알아야 원인을 빨리 찾는다.
            JsonNode common = root.path("results").path("common");
            String errorCode = common.path("errorCode").asText("");

            if (!errorCode.isBlank() && !"0".equals(errorCode)) {
                throw new IllegalStateException(
                        "행안부 좌표 API가 오류를 반환했습니다. errorCode=" + errorCode
                                + ", errorMessage=" + common.path("errorMessage").asText(""));
            }

            JsonNode jusoArray = root.path("results").path("juso");

            if (!jusoArray.isArray() || jusoArray.isEmpty()) {
                throw new IllegalStateException("행안부 좌표 API 응답에 juso 데이터가 없습니다.");
            }

            JsonNode firstJuso = jusoArray.get(0);

            // 필드가 없거나 숫자로 읽히지 않으면 asDouble이 0.0을 준다.
            double entX = firstJuso.path("entX").asDouble();
            double entY = firstJuso.path("entY").asDouble();

            if (entX == 0.0 || entY == 0.0) {
                throw new IllegalStateException("행안부 좌표 API 응답에 좌표 데이터가 없습니다.");
            }

            if (isOutOfKorea(entX, entY)) {
                throw new IllegalStateException(
                        "행안부 좌표 API 응답이 국내 좌표 범위를 벗어났습니다. entX=" + entX + ", entY=" + entY);
            }

            return new EntCoordinateResult(entX, entY);

        } catch (IllegalStateException e) {
            // 위에서 사유를 구분해 던진 예외는 그대로 올린다.
            // 아래 catch로 묶으면 메시지가 "호출 중 오류"로 덮여 사유가 사라진다.
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("행안부 좌표제공 API 호출 중 오류가 발생했습니다.", e);
        }
    }

    /**
     EPSG:5179는 원점이 127.5도 E / 38도 N이고 false easting 100만, false northing 200만이다.
     남한은 대략 동경 124.5~132도, 북위 32~38.7도 안에 들어오므로 아래 범위를 벗어나면
     좌표가 아니라 잘못된 값이다.

     0.0만 막으면 1.0이나 99999999 같은 값이 그대로 통과해 엉뚱한 격자로 변환되고,
     기상청은 그 격자에도 응답을 주기 때문에 조용히 틀린 날씨를 보게 된다.
     경계는 실제 주소를 거부하지 않도록 넉넉하게 잡았다.
     */
    private boolean isOutOfKorea(double entX, double entY) {
        return entX < 600_000 || entX > 1_500_000
                || entY < 1_250_000 || entY > 2_150_000;
    }
}