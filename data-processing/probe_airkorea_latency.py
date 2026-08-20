"""에어코리아 응답 시간 측정.

대시보드에 뜨던 타임아웃이 우리 read timeout이 짧아서인지, 아니면 상대가
실제로 실패하는 것인지 가리려고 만들었다.

같은 요청을 반복해서 보내고 응답 시간을 그대로 늘어놓는다.
평균만 보면 이분화된 분포가 뭉개지므로 개별 값을 전부 출력한다.

두 번째로, 실제 코드와 같은 조건(read timeout 4초)에서 재시도 1회가
성공률을 얼마나 바꾸는지 잰다.

사용법:
    python probe_airkorea_latency.py
    python probe_airkorea_latency.py --count 30 --sido 부산
    python probe_airkorea_latency.py --retry-trial 15

인증키는 저장소 루트의 .env에서 DATAPORTAL_AUTH_KEY를 읽는다.
환경변수로 넘겨도 된다. 출력에는 키를 찍지 않는다.

에어코리아 개발계정은 일 500건이므로 --count를 크게 잡지 말 것.

표준 라이브러리만 사용한다.
"""

import argparse
import json
import os
import statistics
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

BASE_URL = "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty"

# RestClientConfig와 같은 값. 이 컷에서 무엇이 살아남는지 보려는 것이다.
SERVICE_READ_TIMEOUT = 4.0

# 측정을 방해받지 않으려고 넉넉히 준다. 상대가 언제 포기하는지 보려면
# 우리가 먼저 끊으면 안 된다.
PROBE_TIMEOUT = 30.0


def load_auth_key(env_path):
    key = os.environ.get("DATAPORTAL_AUTH_KEY")
    if key:
        return key

    if not env_path.exists():
        sys.exit(
            f".env를 찾지 못했습니다: {env_path}\n"
            "DATAPORTAL_AUTH_KEY를 환경변수로 넘기거나 --env-file로 경로를 지정하세요."
        )

    for line in env_path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line.startswith("DATAPORTAL_AUTH_KEY="):
            return line.split("=", 1)[1].strip().strip('"').strip("'")

    sys.exit(f"{env_path}에 DATAPORTAL_AUTH_KEY가 없습니다.")


def call(auth_key, sido, timeout):
    """한 번 호출하고 (걸린 시간 ms, 결과 표시) 를 돌려준다."""
    query = urllib.parse.urlencode({
        "sidoName": sido,
        "pageNo": 1,
        "numOfRows": 100,
        "returnType": "json",
        "serviceKey": auth_key,
        "ver": "1.5",
    })

    started = time.perf_counter()

    try:
        with urllib.request.urlopen(urllib.request.Request(f"{BASE_URL}?{query}"),
                                    timeout=timeout) as response:
            body = response.read()

        elapsed = (time.perf_counter() - started) * 1000

        try:
            payload = json.loads(body)
            result_code = payload["response"]["header"]["resultCode"]
            items = len(payload["response"]["body"]["items"])
        except (ValueError, KeyError, TypeError):
            return elapsed, "응답을 해석할 수 없음", 0

        if result_code != "00":
            return elapsed, f"resultCode={result_code}", 0

        return elapsed, "성공", items

    except urllib.error.HTTPError as error:
        # 게이트웨이가 자기 타임아웃까지 붙잡고 있다가 던지는 것이 여기로 온다.
        return (time.perf_counter() - started) * 1000, f"HTTP {error.code}", 0

    except Exception as error:  # 타임아웃과 연결 실패
        name = type(error).__name__
        label = "읽기 타임아웃" if "timeout" in name.lower() else name
        return (time.perf_counter() - started) * 1000, label, 0


def summarize(label, samples):
    if not samples:
        print(f"  {label} 0건")
        return

    print(f"  {label} {len(samples):2d}건   "
          f"최소 {min(samples):7.0f}   중앙 {statistics.median(samples):7.0f}   "
          f"최대 {max(samples):7.0f} ms")


def probe_distribution(auth_key, sido, count):
    """응답 시간이 어떻게 흩어지는지 본다. 우리 타임아웃 없이 끝까지 기다린다."""
    print(f"=== 응답 시간 분포 ({sido}, {count}회) ===")
    print("   우리 타임아웃 없이 상대가 언제 답하는지 그대로 잰다")
    print()

    ok, failed = [], []

    for attempt in range(1, count + 1):
        elapsed, result, items = call(auth_key, sido, PROBE_TIMEOUT)
        (ok if result == "성공" else failed).append(elapsed)

        over = "  <- 서비스 컷(4초) 초과" if elapsed > SERVICE_READ_TIMEOUT * 1000 else ""
        detail = f"측정소 {items}곳" if result == "성공" else result
        print(f"  {attempt:3d}   {elapsed:8.0f}ms   {detail}{over}")

    print()
    summarize("성공", ok)
    summarize("실패", failed)

    if ok and failed:
        gap_start = max(ok)
        gap_end = min(failed)

        if gap_start < gap_end:
            print()
            print(f"  가장 느린 성공 {gap_start:.0f}ms 와 가장 빠른 실패 {gap_end:.0f}ms 사이가 비어 있다.")
            print(f"  느려지다 실패하는 것이 아니라 성공과 실패가 다른 상태다.")
            print(f"  타임아웃을 늘려도 이 구간에는 건질 요청이 없다.")


def probe_retry(auth_key, sido, count):
    """실제 코드와 같은 4초 컷에서 재시도 1회가 성공률을 얼마나 바꾸는지 본다."""
    print()
    print(f"=== 4초 컷 + 재시도 1회 ({sido}, {count}회) ===")
    print("   RestClientConfig의 read timeout과 같은 조건")
    print()

    first_ok = retried_ok = 0
    waits_without_retry, waits_with_retry = [], []

    for attempt in range(1, count + 1):
        elapsed, result, _ = call(auth_key, sido, SERVICE_READ_TIMEOUT)
        waits_without_retry.append(elapsed)

        if result == "성공":
            first_ok += 1
            retried_ok += 1
            waits_with_retry.append(elapsed)
            print(f"  {attempt:3d}   1차 성공 {elapsed:7.0f}ms")
            continue

        retry_elapsed, retry_result, _ = call(auth_key, sido, SERVICE_READ_TIMEOUT)
        waits_with_retry.append(elapsed + retry_elapsed)

        if retry_result == "성공":
            retried_ok += 1

        print(f"  {attempt:3d}   1차 {result} {elapsed:7.0f}ms"
              f"  ->  2차 {retry_result} {retry_elapsed:7.0f}ms"
              f"   사용자 대기 {elapsed + retry_elapsed:7.0f}ms")

    print()
    print(f"  재시도 없음   성공 {first_ok:2d}/{count}  =  {first_ok / count * 100:3.0f}%"
          f"   대기 중앙 {statistics.median(waits_without_retry):6.0f}ms"
          f"  최대 {max(waits_without_retry):6.0f}ms")
    print(f"  재시도 1회    성공 {retried_ok:2d}/{count}  =  {retried_ok / count * 100:3.0f}%"
          f"   대기 중앙 {statistics.median(waits_with_retry):6.0f}ms"
          f"  최대 {max(waits_with_retry):6.0f}ms")
    print()
    print("  재시도가 성공할 때는 빠르다. 실패가 지연이 아니라 다른 상태이기 때문이고,")
    print("  그래서 백오프를 두지 않는다. 대신 둘 다 실패하면 대기가 두 배가 되므로")
    print("  실패한 대상은 쿨다운에 넣어 그동안 외부 호출을 건너뛴다.")


def main():
    parser = argparse.ArgumentParser(description="에어코리아 응답 시간을 잰다.")
    parser.add_argument("--sido", default="서울")
    parser.add_argument("--count", type=int, default=20,
                        help="분포 측정 횟수 (기본 20). 일 500건 한도를 넘기지 말 것")
    parser.add_argument("--retry-trial", type=int, default=15,
                        help="재시도 효과 측정 횟수 (기본 15). 0이면 건너뛴다")
    parser.add_argument("--env-file", type=Path,
                        default=Path(__file__).resolve().parent.parent / ".env")
    args = parser.parse_args()

    auth_key = load_auth_key(args.env_file)

    probe_distribution(auth_key, args.sido, args.count)

    if args.retry_trial > 0:
        probe_retry(auth_key, args.sido, args.retry_trial)

    used = args.count + args.retry_trial * 2
    print()
    print(f"이번 실행에서 최대 {used}건을 호출했다. 개발계정 한도는 일 500건이다.")


if __name__ == "__main__":
    main()
