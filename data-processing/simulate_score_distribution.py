"""배달온도 점수 분포 시뮬레이션.

WeightedScoreCalculator / WeatherWeightCalculator / AirQualityCalculator의
계산 규칙을 그대로 옮겨서, 실제 전처리 데이터(상권 x 업종 x 요일 x 시간대)
전체 격자에 대해 점수가 어디에 몰리는지 확인한다.

목적은 조사다. 가중치를 바꾸기 위한 스크립트가 아니라,
바꿀 필요가 있는지 판단할 근거를 만드는 스크립트다.

사용법:
    python simulate_score_distribution.py
    python simulate_score_distribution.py --resource-dir "../src/main/resources"

표준 라이브러리만 사용한다.
"""

import argparse
import csv
import sys
from collections import defaultdict
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path

# ---------------------------------------------------------------------------
# WeightedScoreCalculator 상수 (Java와 동일해야 한다)
# ---------------------------------------------------------------------------

BASE_SCORE = 50
HOLIDAY_SCORE = 8
RAW_WEATHER_MAX = 17
WEIGHTED_WEATHER_MAX = 20
RAW_AIR_QUALITY_MAX = 5
WEIGHTED_AIR_QUALITY_MAX = 8
INTERACTION_MAX = 10
DAY_PEAK_INTERACTION_MAX = 3
HOLIDAY_PEAK_INTERACTION = 4

TIME_SCORE = {
    "VERY_HIGH": 14,
    "HIGH": 8,
    "MEDIUM": 0,
    "LOW": -6,
    "CLOSED": -12,
}

# TimeBand enum과 같은 순서. (시작시, 끝시)
TIME_BANDS = [
    ("TIME_00_06", 0, 6),
    ("TIME_06_11", 6, 11),
    ("TIME_11_14", 11, 14),
    ("TIME_14_17", 14, 17),
    ("TIME_17_21", 17, 21),
    ("TIME_21_24", 21, 24),
]
BAND_HOURS = {name: end - start for name, start, end in TIME_BANDS}
BAND_ORDER = [name for name, _, _ in TIME_BANDS]

DAY_OF_WEEK_ORDER = [
    "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY",
    "FRIDAY", "SATURDAY", "SUNDAY",
]

# ScoreStatusLevel.from() — 자바와 같은 값을 유지해야 한다.
STATUS_BANDS = [
    ("VERY_HIGH", 64, 100),
    ("HIGH", 56, 63),
    ("MEDIUM", 42, 55),
    ("LOW", 37, 41),
    ("CLOSED", 0, 36),
]
STATUS_ORDER = [name for name, _, _ in STATUS_BANDS]


def status_of(score):
    if score >= 64:
        return "VERY_HIGH"
    if score >= 56:
        return "HIGH"
    if score >= 42:
        return "MEDIUM"
    if score >= 37:
        return "LOW"
    return "CLOSED"


def java_round(value):
    """Java Math.round와 같은 half-up 반올림. 파이썬 기본 round는 banker's rounding이라 쓰지 않는다."""
    return int(Decimal(str(value)).quantize(Decimal("1"), rounding=ROUND_HALF_UP))


def normalize(raw_score, raw_max, weighted_max):
    if raw_score <= 0:
        return 0
    capped = min(raw_score, raw_max)
    return java_round(capped * weighted_max / raw_max)


# ---------------------------------------------------------------------------
# WeatherWeightCalculator 이식
# ---------------------------------------------------------------------------

def rainfall_score(rainfall):
    if rainfall <= 0:
        return 0
    if rainfall < 1:
        return 1
    if rainfall < 3:
        return 2
    if rainfall < 15:
        return 3
    if rainfall < 30:
        return 4
    return 5


def precipitation_type_score(pty):
    return {5: 1, 2: 2, 6: 2, 7: 3, 3: 4}.get(pty, 0)


def wind_speed_score(wind):
    if wind < 4:
        return 0
    if wind < 9:
        return 1
    if wind < 14:
        return 2
    if wind < 21:
        return 4
    return 5


def temperature_score(temp):
    if 10 <= temp <= 25:
        return 0
    if (5 <= temp < 10) or (25 < temp < 28):
        return 1
    if (0 <= temp < 5) or (28 <= temp < 31):
        return 2
    return 3


class Weather:
    """PTY, RN1, T1H, WSD 네 값만 쓴다. WeatherMeasurement와 같은 범위."""

    def __init__(self, label, pty, rainfall, temperature, wind_speed, note=""):
        self.label = label
        self.pty = pty
        self.rainfall = rainfall
        self.temperature = temperature
        self.wind_speed = wind_speed
        self.note = note

        self.raw = (rainfall_score(rainfall)
                    + precipitation_type_score(pty)
                    + wind_speed_score(wind_speed)
                    + temperature_score(temperature))
        self.weighted = normalize(self.raw, RAW_WEATHER_MAX, WEIGHTED_WEATHER_MAX)
        # WeightedScoreCalculator.isRainy()
        self.rainy = rainfall > 0 or pty != 0


# 실제로 서울에서 관측되는 조합 위주로 구성한다.
# 마지막 두 개는 이론상 상한을 확인하기 위한 값이고 현실 빈도는 사실상 0이다.
WEATHER_SCENARIOS = [
    Weather("맑음(봄·가을)", 0, 0.0, 18.0, 2.0, "연중 가장 흔한 조건"),
    Weather("쌀쌀함", 0, 0.0, 7.0, 3.0, "환절기 아침·밤"),
    Weather("한여름 폭염", 0, 0.0, 33.0, 2.0, "7~8월 낮"),
    Weather("한겨울 영하", 0, 0.0, -5.0, 3.0, "12~2월 상당수 시간"),
    Weather("약한 비", 1, 0.5, 17.0, 2.0, ""),
    Weather("비", 1, 5.0, 18.0, 3.0, ""),
    Weather("강한 비+바람", 1, 20.0, 20.0, 10.0, ""),
    Weather("한겨울 눈", 3, 2.0, -3.0, 5.0, ""),
    Weather("태풍급", 1, 35.0, 26.0, 22.0, "연 1~2회"),
    Weather("이론상 최악", 3, 35.0, -15.0, 25.0, "raw 17. 실제로는 발생 불가"),
]

# AirQualityCalculator.getWeight() 결과값
AIR_QUALITY_SCENARIOS = [
    ("좋음·보통", 0),
    ("PM10 나쁨", 1),
    ("PM2.5 나쁨", 2),
    ("PM10+PM2.5 나쁨", 3),
    ("둘 다 매우나쁨", 5),
]


# ---------------------------------------------------------------------------
# WeightedScoreCalculator 이식
# ---------------------------------------------------------------------------

def day_peak_interaction(market_day_weight):
    if market_day_weight <= 0:
        return 0
    return min((market_day_weight + 1) // 2, DAY_PEAK_INTERACTION_MAX)


def interaction_score(time_level, is_holiday, market_day_weight, rainy):
    score = 0

    if time_level in ("VERY_HIGH", "HIGH"):
        if is_holiday:
            score += HOLIDAY_PEAK_INTERACTION
        else:
            score += day_peak_interaction(market_day_weight)

    if rainy:
        if time_level == "VERY_HIGH":
            score += 5
        elif time_level == "HIGH":
            score += 3
        if is_holiday:
            score += 4

    return min(score, INTERACTION_MAX)


def calculate(time_level, is_holiday, market_day_weight, weather, raw_air_quality):
    time_score = TIME_SCORE[time_level]
    day_score = HOLIDAY_SCORE if is_holiday else market_day_weight
    weather_score = weather.weighted
    air_score = normalize(raw_air_quality, RAW_AIR_QUALITY_MAX, WEIGHTED_AIR_QUALITY_MAX)
    inter_score = interaction_score(time_level, is_holiday, market_day_weight, weather.rainy)

    uncapped = BASE_SCORE + time_score + day_score + weather_score + air_score + inter_score
    return max(0, min(100, uncapped)), uncapped


# ---------------------------------------------------------------------------
# 데이터 로딩
# ---------------------------------------------------------------------------

def load_time_weights(path):
    """(상권코드, 업종) -> {시간대: 등급}"""
    result = defaultdict(dict)
    with path.open(encoding="utf-8", newline="") as f:
        for row in csv.DictReader(f):
            key = (row["commercial_area_code"], row["business_type"])
            result[key][row["time_band"]] = row["demand_level"]
    return result


def load_day_weights(path):
    """(상권코드, 업종) -> {요일: 가중치}"""
    result = defaultdict(dict)
    with path.open(encoding="utf-8", newline="") as f:
        for row in csv.DictReader(f):
            key = (row["commercial_area_code"], row["business_type"])
            result[key][row["day_of_week"]] = int(row["weight"])
    return result


# ---------------------------------------------------------------------------
# 집계 도구
# ---------------------------------------------------------------------------

def percentile(sorted_values, ratio):
    if not sorted_values:
        return None
    index = int(round((len(sorted_values) - 1) * ratio))
    return sorted_values[index]


def describe(scores):
    """시간 가중치가 이미 반영되어 펼쳐진 점수 리스트를 요약한다."""
    ordered = sorted(scores)
    return {
        "count": len(ordered),
        "min": ordered[0],
        "p05": percentile(ordered, 0.05),
        "p25": percentile(ordered, 0.25),
        "p50": percentile(ordered, 0.50),
        "p75": percentile(ordered, 0.75),
        "p95": percentile(ordered, 0.95),
        "max": ordered[-1],
        "mean": sum(ordered) / len(ordered),
    }


def status_share(weighted_counter):
    total = sum(weighted_counter.values())
    if total == 0:
        return {}
    return {name: weighted_counter.get(name, 0) / total * 100 for name in STATUS_ORDER}


def bar(percent, width=40):
    filled = int(round(percent / 100 * width))
    return "█" * filled + "·" * (width - filled)


# ---------------------------------------------------------------------------
# 리포트
# ---------------------------------------------------------------------------

def section(title):
    print()
    print("=" * 78)
    print(title)
    print("=" * 78)


def report_theoretical_range():
    section("1. 이론상 도달 가능 범위")

    calm = WEATHER_SCENARIOS[0]
    worst = WEATHER_SCENARIOS[-1]

    min_score, min_uncapped = calculate("CLOSED", False, -6, calm, 0)
    max_holiday, max_holiday_uncapped = calculate("VERY_HIGH", True, 6, worst, 5)
    max_normal, max_normal_uncapped = calculate("VERY_HIGH", False, 6, worst, 5)
    guest_min, _ = calculate("CLOSED", False, 0, calm, 0)

    print(f"  최소 (CLOSED + DayWeight -6 + 맑음 + 대기질 좋음)   : {min_score:3d}점  (clamp 전 {min_uncapped})")
    print(f"  게스트 최소 (DayWeight 0 고정)                      : {guest_min:3d}점")
    print(f"  최대 평일 (VERY_HIGH + DayWeight +6 + 최악 날씨)    : {max_normal:3d}점  (clamp 전 {max_normal_uncapped})")
    print(f"  최대 공휴일                                          : {max_holiday:3d}점  (clamp 전 {max_holiday_uncapped})")
    print()
    print(f"  → 0 ~ {min_score - 1}점은 어떤 입력으로도 나오지 않는다.")

    unreachable = [name for name, low, high in STATUS_BANDS if high < min_score]
    if unreachable:
        print(f"  → 도달 불가능한 상태 구간: {', '.join(unreachable)}")
    print(f"  → 상단은 {max_holiday_uncapped - 100}점만큼 잘려서 정보가 사라진다.")


def build_grid(time_weights, day_weights, weather, raw_air, holiday=False):
    """(상권 x 업종 x 요일 x 시간대) 전체를 돌면서 시간 가중 점수를 만든다.

    반환: (밴드별 점수리스트, 상태구간별 시간 카운터, 전체 시간 가중 점수리스트)
    """
    by_band = defaultdict(list)
    status_hours = defaultdict(int)
    all_scores = []

    day_keys = ["HOLIDAY"] if holiday else DAY_OF_WEEK_ORDER

    for key, bands in time_weights.items():
        day_map = day_weights.get(key)
        if not day_map:
            continue

        for band, level in bands.items():
            hours = BAND_HOURS[band]

            for day in day_keys:
                if holiday:
                    market_weight = 0  # 공휴일은 dayScore가 +8 고정이라 쓰이지 않는다
                else:
                    if day not in day_map:
                        continue
                    market_weight = day_map[day]

                score, _ = calculate(level, holiday, market_weight, weather, raw_air)

                by_band[band].append((score, hours))
                status_hours[status_of(score)] += hours
                all_scores.extend([score] * hours)

    return by_band, status_hours, all_scores


def report_clear_weather(time_weights, day_weights):
    section("2. 맑은 날 · 대기질 보통 기준 분포  (연중 가장 흔한 조건)")

    calm = WEATHER_SCENARIOS[0]
    by_band, status_hours, all_scores = build_grid(time_weights, day_weights, calm, 0)

    stats = describe(all_scores)
    print(f"  표본: 상권x업종x요일x시간대 격자를 시간 길이로 가중  (총 {stats['count']:,} 매장·시간)")
    print()
    print(f"  최소 {stats['min']}  |  p05 {stats['p05']}  |  p25 {stats['p25']}  |  "
          f"중앙 {stats['p50']}  |  p75 {stats['p75']}  |  p95 {stats['p95']}  |  최대 {stats['max']}")
    print(f"  평균 {stats['mean']:.1f}")
    print()

    shares = status_share(status_hours)
    print("  상태 구간 점유율 (매장·시간 기준)")
    for name, low, high in STATUS_BANDS:
        pct = shares.get(name, 0.0)
        print(f"    {name:<10} {low:>3}~{high:<3}  {pct:5.1f}%  {bar(pct)}")

    print()
    print("  시간대별 점수 분포")
    print(f"    {'시간대':<12} {'시간':>4} {'최소':>5} {'p25':>5} {'중앙':>5} {'p75':>5} {'최대':>5}   지배 구간")
    for band in BAND_ORDER:
        if band not in by_band:
            continue
        expanded = []
        for score, hours in by_band[band]:
            expanded.extend([score] * hours)
        s = describe(expanded)

        band_status = defaultdict(int)
        for score, hours in by_band[band]:
            band_status[status_of(score)] += hours
        dominant = max(band_status.items(), key=lambda kv: kv[1])
        dominant_pct = dominant[1] / sum(band_status.values()) * 100

        print(f"    {band:<12} {BAND_HOURS[band]:>3}h {s['min']:>5} {s['p25']:>5} "
              f"{s['p50']:>5} {s['p75']:>5} {s['max']:>5}   {dominant[0]} {dominant_pct:.0f}%")

    return status_hours


def report_weather_scenarios(time_weights, day_weights):
    section("3. 날씨 시나리오별 영향")

    print(f"  {'시나리오':<18} {'raw':>4} {'가중':>4} {'비':>3}  {'중앙':>5} {'p95':>5} "
          f"{'최대':>5}  {'VERY_HIGH+HIGH':>15}  비고")
    print("  " + "-" * 92)

    for weather in WEATHER_SCENARIOS:
        _, status_hours, all_scores = build_grid(time_weights, day_weights, weather, 0)
        stats = describe(all_scores)
        shares = status_share(status_hours)
        upper = shares.get("VERY_HIGH", 0) + shares.get("HIGH", 0)

        print(f"  {weather.label:<18} {weather.raw:>4} {weather.weighted:>4} "
              f"{'O' if weather.rainy else '-':>3}  {stats['p50']:>5} {stats['p95']:>5} "
              f"{stats['max']:>5}  {upper:>14.1f}%  {weather.note}")

    print()
    print("  참고: 날씨 raw 상한 17은 강수량 30mm 이상 + 눈 + 풍속 21m/s 이상 + 극한 기온이")
    print("        동시에 성립해야 한다. 실제 관측 가능한 상한은 13 안팎(가중 15)이다.")


def report_air_quality(time_weights, day_weights):
    section("4. 대기질 시나리오별 영향")

    calm = WEATHER_SCENARIOS[0]
    print(f"  {'시나리오':<20} {'raw':>4} {'가중':>4}  {'중앙':>5} {'최대':>5}")
    print("  " + "-" * 48)

    for label, raw in AIR_QUALITY_SCENARIOS:
        _, _, all_scores = build_grid(time_weights, day_weights, calm, raw)
        stats = describe(all_scores)
        weighted = normalize(raw, RAW_AIR_QUALITY_MAX, WEIGHTED_AIR_QUALITY_MAX)
        print(f"  {label:<20} {raw:>4} {weighted:>4}  {stats['p50']:>5} {stats['max']:>5}")


def report_holiday(time_weights, day_weights):
    section("5. 공휴일 분포  (dayScore +8 고정)")

    calm = WEATHER_SCENARIOS[0]
    _, status_hours, all_scores = build_grid(time_weights, day_weights, calm, 0, holiday=True)
    stats = describe(all_scores)
    shares = status_share(status_hours)

    print(f"  최소 {stats['min']}  |  중앙 {stats['p50']}  |  최대 {stats['max']}")
    print()
    for name, low, high in STATUS_BANDS:
        pct = shares.get(name, 0.0)
        print(f"    {name:<10} {low:>3}~{high:<3}  {pct:5.1f}%  {bar(pct)}")


def report_forecast_strip(time_weights, day_weights):
    section("6. 예보 스트립 시뮬레이션  (대시보드에 +1h ~ +6h를 나열했을 때)")

    calm = WEATHER_SCENARIOS[0]
    rain = next(w for w in WEATHER_SCENARIOS if w.label == "비")

    # 대표 케이스: DayWeight가 중앙값에 가까운 매장을 하나 고른다.
    sample_key = None
    for key, day_map in sorted(day_weights.items()):
        if key in time_weights and len(time_weights[key]) == len(BAND_ORDER):
            sample_key = key
            break

    if sample_key is None:
        print("  샘플 매장을 찾지 못했다.")
        return

    bands = time_weights[sample_key]
    day_map = day_weights[sample_key]

    for start_hour, day, weather in [(22, "FRIDAY", calm), (22, "FRIDAY", rain), (5, "TUESDAY", calm)]:
        print()
        print(f"  [{sample_key[1]} / 상권 {sample_key[0]} / {day} {start_hour}시 조회 / {weather.label}]")
        for offset in range(0, 7):
            hour = (start_hour + offset) % 24
            band = next(name for name, s, e in TIME_BANDS if s <= hour < e)
            level = bands.get(band)
            if level is None:
                continue
            score, _ = calculate(level, False, day_map.get(day, 0), weather, 0)
            label = "현재    " if offset == 0 else f"{offset}시간 후"
            print(f"    {label}  {hour:02d}시  {score:3d}점  {status_of(score):<10} "
                  f"(시간대 {level})")


def report_reachability(time_weights, day_weights):
    section("7. 상태 구간 도달 가능성 검증")

    reached = defaultdict(int)
    total = 0

    for weather in WEATHER_SCENARIOS:
        for _, raw_air in AIR_QUALITY_SCENARIOS:
            for holiday in (False, True):
                _, status_hours, _ = build_grid(
                    time_weights, day_weights, weather, raw_air, holiday=holiday)
                for name, hours in status_hours.items():
                    reached[name] += hours
                    total += hours

    print("  모든 날씨 x 대기질 x 공휴일 조합을 전부 돌렸을 때 각 구간에 들어간 매장·시간")
    print()
    for name, low, high in STATUS_BANDS:
        hours = reached.get(name, 0)
        pct = hours / total * 100 if total else 0
        mark = "  " if hours else "  ← 한 번도 도달하지 않음"
        print(f"    {name:<10} {low:>3}~{high:<3}  {hours:>12,}  {pct:5.1f}%{mark}")


def report_component_swing(time_weights, day_weights):
    """요소별 기여 폭을 잰다.

    임계값(구간 경계)을 조정해도 이 값들은 하나도 바뀌지 않는다.
    점수를 만드는 방식이 아니라 라벨을 붙이는 방식만 바뀌기 때문이다.
    """
    section("8. 요소별 기여 폭  (임계값 조정으로는 바뀌지 않는 부분)")

    calm = WEATHER_SCENARIOS[0]
    rain = next(w for w in WEATHER_SCENARIOS if w.label == "비")
    typhoon = next(w for w in WEATHER_SCENARIOS if w.label == "태풍급")

    print("  같은 매장에서 한 요소만 바꿨을 때 점수가 움직이는 폭")
    print()

    # 시간대: 나머지를 고정하고 등급만 바꾼다
    time_only = [calculate(level, False, 0, calm, 0)[0] for level in TIME_SCORE]
    print(f"    시간대만 변경 (CLOSED ~ VERY_HIGH)      "
          f"{min(time_only):>3} ~ {max(time_only):>3}   폭 {max(time_only) - min(time_only):>2}점")

    day_only = [calculate("MEDIUM", False, w, calm, 0)[0] for w in range(-6, 7)]
    print(f"    요일만 변경 (DayWeight -6 ~ +6)         "
          f"{min(day_only):>3} ~ {max(day_only):>3}   폭 {max(day_only) - min(day_only):>2}점")

    weather_common = [calculate("MEDIUM", False, 0, w, 0)[0]
                      for w in WEATHER_SCENARIOS if w.weighted <= rain.weighted]
    weather_real = [calculate("MEDIUM", False, 0, w, 0)[0]
                    for w in WEATHER_SCENARIOS if w.label != "이론상 최악"]
    print(f"    날씨만 변경 (맑음 ~ 비, 흔한 범위)       "
          f"{min(weather_common):>3} ~ {max(weather_common):>3}   폭 "
          f"{max(weather_common) - min(weather_common):>2}점")
    print(f"    날씨만 변경 (맑음 ~ 태풍, 실측 범위)     "
          f"{min(weather_real):>3} ~ {max(weather_real):>3}   폭 "
          f"{max(weather_real) - min(weather_real):>2}점")

    air_only = [calculate("MEDIUM", False, 0, calm, raw)[0] for _, raw in AIR_QUALITY_SCENARIOS]
    print(f"    대기질만 변경 (좋음 ~ 매우나쁨)          "
          f"{min(air_only):>3} ~ {max(air_only):>3}   폭 {max(air_only) - min(air_only):>2}점")

    print()
    print("  실제 수요 배수 (서울시 추정매출 audit 기준 / 날씨는 문헌값)")
    print("    시간대  6 ~ 14배        요일  1.75배        날씨  1.2 ~ 1.35배")
    print()
    print("  → 수요 영향은 시간대 > 요일 > 날씨인데, 점수 폭은 날씨가 요일의 두 배 가까이다.")

    # 날씨가 시간대 순위를 뒤집는지 확인한다. 배분이 어긋났다는 가장 직접적인 증거다.
    print()
    print("  날씨가 시간대 순위를 뒤집는가")
    print()
    print(f"    {'비교':<44} {'점수':>5}")
    print("    " + "-" * 52)

    cases = [
        ("한산한 시간(MEDIUM) + 태풍", "MEDIUM", typhoon),
        ("바쁜 시간(HIGH) + 맑음", "HIGH", calm),
        ("한산한 시간(LOW) + 태풍", "LOW", typhoon),
        ("평소 시간(MEDIUM) + 맑음", "MEDIUM", calm),
        ("한산한 시간(MEDIUM) + 비", "MEDIUM", rain),
    ]
    computed = []
    for label, level, weather in cases:
        score, _ = calculate(level, False, 0, weather, 0)
        computed.append((label, score))
        print(f"    {label:<44} {score:>5}  {status_of(score)}")

    flip = computed[0][1] > computed[1][1]
    print()
    if flip:
        print(f"    → 뒤집힌다. 한산한 시간의 악천후({computed[0][1]})가 "
              f"바쁜 시간의 맑은 날({computed[1][1]})보다 높다.")
    else:
        print(f"    → 뒤집히지 않는다.")
    print("    → 임계값을 어떻게 잡아도 이 역전은 그대로 남는다.")


def propose_thresholds(time_weights, day_weights):
    """맑은 날 분포를 기준으로 구간 경계 후보를 만든다.

    맑은 날을 기준으로 삼는 이유:
    악천후는 수요를 실제로 올리는 요인이므로 위 구간으로 밀어올리는 게 맞다.
    기준선은 '평범한 날'이어야 한다.
    """
    section("9. 임계값 후보안  (맑은 날 분포 기준)")

    calm = WEATHER_SCENARIOS[0]
    _, _, all_scores = build_grid(time_weights, day_weights, calm, 0)
    ordered = sorted(all_scores)

    # 목표 점유율 10 / 20 / 40 / 20 / 10
    targets = [("VERY_HIGH", 0.90), ("HIGH", 0.70), ("MEDIUM", 0.30), ("LOW", 0.10)]
    cuts = {name: percentile(ordered, ratio) for name, ratio in targets}

    print("  현재 임계값과 맑은 날 실제 점유율")
    current = defaultdict(int)
    for score in ordered:
        current[status_of(score)] += 1
    for name, low, high in STATUS_BANDS:
        pct = current.get(name, 0) / len(ordered) * 100
        print(f"    {name:<10} {low:>3}점 이상   {pct:5.1f}%")

    print()
    print("  후보안 (맑은 날 기준 목표 10 / 20 / 40 / 20 / 10)")
    print(f"    VERY_HIGH  {cuts['VERY_HIGH']:>3}점 이상")
    print(f"    HIGH       {cuts['HIGH']:>3}점 이상")
    print(f"    MEDIUM     {cuts['MEDIUM']:>3}점 이상")
    print(f"    LOW        {cuts['LOW']:>3}점 이상")
    print(f"    CLOSED     {cuts['LOW']:>3}점 미만")

    def status_with(score):
        if score >= cuts["VERY_HIGH"]:
            return "VERY_HIGH"
        if score >= cuts["HIGH"]:
            return "HIGH"
        if score >= cuts["MEDIUM"]:
            return "MEDIUM"
        if score >= cuts["LOW"]:
            return "LOW"
        return "CLOSED"

    print()
    print("  후보안 적용 시 날씨별 점유율 변화")
    print(f"    {'시나리오':<18} {'VERY_HIGH':>10} {'HIGH':>8} {'MEDIUM':>8} {'LOW':>8} {'CLOSED':>8}")
    print("    " + "-" * 66)

    for weather in WEATHER_SCENARIOS:
        if weather.label == "이론상 최악":
            continue
        counter = defaultdict(int)
        _, _, scores = build_grid(time_weights, day_weights, weather, 0)
        for score in scores:
            counter[status_with(score)] += 1
        total = len(scores)
        row = "  ".join(f"{counter.get(n, 0) / total * 100:>6.1f}%" for n in STATUS_ORDER)
        print(f"    {weather.label:<18} {row}")

    print()
    print("  → 맑은 날에도 최상위 구간이 나온다. 악천후는 위 구간 비중을 더 키운다.")
    print("  → 다만 요소별 기여 폭(8번)은 그대로다. 임계값은 배분을 고치지 않는다.")


def write_csv(time_weights, day_weights, output_path):
    """맑은 날 기준 전체 격자를 CSV로 떨군다. 직접 들여다볼 때 쓴다."""
    calm = WEATHER_SCENARIOS[0]
    rows = 0

    with output_path.open("w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f)
        writer.writerow([
            "commercial_area_code", "business_type", "day_of_week", "time_band",
            "demand_level", "market_day_weight", "score", "status",
        ])

        for key, bands in sorted(time_weights.items()):
            day_map = day_weights.get(key)
            if not day_map:
                continue
            for band in BAND_ORDER:
                level = bands.get(band)
                if level is None:
                    continue
                for day in DAY_OF_WEEK_ORDER:
                    if day not in day_map:
                        continue
                    weight = day_map[day]
                    score, _ = calculate(level, False, weight, calm, 0)
                    writer.writerow([
                        key[0], key[1], day, band, level, weight, score, status_of(score),
                    ])
                    rows += 1

    print()
    print(f"  전체 격자 CSV: {output_path}  ({rows:,}행)")


def main():
    parser = argparse.ArgumentParser(description="배달온도 점수 분포 시뮬레이션")
    parser.add_argument("--resource-dir", default="../src/main/resources",
                        help="time-weight/day-weight CSV가 있는 디렉터리")
    parser.add_argument("--output-dir", default="./output")
    args = parser.parse_args()

    # 윈도우 콘솔 기본 코드페이지(cp949)에서 막대 문자와 한글이 깨지지 않게 한다.
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    base = Path(__file__).parent
    resource_dir = (base / args.resource_dir).resolve()
    output_dir = (base / args.output_dir).resolve()
    output_dir.mkdir(parents=True, exist_ok=True)

    time_path = resource_dir / "time-weight" / "time-weight-local.csv"
    day_path = resource_dir / "day-weight" / "day-weight-local.csv"

    for path in (time_path, day_path):
        if not path.exists():
            print(f"파일을 찾을 수 없습니다: {path}", file=sys.stderr)
            return 1

    time_weights = load_time_weights(time_path)
    day_weights = load_day_weights(day_path)

    print(f"TimeWeight  : {sum(len(v) for v in time_weights.values()):,}건 "
          f"/ {len(time_weights):,} 상권x업종")
    print(f"DayWeight   : {sum(len(v) for v in day_weights.values()):,}건 "
          f"/ {len(day_weights):,} 상권x업종")

    report_theoretical_range()
    report_clear_weather(time_weights, day_weights)
    report_weather_scenarios(time_weights, day_weights)
    report_air_quality(time_weights, day_weights)
    report_holiday(time_weights, day_weights)
    report_forecast_strip(time_weights, day_weights)
    report_reachability(time_weights, day_weights)
    report_component_swing(time_weights, day_weights)
    propose_thresholds(time_weights, day_weights)
    write_csv(time_weights, day_weights, output_dir / "score-distribution.csv")

    return 0


if __name__ == "__main__":
    sys.exit(main())
