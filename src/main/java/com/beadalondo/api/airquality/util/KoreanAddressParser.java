package com.beadalondo.api.airquality.util;

import org.springframework.stereotype.Component;

@Component
public class KoreanAddressParser {

    public String extractSidoName(String address) {
        String[] parts = split(address);
        String sido = parts[0];

        return switch (sido) {
            case "서울특별시", "서울시", "서울" -> "서울";
            case "부산광역시", "부산시", "부산" -> "부산";
            case "대구광역시", "대구시", "대구" -> "대구";
            case "인천광역시", "인천시", "인천" -> "인천";
            case "광주광역시", "광주시", "광주" -> "광주";
            case "대전광역시", "대전시", "대전" -> "대전";
            case "울산광역시", "울산시", "울산" -> "울산";
            case "세종특별자치시", "세종시", "세종" -> "세종";
            case "경기도", "경기" -> "경기";
            case "강원특별자치도", "강원도" -> "강원";
            case "충청북도", "충북" -> "충북";
            case "충청남도", "충남" -> "충남";
            case "전북특별자치도", "전라북도", "전북" -> "전북";
            case "전라남도", "전남" -> "전남";
            case "경상북도", "경북" -> "경북";
            case "경상남도", "경남" -> "경남";
            case "제주특별자치도", "제주도", "제주" -> "제주";
            default -> throw new IllegalArgumentException("지원하지 않는 시도명입니다: " + sido);
        };
    }

    public static String extractDistrictName(String address) {
        String[] parts = split(address);

        if (parts.length < 2) {
            throw new IllegalArgumentException("주소에서 구/군 정보를 추출할 수 없습니다: " + address);
        }

        return parts[1];
    }

    private static String[] split(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("주소가 비어 있습니다.");
        }

        return address.trim().split("\\s+");
    }
}
