package com.baedalondo.api.weather.domain;

/**
 * 기상청 격자 좌표 한 쌍.
 *
 * 날씨 캐시는 매장이 아니라 격자 단위로 재사용되므로, 사전 적재 대상도 매장 목록이 아니라
 * 중복을 제거한 격자 목록이다. 같은 건물에 매장이 여러 개여도 호출은 한 번이다.
 */
public record WeatherGrid(Integer nx, Integer ny) {
}
