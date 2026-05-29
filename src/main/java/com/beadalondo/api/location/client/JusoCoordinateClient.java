package com.beadalondo.api.location.client;

import com.beadalondo.api.location.dto.JusoAddressRequest;
import com.beadalondo.api.location.dto.EntCoordinateResult;
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

            JsonNode jusoArray = root.path("results").path("juso");

            if (!jusoArray.isArray() || jusoArray.isEmpty()) {
                throw new IllegalStateException("행안부 좌표 API 응답에 juso 데이터가 없습니다.");
            }

            JsonNode firstJuso = jusoArray.get(0);

            double entX = firstJuso.path("entX").asDouble();
            double entY = firstJuso.path("entY").asDouble();


            return new EntCoordinateResult(entX, entY);

        } catch (Exception e) {
            throw new IllegalStateException("행안부 좌표제공 API 호출 중 오류가 발생했습니다.", e);
        }
    }
}