package com.baedalondo.api.location.client;

import com.baedalondo.api.location.dto.JusoAddressRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
public class JusoAddressSearchClient {

    private final RestClient restClient;
    private final String confmKey;

    public JusoAddressSearchClient(
            RestClient.Builder restClientBuilder,
            @Value("${jusogokr.api.search-auth-key:${jusogokr.api.popup-auth-key:TESTJUSOGOKR}}") String confmKey,
            @Value("${jusogokr.api.search-base-url:https://business.juso.go.kr/addrlink/addrLinkApi.do}") String baseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
        this.confmKey = confmKey;
    }

    public JusoAddressRequest searchFirst(String keyword) {
        try {
            JsonNode root = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("confmKey", confmKey)
                            .queryParam("currentPage", 1)
                            .queryParam("countPerPage", 10)
                            .queryParam("keyword", keyword)
                            .queryParam("resultType", "json")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null) {
                throw new IllegalStateException("도로명주소 검색 API 응답이 비어 있습니다.");
            }

            JsonNode common = root.path("results").path("common");
            String errorCode = common.path("errorCode").asString();
            if (!"0".equals(errorCode)) {
                String errorMessage = common.path("errorMessage").asString("");
                throw new IllegalStateException("도로명주소 검색 API 오류: " + errorMessage);
            }

            JsonNode jusoArray = root.path("results").path("juso");
            if (!jusoArray.isArray() || jusoArray.isEmpty()) {
                throw new IllegalStateException("도로명주소 검색 결과가 없습니다. keyword=" + keyword);
            }

            return toRequest(jusoArray.get(0));
        } catch (RuntimeException e) {
            throw new IllegalStateException("도로명주소 검색 API 호출 중 오류가 발생했습니다. keyword=" + keyword, e);
        }
    }

    private JusoAddressRequest toRequest(JsonNode juso) {
        JusoAddressRequest request = new JusoAddressRequest();

        request.setRoadFullAddr(text(juso, "roadAddr"));
        request.setRoadAddrPart1(text(juso, "roadAddrPart1"));
        request.setRoadAddrPart2(text(juso, "roadAddrPart2"));
        request.setAddrDetail("");
        request.setJibunAddr(text(juso, "jibunAddr"));
        request.setZipNo(text(juso, "zipNo"));
        request.setSiNm(text(juso, "siNm"));
        request.setSggNm(text(juso, "sggNm"));
        request.setEmdNm(text(juso, "emdNm"));
        request.setAdmCd(text(juso, "admCd"));
        request.setRnMgtSn(text(juso, "rnMgtSn"));
        request.setBdMgtSn(text(juso, "bdMgtSn"));
        request.setRn(text(juso, "rn"));
        request.setUdrtYn(text(juso, "udrtYn"));
        request.setBuldMnnm(text(juso, "buldMnnm"));
        request.setBuldSlno(text(juso, "buldSlno"));

        return request;
    }

    private String text(JsonNode node, String fieldName) {
        return node.path(fieldName).asString("");
    }
}
