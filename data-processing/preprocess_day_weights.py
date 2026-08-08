"""서울시 상권분석서비스 추정매출(2023~2025) -> 상권 x 업종 x 요일 DayWeight 전처리.

사용법:
    python preprocess_day_weights.py
    python preprocess_day_weights.py --input-dir "D:/csv" --output-dir "./output"

표준 라이브러리만 사용한다.
"""

import argparse
import calendar
import csv
import sys
from collections import defaultdict, namedtuple
from datetime import date, timedelta
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path

YEARS = (2023, 2024, 2025)
QUARTERS = (1, 2, 3, 4)

ENCODING = "cp949"

# 서울시 서비스 업종 코드 -> 배달온도 BusinessType.
# CS100009(호프-간이주점)를 비롯한 나머지 업종은 MVP 대상이 아니다.
SERVICE_CODE_TO_BUSINESS_TYPE = {
    "CS100001": "KOREAN_FOOD",
    "CS100002": "CHINESE_FOOD",
    "CS100003": "JAPANESE_FOOD",
    "CS100004": "WESTERN_FOOD",
    "CS100005": "BAKERY",
    "CS100006": "FAST_FOOD",
    "CS100007": "CHICKEN",
    "CS100008": "BUNSIK",
    "CS100010": "CAFE_BEVERAGE",
}

# 출력 정렬용 고정 순서. 배달온도 BusinessType Enum 선언 순서와 맞춘다.
BUSINESS_TYPE_ORDER = [
    "KOREAN_FOOD", "CHINESE_FOOD", "JAPANESE_FOOD", "WESTERN_FOOD",
    "CHICKEN", "FAST_FOOD", "BUNSIK", "CAFE_BEVERAGE", "BAKERY",
]

# index 0~6이 date.weekday()와 같은 순서여야 한다.
DAY_OF_WEEK_NAMES = [
    "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY",
]
DAY_COUNT_COLUMNS = [
    "월요일_매출_건수", "화요일_매출_건수", "수요일_매출_건수", "목요일_매출_건수",
    "금요일_매출_건수", "토요일_매출_건수", "일요일_매출_건수",
]

TOTAL_COUNT_COLUMN = "당월_매출_건수"

REQUIRED_COLUMNS = [
    "기준_년분기_코드", "상권_구분_코드", "상권_구분_코드_명",
    "상권_코드", "상권_코드_명",
    "서비스_업종_코드", "서비스_업종_코드_명",
    TOTAL_COUNT_COLUMN,
] + DAY_COUNT_COLUMNS

EXPECTED_QUARTER_CODES = {f"{y}{q}" for y in YEARS for q in QUARTERS}
EXPECTED_QUARTER_COUNT = len(EXPECTED_QUARTER_CODES)  # 12

# Local 값을 만들기 위한 최소 표본 기준. 3개년 총 매출건수다.
# 통계적으로 최적화한 임계값이 아니라, 표본이 지나치게 작은 조합에서
# 자체 요일 패턴을 만들지 않기 위한 최소 데이터 품질 기준이다.
# 이 기준에 미달하는 조합은 City 값으로 fallback한다.
MIN_LOCAL_TRANSACTIONS = 25_000

WEIGHT_MIN = -6
WEIGHT_MAX = 6
INDEX_PER_WEIGHT_STEP = 5

Row = namedtuple(
    "Row",
    "quarter_code area_code area_name area_kind_code service_code business_type "
    "day_counts total_count",
)


class ValidationError(Exception):
    """검증 실패. 조용히 넘어가지 않고 파이프라인을 중단시킨다."""


# ---------------------------------------------------------------- load


def find_input_files(input_dir):
    """연도별 CSV를 찾는다. 파일명 표기가 연도마다 조금씩 달라 glob으로 찾는다."""
    files = {}
    for year in YEARS:
        matches = sorted(input_dir.glob(f"*추정매출-상권*{year}*.csv"))
        if not matches:
            raise ValidationError(
                f"{year}년 CSV를 찾을 수 없습니다. 탐색 경로={input_dir}"
            )
        if len(matches) > 1:
            raise ValidationError(
                f"{year}년 CSV가 여러 개입니다: {[m.name for m in matches]}"
            )
        files[year] = matches[0]
    return files


def parse_count(value):
    """매출건수 문자열을 정수로 바꾼다. 비어 있거나 숫자가 아니면 None."""
    text = (value or "").strip()
    if not text:
        return None
    try:
        return int(text)
    except ValueError:
        return None


def load_data(input_dir):
    """3개 연도 CSV 전체를 읽는다. 업종 필터는 아직 하지 않는다."""
    files = find_input_files(input_dir)
    rows = []

    for year in YEARS:
        path = files[year]
        with path.open(encoding=ENCODING, newline="") as f:
            reader = csv.DictReader(f)

            missing = [c for c in REQUIRED_COLUMNS if c not in (reader.fieldnames or [])]
            if missing:
                raise ValidationError(f"{path.name}에 필요한 컬럼이 없습니다: {missing}")

            for record in reader:
                service_code = record["서비스_업종_코드"]
                rows.append(Row(
                    quarter_code=record["기준_년분기_코드"].strip(),
                    area_code=record["상권_코드"].strip(),
                    area_name=record["상권_코드_명"].strip(),
                    area_kind_code=record["상권_구분_코드"].strip(),
                    service_code=service_code,
                    business_type=SERVICE_CODE_TO_BUSINESS_TYPE.get(service_code),
                    day_counts=tuple(parse_count(record[c]) for c in DAY_COUNT_COLUMNS),
                    total_count=parse_count(record[TOTAL_COUNT_COLUMN]),
                ))

        print(f"  {path.name}: {sum(1 for r in rows):,}행 누적")

    return rows


def filter_business_types(rows):
    """배달온도가 지원하는 9개 업종만 남긴다."""
    return [r for r in rows if r.business_type is not None]


# ---------------------------------------------------------------- validate


def validate_data(all_rows, supported_rows):
    """검증 실패는 무엇이 문제인지 밝히고 예외로 중단시킨다."""
    problems = []

    # 1. (년분기, 상권, 업종) 조합 중복
    seen = defaultdict(int)
    for r in all_rows:
        seen[(r.quarter_code, r.area_code, r.service_code)] += 1
    duplicates = [k for k, v in seen.items() if v > 1]
    if duplicates:
        problems.append(
            f"중복 키 {len(duplicates)}건. 예시={duplicates[:5]}"
        )

    # 2. 당월_매출_건수 == 요일 7개 합계 (지원 업종 행 대상)
    mismatches = []
    for r in supported_rows:
        if r.total_count is None or any(c is None for c in r.day_counts):
            continue
        if sum(r.day_counts) != r.total_count:
            mismatches.append(
                (r.quarter_code, r.area_code, r.service_code,
                 r.total_count, sum(r.day_counts))
            )
    if mismatches:
        problems.append(
            f"당월_매출_건수 != 요일 합계 {len(mismatches)}건. 예시={mismatches[:5]}"
        )

    # 3. 기준_년분기_코드 형식
    unexpected = sorted({r.quarter_code for r in all_rows} - EXPECTED_QUARTER_CODES)
    if unexpected:
        problems.append(f"예상치 못한 기준_년분기_코드: {unexpected}")

    missing_quarters = sorted(EXPECTED_QUARTER_CODES - {r.quarter_code for r in all_rows})
    if missing_quarters:
        problems.append(f"누락된 분기: {missing_quarters}")

    # 4. 숫자 컬럼의 null / 음수 (지원 업종 행 대상)
    null_rows = [r for r in supported_rows
                 if r.total_count is None or any(c is None for c in r.day_counts)]
    if null_rows:
        problems.append(
            f"숫자 컬럼이 비어 있거나 숫자가 아닌 행 {len(null_rows)}건. "
            f"예시={[(r.quarter_code, r.area_code, r.service_code) for r in null_rows[:5]]}"
        )

    negative_rows = [r for r in supported_rows
                     if (r.total_count is not None and r.total_count < 0)
                     or any(c is not None and c < 0 for c in r.day_counts)]
    if negative_rows:
        problems.append(
            f"음수 매출건수 {len(negative_rows)}건. "
            f"예시={[(r.quarter_code, r.area_code, r.service_code) for r in negative_rows[:5]]}"
        )

    if problems:
        raise ValidationError(
            "데이터 검증 실패:\n" + "\n".join(f"  - {p}" for p in problems)
        )


# ---------------------------------------------------------------- weekday calendar


def count_weekdays_in_quarter(year, quarter):
    """해당 분기에 각 요일이 실제로 며칠 있었는지 센다. index 0=MONDAY."""
    first_month = 3 * (quarter - 1) + 1
    last_month = first_month + 2

    start = date(year, first_month, 1)
    end = date(year, last_month, calendar.monthrange(year, last_month)[1])

    counts = [0] * 7
    current = start
    while current <= end:
        counts[current.weekday()] += 1
        current += timedelta(days=1)

    return counts


def build_weekday_calendar():
    """기준_년분기_코드 -> 요일별 실제 일수."""
    return {
        f"{year}{quarter}": count_weekdays_in_quarter(year, quarter)
        for year in YEARS
        for quarter in QUARTERS
    }


# ---------------------------------------------------------------- weights


def round_half_up(value):
    """0.5는 사람이 기대하는 대로 절댓값이 큰 쪽으로 반올림한다."""
    return int(Decimal(str(value)).quantize(Decimal("1"), rounding=ROUND_HALF_UP))


def to_day_weight(day_index):
    """DayIndex -> DayWeight. (DayIndex - 100) / 5, -6 ~ +6으로 clamp."""
    raw = (day_index - 100) / INDEX_PER_WEIGHT_STEP
    return max(WEIGHT_MIN, min(WEIGHT_MAX, round_half_up(raw)))


def to_day_indexes(day_transactions, day_counts):
    """요일별 1일 평균 매출건수를 구하고 7일 평균을 100으로 정규화한다."""
    daily_averages = [
        day_transactions[i] / day_counts[i] if day_counts[i] else 0.0
        for i in range(7)
    ]
    weekly_average = sum(daily_averages) / 7

    if weekly_average <= 0:
        return None, None

    day_indexes = [avg / weekly_average * 100 for avg in daily_averages]
    return daily_averages, (weekly_average, day_indexes)


def build_local_weights(supported_rows, weekday_calendar,
                        min_transactions=MIN_LOCAL_TRANSACTIONS):
    """상권 x 업종 단위.

    12개 분기가 모두 존재하고 3개년 총 매출건수가 min_transactions 이상인
    조합만 Local 값을 만든다. 나머지는 런타임에서 City 값으로 fallback한다.
    """
    transactions = defaultdict(lambda: [0] * 7)
    weekday_days = defaultdict(lambda: [0] * 7)
    quarters = defaultdict(set)
    area_names = {}

    for r in supported_rows:
        key = (r.area_code, r.business_type)
        quarters[key].add(r.quarter_code)
        area_names[key] = r.area_name

        quarter_weekdays = weekday_calendar[r.quarter_code]
        for i in range(7):
            transactions[key][i] += r.day_counts[i]
            weekday_days[key][i] += quarter_weekdays[i]

    results = []
    skipped_incomplete = 0
    skipped_zero = 0
    skipped_below_threshold = 0

    for key in sorted(quarters, key=lambda k: (k[0], BUSINESS_TYPE_ORDER.index(k[1]))):
        quarter_count = len(quarters[key])

        if quarter_count != EXPECTED_QUARTER_COUNT:
            skipped_incomplete += 1
            continue

        total_transactions = sum(transactions[key])
        if total_transactions == 0:
            skipped_zero += 1
            continue

        if total_transactions < min_transactions:
            skipped_below_threshold += 1
            continue

        daily_averages, normalized = to_day_indexes(transactions[key], weekday_days[key])
        if normalized is None:
            skipped_zero += 1
            continue

        weekly_average, day_indexes = normalized
        area_code, business_type = key

        for i in range(7):
            results.append({
                "commercial_area_code": area_code,
                "commercial_area_name": area_names[key],
                "business_type": business_type,
                "day_of_week": DAY_OF_WEEK_NAMES[i],
                "quarter_count": quarter_count,
                "total_transactions": total_transactions,
                "weekday_transactions": transactions[key][i],
                "weekday_days": weekday_days[key][i],
                "weekday_daily_average": round(daily_averages[i], 4),
                "weekly_daily_average": round(weekly_average, 4),
                "day_index": round(day_indexes[i], 2),
                "day_weight": to_day_weight(day_indexes[i]),
            })

    skipped = skipped_incomplete + skipped_zero + skipped_below_threshold

    return results, {
        "combinations": len(quarters),
        "complete": len(quarters) - skipped,
        "skipped_incomplete": skipped_incomplete,
        "skipped_zero": skipped_zero,
        "skipped_below_threshold": skipped_below_threshold,
        "min_transactions": min_transactions,
        "city_fallback": skipped,
    }


def build_city_weights(supported_rows, weekday_calendar):
    """서울 전체 fallback. 상권별 DayIndex 평균이 아니라 원본 매출건수를 먼저 합산한다."""
    transactions = defaultdict(lambda: [0] * 7)
    quarters = defaultdict(set)

    for r in supported_rows:
        quarters[r.business_type].add(r.quarter_code)
        for i in range(7):
            transactions[r.business_type][i] += r.day_counts[i]

    results = []
    for business_type in BUSINESS_TYPE_ORDER:
        if business_type not in quarters:
            continue

        weekday_days = [
            sum(weekday_calendar[q][i] for q in quarters[business_type])
            for i in range(7)
        ]

        daily_averages, normalized = to_day_indexes(transactions[business_type], weekday_days)
        if normalized is None:
            continue

        weekly_average, day_indexes = normalized

        for i in range(7):
            results.append({
                "business_type": business_type,
                "day_of_week": DAY_OF_WEEK_NAMES[i],
                "day_index": round(day_indexes[i], 2),
                "day_weight": to_day_weight(day_indexes[i]),
                "quarter_count": len(quarters[business_type]),
                "total_transactions": sum(transactions[business_type]),
            })

    return results


# ---------------------------------------------------------------- output


def write_outputs(output_dir, local_rows, city_rows):
    output_dir.mkdir(parents=True, exist_ok=True)

    local_path = output_dir / "day-weight-local.csv"
    with local_path.open("w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["commercial_area_code", "business_type", "day_of_week", "weight"])
        for row in local_rows:
            writer.writerow([row["commercial_area_code"], row["business_type"],
                             row["day_of_week"], row["day_weight"]])

    city_path = output_dir / "day-weight-city.csv"
    with city_path.open("w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["business_type", "day_of_week", "weight"])
        for row in city_rows:
            writer.writerow([row["business_type"], row["day_of_week"], row["day_weight"]])

    audit_path = output_dir / "day-weight-audit.csv"
    audit_columns = [
        "commercial_area_code", "commercial_area_name", "business_type", "day_of_week",
        "quarter_count", "total_transactions", "weekday_transactions", "weekday_days",
        "weekday_daily_average", "weekly_daily_average", "day_index", "day_weight",
    ]
    with audit_path.open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=audit_columns)
        writer.writeheader()
        writer.writerows(local_rows)

    return local_path, city_path, audit_path


# ---------------------------------------------------------------- report


def print_report(all_rows, supported_rows, local_rows, city_rows, local_stats):
    print("\n" + "=" * 70)
    print("검증 결과")
    print("=" * 70)

    by_type = defaultdict(int)
    for r in supported_rows:
        by_type[r.business_type] += 1

    local_combos_by_type = defaultdict(set)
    for row in local_rows:
        local_combos_by_type[row["business_type"]].add(row["commercial_area_code"])

    area_codes = {r.area_code for r in supported_rows}
    weights = [row["day_weight"] for row in local_rows] + [row["day_weight"] for row in city_rows]

    print(f" 1. 원본 총 행 수                : {len(all_rows):,}")
    print(f" 2. 9개 업종 필터 후 행 수       : {len(supported_rows):,}")
    print(" 3. 업종별 행 수")
    for bt in BUSINESS_TYPE_ORDER:
        print(f"      {bt:<15} {by_type[bt]:>7,}")
    print(f" 4. 상권코드 개수                : {len(area_codes):,}")
    print(f" 5. 상권 x BusinessType 조합     : {local_stats['combinations']:,}")
    print(f" 6. Local 자격 통과 조합         : {local_stats['complete']:,}"
          f"  ({local_stats['complete'] / local_stats['combinations'] * 100:.1f}%)")
    print(f"      - 12분기 미만 제외          : {local_stats['skipped_incomplete']:,}")
    print(f"      - 매출 0 제외               : {local_stats['skipped_zero']:,}")
    print(f"      - {local_stats['min_transactions']:,}건 미만 제외      "
          f": {local_stats['skipped_below_threshold']:,}")
    print(f"      - City fallback 전환 합계   : {local_stats['city_fallback']:,}")
    print(f" 7. Local output 행 수           : {len(local_rows):,}"
          f"  (= {local_stats['complete']:,} x 7)")
    print(f" 8. City output 행 수            : {len(city_rows)}")
    print(f" 9. City가 정확히 63행인가       : {len(city_rows) == 63}")
    print(f"10. DayWeight 최소 / 최대        : {min(weights)} / {max(weights)}")
    print(f"11. -6 ~ +6 범위 밖 값           : "
          f"{sum(1 for w in weights if w < WEIGHT_MIN or w > WEIGHT_MAX)}건")

    null_count = sum(
        1 for row in local_rows + city_rows for v in row.values() if v is None or v == ""
    )
    print(f"12. 최종 output의 null 값        : {null_count}건")

    print("\n" + "=" * 70)
    print(f"BusinessType별 Local 조합 수 (threshold {local_stats['min_transactions']:,}건)")
    print("=" * 70)
    for bt in BUSINESS_TYPE_ORDER:
        print(f"  {bt:<15} {len(local_combos_by_type[bt]):>6,}")
    print(f"  {'합계':<15} {sum(len(v) for v in local_combos_by_type.values()):>6,}")

    print("\n" + "=" * 70)
    print("City DayWeight (서울 전체 fallback)")
    print("=" * 70)
    header = "BusinessType".ljust(15) + "".join(d[:3].rjust(6) for d in DAY_OF_WEEK_NAMES)
    print(header)
    print("-" * len(header))
    city_by_type = defaultdict(dict)
    for row in city_rows:
        city_by_type[row["business_type"]][row["day_of_week"]] = row
    for bt in BUSINESS_TYPE_ORDER:
        line = bt.ljust(15)
        for day in DAY_OF_WEEK_NAMES:
            line += str(city_by_type[bt][day]["day_weight"]).rjust(6)
        print(line)

    print("\nCity DayIndex (참고)")
    print("-" * len(header))
    for bt in BUSINESS_TYPE_ORDER:
        line = bt.ljust(15)
        for day in DAY_OF_WEEK_NAMES:
            line += f"{city_by_type[bt][day]['day_index']:>6.1f}"
        print(line)

    print_local_samples(local_rows)
    print_extremes(local_rows)


def print_local_samples(local_rows, sample_size=5):
    """매출건수가 가장 많은 상권 x 업종 조합을 예시로 보여준다."""
    print("\n" + "=" * 70)
    print(f"Local 예시 (매출건수 상위 {sample_size}개 상권 x 업종)")
    print("=" * 70)

    by_combo = defaultdict(list)
    for row in local_rows:
        by_combo[(row["commercial_area_code"], row["business_type"])].append(row)

    top = sorted(by_combo.items(),
                 key=lambda kv: kv[1][0]["total_transactions"],
                 reverse=True)[:sample_size]

    for (area_code, business_type), rows in top:
        rows_by_day = {r["day_of_week"]: r for r in rows}
        print(f"\n{area_code} {rows[0]['commercial_area_name']} / {business_type} "
              f"(12분기 총 {rows[0]['total_transactions']:,}건)")
        print("       " + "".join(d[:3].rjust(8) for d in DAY_OF_WEEK_NAMES))
        print("index  " + "".join(f"{rows_by_day[d]['day_index']:>8.1f}"
                                  for d in DAY_OF_WEEK_NAMES))
        print("weight " + "".join(f"{rows_by_day[d]['day_weight']:>8}"
                                  for d in DAY_OF_WEEK_NAMES))


def print_extremes(local_rows, sample_size=5):
    print("\n" + "=" * 70)
    print(f"극단적인 Local DayIndex 상위 {sample_size}건 / 하위 {sample_size}건")
    print("=" * 70)

    ordered = sorted(local_rows, key=lambda r: r["day_index"])

    print("\n[가장 낮은 요일]")
    for row in ordered[:sample_size]:
        print(f"  {row['commercial_area_code']} {row['commercial_area_name'][:18]:<18} "
              f"{row['business_type']:<14} {row['day_of_week']:<10} "
              f"index={row['day_index']:>7.1f} weight={row['day_weight']:>3} "
              f"(총 {row['total_transactions']:,}건)")

    print("\n[가장 높은 요일]")
    for row in reversed(ordered[-sample_size:]):
        print(f"  {row['commercial_area_code']} {row['commercial_area_name'][:18]:<18} "
              f"{row['business_type']:<14} {row['day_of_week']:<10} "
              f"index={row['day_index']:>7.1f} weight={row['day_weight']:>3} "
              f"(총 {row['total_transactions']:,}건)")


# ---------------------------------------------------------------- main


def main():
    script_dir = Path(__file__).resolve().parent

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input-dir", type=Path, default=script_dir / "input",
                        help="서울시 추정매출 CSV 3개가 있는 폴더")
    parser.add_argument("--output-dir", type=Path, default=script_dir / "output",
                        help="결과 CSV를 쓸 폴더")
    parser.add_argument("--min-transactions", type=int, default=MIN_LOCAL_TRANSACTIONS,
                        help="Local 값 생성에 필요한 3개년 최소 총 매출건수")
    args = parser.parse_args()

    print("[1/5] CSV 로딩")
    all_rows = load_data(args.input_dir)

    print("[2/5] 지원 업종 필터")
    supported_rows = filter_business_types(all_rows)
    print(f"  {len(all_rows):,}행 -> {len(supported_rows):,}행")

    print("[3/5] 데이터 검증")
    validate_data(all_rows, supported_rows)
    print("  통과")

    print("[4/5] DayWeight 계산")
    print(f"  Local 최소 거래량 threshold: {args.min_transactions:,}건")
    weekday_calendar = build_weekday_calendar()
    local_rows, local_stats = build_local_weights(
        supported_rows, weekday_calendar, args.min_transactions)
    # City는 threshold와 무관하게 서울 전체 원본 매출건수로 계산한다.
    city_rows = build_city_weights(supported_rows, weekday_calendar)

    print("[5/5] 결과 파일 생성")
    for path in write_outputs(args.output_dir, local_rows, city_rows):
        print(f"  {path}")

    print_report(all_rows, supported_rows, local_rows, city_rows, local_stats)


if __name__ == "__main__":
    try:
        main()
    except ValidationError as e:
        print(f"\n[FAILED] {e}", file=sys.stderr)
        sys.exit(1)
