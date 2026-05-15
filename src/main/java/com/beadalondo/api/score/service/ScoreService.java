package com.beadalondo.api.score.service;

import com.beadalondo.api.score.ScoreResult;
import com.beadalondo.api.score.calculator.DayWeightCalculator;
import com.beadalondo.api.score.status.DayDemandLevel;
import com.beadalondo.api.score.status.TimeDemandLevel;
import com.beadalondo.api.score.calculator.TimeWeightCalculator;
import com.beadalondo.api.store.domain.Store;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class ScoreService {

//    private final TimeWeightCalculator timeWeightCalculator;
//    private final WeatherCalculator weatherCalculator;
//
//    public ScoreService(TimeWeightCalculator timeWeightCalculator, WeatherCalculator weatherCalculator) {
//        this.timeWeightCalculator = timeWeightCalculator;
//        this.weatherCalculator = weatherCalculator;
//    }

    private final TimeWeightCalculator timeWeightCalculator;
    private final DayWeightCalculator dayWeightCalculator;

    public ScoreService(TimeWeightCalculator timeWeightCalculator,  DayWeightCalculator dayWeightCalculator) {
        this.timeWeightCalculator = timeWeightCalculator;
        this.dayWeightCalculator = new DayWeightCalculator();
    }


    public ScoreResult calculateCurrentScore(Store store) {
        int baseScore = 40;

        TimeDemandLevel timeDemandLevel = timeWeightCalculator.calculate(LocalTime.now());
        DayDemandLevel dayDemandLevel = dayWeightCalculator.calculate(LocalDate.now());

        int score = capScore(baseScore + timeDemandLevel.getWeight() + dayDemandLevel.getWeight());

        String status = calculateStatus(score);
        String message = createMessage(score);

        System.out.println("timeWeight = " + timeDemandLevel.getWeight());
        System.out.println("dayWeight = " + dayDemandLevel.getWeight());

        return new ScoreResult(score, status, message);
    }

    private int capScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private String calculateStatus(int score) {
        if (score >= 80) {
            return "상 · 수요 급등 구간";
        }

        if (score >= 40) {
            return "중 · 평시 운영 구간";
        }

        if (score >= 20) {
            return "하 · 수요 둔화 구간";
        }

        return "마감 · 조기 마감 검토";
    }

    private String createMessage(int score) {
        if (score >= 80) {
            return "오늘은 배달 수요가 높을 가능성이 큽니다. 연장 영업과 재료 추가 준비를 고려하세요.";
        }

        if (score >= 40) {
            return "평상시 영업을 유지하세요. 피크 시간대 주문 흐름을 지켜보세요.";
        }

        if (score >= 20) {
            return "현재 수요가 낮은 편입니다. 식자재 선조리와 인력 운영을 보수적으로 가져가세요.";
        }

        return "기대 수요가 매우 낮습니다. 유지 비용을 고려해 조기 마감을 검토하세요.";
    }
}
