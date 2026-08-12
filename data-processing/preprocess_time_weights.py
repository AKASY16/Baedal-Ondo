"""서울시 추정매출(2023~2025) -> 상권 x 업종 x 시간대 수요 등급 전처리.

원본의 시간대별 매출건수를 구간 길이로 나눠 시간당 건수로 보정하고,
각 상권 x 업종의 24시간 평균 시간당 건수를 100으로 둔 TimeIndex를 만든다.
"""

import argparse
import calendar
import csv
import sys
from collections import defaultdict, namedtuple
from pathlib import Path

from preprocess_day_weights import (
    BUSINESS_TYPE_ORDER,
    ENCODING,
    EXPECTED_QUARTER_CODES,
    EXPECTED_QUARTER_COUNT,
    MIN_LOCAL_TRANSACTIONS,
    SERVICE_CODE_TO_BUSINESS_TYPE,
    TOTAL_COUNT_COLUMN,
    ValidationError,
    find_input_files,
    parse_count,
)


TIME_BANDS = (
    ("TIME_00_06", "시간대_건수~06_매출_건수", 6),
    ("TIME_06_11", "시간대_건수~11_매출_건수", 5),
    ("TIME_11_14", "시간대_건수~14_매출_건수", 3),
    ("TIME_14_17", "시간대_건수~17_매출_건수", 3),
    ("TIME_17_21", "시간대_건수~21_매출_건수", 4),
    ("TIME_21_24", "시간대_건수~24_매출_건수", 3),
)

REQUIRED_COLUMNS = [
    "기준_년분기_코드",
    "상권_구분_코드",
    "상권_코드",
    "상권_코드_명",
    "서비스_업종_코드",
    TOTAL_COUNT_COLUMN,
] + [column for _, column, _ in TIME_BANDS]

# TimeIndex는 같은 상권 x 업종의 24시간 평균 시간당 활동량을 100으로 둔 상대값이다.
# 정확한 배달량 예측이 아니라 현재 시간대의 상업활동 강도를 5단계로 제한해 반영한다.
CLOSED_INDEX_MAX = 10
LOW_INDEX_MAX = 70
MEDIUM_INDEX_MAX = 130
HIGH_INDEX_MAX = 220

TimeRow = namedtuple(
    "TimeRow",
    "quarter_code area_code area_name area_kind_code business_type "
    "time_counts total_count",
)


def load_data(input_dir):
    files = find_input_files(input_dir)
    rows = []

    for year in sorted(files):
        path = files[year]
        with path.open(encoding=ENCODING, newline="") as handle:
            reader = csv.DictReader(handle)
            missing = [column for column in REQUIRED_COLUMNS
                       if column not in (reader.fieldnames or [])]
            if missing:
                raise ValidationError(f"{path.name}에 필요한 컬럼이 없습니다: {missing}")

            for record in reader:
                business_type = SERVICE_CODE_TO_BUSINESS_TYPE.get(
                    record["서비스_업종_코드"])
                if business_type is None:
                    continue

                rows.append(TimeRow(
                    quarter_code=record["기준_년분기_코드"].strip(),
                    area_code=record["상권_코드"].strip(),
                    area_name=record["상권_코드_명"].strip(),
                    area_kind_code=record["상권_구분_코드"].strip(),
                    business_type=business_type,
                    time_counts=tuple(parse_count(record[column])
                                      for _, column, _ in TIME_BANDS),
                    total_count=parse_count(record[TOTAL_COUNT_COLUMN]),
                ))

        print(f"  {path.name}: 지원 업종 누적 {len(rows):,}행")

    return rows


def validate_data(rows):
    problems = []

    unexpected = sorted({row.quarter_code for row in rows} - EXPECTED_QUARTER_CODES)
    missing_quarters = sorted(EXPECTED_QUARTER_CODES - {row.quarter_code for row in rows})
    if unexpected:
        problems.append(f"예상하지 못한 분기: {unexpected}")
    if missing_quarters:
        problems.append(f"누락된 분기: {missing_quarters}")

    null_rows = [row for row in rows
                 if row.total_count is None or any(value is None for value in row.time_counts)]
    if null_rows:
        problems.append(f"숫자 컬럼 누락/오류 {len(null_rows)}건")

    negative_rows = [row for row in rows
                     if (row.total_count is not None and row.total_count < 0)
                     or any(value is not None and value < 0 for value in row.time_counts)]
    if negative_rows:
        problems.append(f"음수 매출건수 {len(negative_rows)}건")

    mismatches = [row for row in rows
                  if row.total_count is not None
                  and all(value is not None for value in row.time_counts)
                  and sum(row.time_counts) != row.total_count]
    if mismatches:
        problems.append(f"당월 매출건수 != 시간대 합계 {len(mismatches)}건")

    seen = defaultdict(int)
    for row in rows:
        seen[(row.quarter_code, row.area_code, row.business_type)] += 1
    duplicates = [key for key, count in seen.items() if count > 1]
    if duplicates:
        problems.append(f"분기 x 상권 x 업종 중복 {len(duplicates)}건")

    if problems:
        raise ValidationError("시간대 데이터 검증 실패:\n" +
                              "\n".join(f"  - {problem}" for problem in problems))


def days_in_quarter(quarter_code):
    year = int(quarter_code[:4])
    quarter = int(quarter_code[4])
    first_month = (quarter - 1) * 3 + 1
    return sum(calendar.monthrange(year, month)[1]
               for month in range(first_month, first_month + 3))


def to_time_indexes(time_transactions, observed_days):
    total_transactions = sum(time_transactions)
    if total_transactions <= 0 or observed_days <= 0:
        return None

    average_per_hour = total_transactions / (observed_days * 24)
    hourly_averages = [
        time_transactions[index] / (observed_days * TIME_BANDS[index][2])
        for index in range(len(TIME_BANDS))
    ]
    indexes = [value / average_per_hour * 100 for value in hourly_averages]
    return average_per_hour, hourly_averages, indexes


def to_demand_level(time_index):
    if time_index < CLOSED_INDEX_MAX:
        return "CLOSED"
    if time_index < LOW_INDEX_MAX:
        return "LOW"
    if time_index < MEDIUM_INDEX_MAX:
        return "MEDIUM"
    if time_index < HIGH_INDEX_MAX:
        return "HIGH"
    return "VERY_HIGH"


def build_local_levels(rows, min_transactions=MIN_LOCAL_TRANSACTIONS):
    transactions = defaultdict(lambda: [0] * len(TIME_BANDS))
    quarters = defaultdict(set)
    area_names = {}

    for row in rows:
        key = (row.area_code, row.business_type)
        quarters[key].add(row.quarter_code)
        area_names[key] = row.area_name
        for index, value in enumerate(row.time_counts):
            transactions[key][index] += value

    results = []
    skipped_incomplete = 0
    skipped_below_threshold = 0
    skipped_zero = 0

    for key in sorted(quarters, key=lambda value: (
            value[0], BUSINESS_TYPE_ORDER.index(value[1]))):
        if len(quarters[key]) != EXPECTED_QUARTER_COUNT:
            skipped_incomplete += 1
            continue

        total_transactions = sum(transactions[key])
        if total_transactions < min_transactions:
            skipped_below_threshold += 1
            continue

        observed_days = sum(days_in_quarter(code) for code in quarters[key])
        normalized = to_time_indexes(transactions[key], observed_days)
        if normalized is None:
            skipped_zero += 1
            continue

        average_per_hour, hourly_averages, indexes = normalized
        area_code, business_type = key
        for index, (time_band, _, hours) in enumerate(TIME_BANDS):
            results.append({
                "commercial_area_code": area_code,
                "commercial_area_name": area_names[key],
                "business_type": business_type,
                "time_band": time_band,
                "quarter_count": len(quarters[key]),
                "total_transactions": total_transactions,
                "band_transactions": transactions[key][index],
                "band_hours": hours,
                "observed_days": observed_days,
                "hourly_average": round(hourly_averages[index], 4),
                "overall_hourly_average": round(average_per_hour, 4),
                "time_index": round(indexes[index], 2),
                "demand_level": to_demand_level(indexes[index]),
            })

    return results, {
        "combinations": len(quarters),
        "qualified": len({(row["commercial_area_code"], row["business_type"])
                           for row in results}),
        "skipped_incomplete": skipped_incomplete,
        "skipped_below_threshold": skipped_below_threshold,
        "skipped_zero": skipped_zero,
    }


def build_city_levels(rows):
    transactions = defaultdict(lambda: [0] * len(TIME_BANDS))
    quarters = defaultdict(set)

    for row in rows:
        quarters[row.business_type].add(row.quarter_code)
        for index, value in enumerate(row.time_counts):
            transactions[row.business_type][index] += value

    results = []
    for business_type in BUSINESS_TYPE_ORDER:
        observed_days = sum(days_in_quarter(code) for code in quarters[business_type])
        normalized = to_time_indexes(transactions[business_type], observed_days)
        if normalized is None:
            continue

        average_per_hour, hourly_averages, indexes = normalized
        for index, (time_band, _, hours) in enumerate(TIME_BANDS):
            results.append({
                "business_type": business_type,
                "time_band": time_band,
                "quarter_count": len(quarters[business_type]),
                "total_transactions": sum(transactions[business_type]),
                "band_transactions": transactions[business_type][index],
                "band_hours": hours,
                "observed_days": observed_days,
                "hourly_average": round(hourly_averages[index], 4),
                "overall_hourly_average": round(average_per_hour, 4),
                "time_index": round(indexes[index], 2),
                "demand_level": to_demand_level(indexes[index]),
            })

    return results


def write_outputs(output_dir, local_rows, city_rows):
    output_dir.mkdir(parents=True, exist_ok=True)

    local_path = output_dir / "time-weight-local.csv"
    with local_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(["commercial_area_code", "business_type", "time_band", "demand_level"])
        for row in local_rows:
            writer.writerow([row["commercial_area_code"], row["business_type"],
                             row["time_band"], row["demand_level"]])

    city_path = output_dir / "time-weight-city.csv"
    with city_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(["business_type", "time_band", "demand_level"])
        for row in city_rows:
            writer.writerow([row["business_type"], row["time_band"], row["demand_level"]])

    audit_path = output_dir / "time-weight-audit.csv"
    audit_columns = list(local_rows[0].keys())
    with audit_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=audit_columns)
        writer.writeheader()
        writer.writerows(local_rows)

    return local_path, city_path, audit_path


def print_report(local_rows, city_rows, stats):
    print("\n" + "=" * 76)
    print("TimeWeight 전처리 결과")
    print("=" * 76)
    print(f"상권 x 업종 조합: {stats['combinations']:,}")
    print(f"Local 채택 조합: {stats['qualified']:,}")
    print(f"Local 출력 행: {len(local_rows):,}")
    print(f"City 출력 행: {len(city_rows):,}")
    print(f"12분기 미달: {stats['skipped_incomplete']:,}")
    print(f"{MIN_LOCAL_TRANSACTIONS:,}건 미달: {stats['skipped_below_threshold']:,}")
    print(f"0건 조합: {stats['skipped_zero']:,}")

    city = defaultdict(dict)
    for row in city_rows:
        city[row["business_type"]][row["time_band"]] = row

    print("\nCity TimeIndex / DemandLevel")
    for business_type in BUSINESS_TYPE_ORDER:
        values = []
        for time_band, _, _ in TIME_BANDS:
            row = city[business_type][time_band]
            values.append(f"{time_band[5:]}={row['time_index']:.1f}/{row['demand_level']}")
        print(f"  {business_type:<15} " + "  ".join(values))


def main():
    script_dir = Path(__file__).resolve().parent
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input-dir", type=Path, default=script_dir / "input")
    parser.add_argument("--output-dir", type=Path, default=script_dir / "output")
    parser.add_argument("--min-transactions", type=int, default=MIN_LOCAL_TRANSACTIONS)
    args = parser.parse_args()

    print("[1/4] 원본 CSV 로딩")
    rows = load_data(args.input_dir)
    print("[2/4] 시간대 데이터 검증")
    validate_data(rows)
    print("  통과")
    print("[3/4] Local / City 시간대 등급 계산")
    local_rows, stats = build_local_levels(rows, args.min_transactions)
    city_rows = build_city_levels(rows)
    print("[4/4] 결과 파일 생성")
    for path in write_outputs(args.output_dir, local_rows, city_rows):
        print(f"  {path}")
    print_report(local_rows, city_rows, stats)


if __name__ == "__main__":
    try:
        main()
    except ValidationError as error:
        print(f"\n[FAILED] {error}", file=sys.stderr)
        sys.exit(1)
