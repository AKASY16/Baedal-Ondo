package com.baedalondo.api.airquality.calculator;

import com.baedalondo.api.common.ServiceTime;
import com.baedalondo.api.airquality.domain.CurrentAirQualityObservation;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class AirQualityCalculator {

    public int getWeight(CurrentAirQualityObservation observation) {
        if (observation == null) {
            return 0;
        }

        int score = 0;

        score += calculatePm10Weight(observation.getPm10Value());
        score += calculatePm25Weight(observation.getPm25Value());

        return score;
    }

    private int calculatePm10Weight(Integer pm10Value) {
        if (pm10Value == null) {
            return 0;
        }

        if (pm10Value >= 151) {
            return 2; // 매우나쁨
        }

        if (pm10Value >= 81) {
            return 1; // 나쁨
        }

        return 0; // 좋음/보통
    }

    private int calculatePm25Weight(Integer pm25Value) {
        if (pm25Value == null) {
            return 0;
        }

        if (pm25Value >= 76) {
            return 3; // 매우나쁨
        }

        if (pm25Value >= 36) {
            return 2; // 나쁨
        }

        return 0; // 좋음/보통
    }
    public LocalDateTime getSafeAirQualityBaseTime() {
        return getSafeAirQualityBaseTime(ServiceTime.now());
    }

    public LocalDateTime getSafeAirQualityBaseTime(LocalDateTime referenceTime) {

        // 에어코리아 실시간 측정정보는 정각 측정값이 매시 15분 내외로 반영
        // 갱신 지연을 고려해 20분 전까지는 이전 정각 데이터를 기준으로 사용
        //
        // 19:00 ~ 19:19 → 18:00 반환
        // 19:20 ~ 19:59 → 19:00 반환
        // 20:00 ~ 20:19 → 19:00 반환
        // 20:20 ~ 20:59 → 20:00 반환

        LocalDateTime currentHour = referenceTime.truncatedTo(ChronoUnit.HOURS);

        if (referenceTime.getMinute() < 20) {
            return currentHour.minusHours(1);
        }

        return currentHour;
    }

}
