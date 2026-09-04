# 배달온도 k6 부하테스트 기록

배달온도 서버의 k6 부하테스트 원본 기록과 성능 개선 전·후 결과를 누적한다.
수치의 단위와 출처를 명시하고, 확인되지 않은 항목은 표에서 생략한다.

## 기록 원칙

1. 테스트 결과는 실행 순서대로 문서 끝에 추가하며 기존 항목을 삭제하거나 덮어쓰지 않는다.
2. 출력 또는 모니터링 지표에서 확인되는 값만 기록한다. 확인할 수 없는 항목은 해당 표에서 생략한다.
3. RPS, duration 등으로 산출한 값은 `계산값`이라고 표시하고 계산식 또는 근거를 메모에 남긴다.
4. k6 원본 출력은 해당 테스트 항목의 `<details>` 영역에 수정 없이 보관한다. 비밀값, 토큰 등 민감정보만 `[REDACTED]`로 치환하고 그 사실을 메모한다.
5. 동일 조건의 Before/After 결과가 생기면 `개선 전/후 비교` 표를 갱신한다.
6. 성능 개선 전 원본 결과와 당시 분석은 이후 결과에 맞추어 소급 수정하지 않는다. 정정이 필요하면 원문은 유지하고 정정 메모를 새로 추가한다.
7. 포트폴리오에 활용할 만한 주요 변화는 `핵심 성과`에 한 줄씩 누적한다.
8. Git diff가 불필요하게 커지지 않도록 새 항목과 관련 요약 표만 수정하며 문서 전체를 재정렬하거나 재포맷하지 않는다.

별도 지시가 있기 전까지 모든 새 테스트는 성능 개선 전 결과인 `Before`로 분류한다.
사용자가 k6 세션 창 전체를 붙여 넣으므로, 전체 출력에 `dropped_iterations` 행이 없으면 dropped iterations는 `0`으로 기록한다.

## 핵심 성과

아직 기록된 핵심 성과가 없다.

<!--
성과 문장 예시(실제 측정값으로 교체):
- YYYY-MM-DD — [개선 내용]: 동일 조건에서 p95 응답 시간이 A ms에서 B ms로 N% 감소하고 오류율이 C%에서 D%로 감소했다. (Before: TEST-ID, After: TEST-ID)
-->

## 테스트 환경

실행마다 달라질 수 있는 값은 각 테스트 항목에도 반드시 기록한다. 이 표는 최근 또는 공통 환경을 빠르게 확인하기 위한 요약이다.

| 항목 | 값 |
|---|---|
| 최근 테스트 날짜 | 2026-09-04 |

## 테스트 목록

| 테스트 ID | 날짜 | 구분 | 테스트명 | 부하 조건 | 비고 |
|---|---|---|---|---|---|
| TEST-20260904-001 | 2026-09-04 | Before | `smoke.js` 단일 반복 스모크 테스트 | 1 VU, 1 iteration | 첫 기록 |
| TEST-20260904-002 | 2026-09-04 | Before | `baseline-practice.js` 기준 테스트 | 1.50 iterations/s, 1m0s, maxVUs 5 | 기본 분류 |
| TEST-20260904-003 | 2026-09-04 | Before | `baseline-before.js` 5분 기준 테스트 | 1.50 iterations/s, 5m0s, maxVUs 5 | 세 번째 결과 |
| TEST-20260904-004 | 2026-09-04 | Before | `peak-before.js` 피크 테스트 | 7.00 iterations/s, 10m0s, maxVUs 10 | 동일 출력 2회 첨부, 1건으로 기록 |
| TEST-20260904-005 | 2026-09-04 | Before | `capacity-20-before.js` 용량 테스트 | 20.00 iterations/s, 3m0s, maxVUs 20 | 기본 분류 |
| TEST-20260904-006 | 2026-09-04 | Before | `capacity-40-before.js` 용량 테스트 | 40.00 iterations/s, 3m0s, maxVUs 40 | 기본 분류 |
| TEST-20260904-007 | 2026-09-04 | Before | `capacity-80-before.js` 용량 테스트 | 80.00 iterations/s, 3m0s, maxVUs 80 | 측정 무효에 가까움 — load generator VU allocation이 부족했으므로 동일 80 RPS를 충분한 VU pool로 재측정해야 함. |
| TEST-20260904-008 | 2026-09-04 | Before | `capacity-80-retry-before.js` 80 RPS 재측정 | 80.00 iterations/s, 3m0s, maxVUs 200 | TEST-20260904-007 재측정; VU 부족 경고 없음, dropped iterations 0(사용자 확인) |
| TEST-20260904-009 | 2026-09-04 | Before | `capacity-120-before.js` 용량 테스트 | 120.00 iterations/s, 3m0s, maxVUs 300 | 측정 무효에 가까움 — maxVUs 300 도달 및 dropped iterations 128건; 충분한 VU pool로 재측정 필요 |
| TEST-20260904-010 | 2026-09-04 | Before | `capacity-100-before.js` 용량 테스트 | 100.00 iterations/s, 3m0s, maxVUs 400 | VU 부족 경고 없음; 유효한 Before 기준 후보 |
| TEST-20260904-011 | 2026-09-04 | Before | 110 RPS 용량 테스트 | 110.00 iterations/s, 3m0s, maxVUs 600 | 묶음 제공 결과; dropped iterations 0 |
| TEST-20260904-012 | 2026-09-04 | Before | 115 RPS 용량 테스트 | 115.00 iterations/s, 3m0s, maxVUs 600 | 묶음 제공 결과; dropped iterations 0 |
| TEST-20260904-013 | 2026-09-04 | Before | 120 RPS 재측정 | 120.00 iterations/s, 3m0s, maxVUs 600 | TEST-20260904-009 재측정; dropped iterations 0 |
| TEST-20260904-014 | 2026-09-04 | Before | 130 RPS 용량 테스트 | 130.00 iterations/s, duration 계산값 약 3m0s, maxVUs 600 | dropped iterations 0; p95 3.38s |
| TEST-20260904-015 | 2026-09-04 | Before | 140 RPS 용량 테스트 | 140 RPS 목표, duration 계산값 약 3m04s, maxVUs 600 | 측정 무효에 가까움 — maxVUs 도달, dropped iterations 344건 |
| TEST-20260904-016 | 2026-09-04 | Before | 150 RPS 용량 테스트 | 150 RPS 목표, duration 계산값 약 3m05s, maxVUs 600 | 측정 무효에 가까움 — maxVUs 도달, dropped iterations 1,920건 |
| TEST-20260904-017 | 2026-09-04 | Before | `capacity-before.js` 125 RPS 용량 테스트 | 125 RPS, duration 계산값 약 3m0s, maxVUs 600 | dropped iterations 0; 유효한 Before 기준 후보 |
| TEST-20260904-018 | 2026-09-04 | Before | CPU 관측용 100 RPS 테스트 | 100 RPS, 3m0s, maxVUs 600 | CPU 지표 매칭 구간: 12:31:15Z~12:34:15Z |
| TEST-20260904-019 | 2026-09-04 | Before | CPU 관측용 120 RPS 테스트 | 120 RPS, k6 집계 약 3m0s, maxVUs 600 | CPU 지표 매칭 구간: 12:36:15Z~12:39:16Z |
| TEST-20260904-020 | 2026-09-04 | Before | CPU 관측용 125 RPS 테스트 | 125 RPS, 3m0s, maxVUs 600 | CPU 지표 매칭 구간: 12:41:16Z~12:44:16Z |
| TEST-20260904-021 | 2026-09-04 | Before | CPU 관측용 130 RPS 테스트 | 130 RPS, k6 집계 약 3m0s, maxVUs 600 | CPU 지표 매칭 구간: 12:46:16Z~12:49:17Z |
| TEST-20260904-022 | 2026-09-04 | Before | CPU 관측용 140 RPS 재측정 | 140 RPS, k6 집계 약 3m0s, maxVUs 1,000 | TEST-20260904-015 재측정; VU 부족 경고 및 dropped iterations 없음 |
| TEST-20260904-023 | 2026-09-04 | Before | CPU 관측용 150 RPS 재측정 | 150 RPS, k6 집계 약 3m0s, maxVUs 1,000 | TEST-20260904-016 재측정; VU 부족 경고 및 dropped iterations 없음 |
| TEST-20260904-024 | 2026-09-04 | Before | 150 RPS 프로세스별 CPU 진단 테스트 | 150 RPS(파일명 기준), k6 집계 약 3m1s, maxVUs 1,000 | 앱·DB 컨테이너 CPU/메모리 18회 관측; dropped iterations 0 |
| TEST-20260904-025 | 2026-09-04 | Before | 140 RPS 정각 회피 재측정 | 140.00 iterations/s, k6 집계 계산값 약 3m0s, maxVUs 1,000 | TEST-20260904-022 재측정; 정각 구간을 피해 13:45:25Z 시작; dropped iterations 0 |

## 개선 전/후 비교

비교는 대상 서버/인스턴스, 경로, 시나리오, executor, VU 또는 rate, duration 등 주요 부하 조건이 같은 테스트끼리 수행한다. 조건이 다르면 차이를 명시하고 직접 비교의 한계를 `분석/메모`에 남긴다.

아직 동일 조건의 Before/After 쌍이 없다. 쌍이 생기면 조건별 비교표를 추가한다.

## 새 테스트 항목 작성 규칙

- 테스트 ID는 `TEST-YYYYMMDD-NNN` 형식을 사용한다.
- 테스트 환경, 조건, 핵심 결과, 서버 관측값은 출력이나 사용자 설명으로 확인된 항목만 기록한다.
- 계산한 값은 계산식과 함께 `계산값`으로 표시한다.
- 원본 k6 출력은 해당 테스트의 접이식 영역에 수정 없이 보관한다.

## 테스트별 기록

새 결과는 기존 기록을 수정하지 않고 이 섹션의 끝에 추가한다.

### TEST-20260904-001 — `smoke.js` 단일 반복 스모크 테스트

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |
| 실행 스크립트 | `smoke.js` |
| k6 실행 방식 | local |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | `smoke.js` 단일 반복 스모크 테스트 |
| 목적 | HTTP 상태 200 및 `dashboard rendered` 확인 |
| executor | `shared-iterations` (출력: 1 iteration shared among 1 VU) |
| VU | 1 (max VUs: 1) |
| rate/RPS | 설정 rate 없음, 실측 9.285561 req/s |
| duration | 실제 약 0.1s, iteration duration 107.35ms; 설정 상한 maxDuration 10m0s + gracefulStop 30s |
| 총 요청 수 | 1 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 84.05ms | `http_req_duration` |
| p50/median | 84.05ms | `http_req_duration`의 `med` |
| p90 | 84.05ms | `http_req_duration` |
| p95 | 84.05ms | `http_req_duration` |
| max | 84.05ms | `http_req_duration` |
| error rate | 0.00% (0/1) | `http_req_failed` |
| dropped iterations | 0 | 전체 k6 세션 출력에 별도 행 없음 |

#### 분석/메모

- checks 2/2 성공, 실패 0건으로 HTTP 200과 dashboard 렌더링 확인을 통과했다.
- 요청 표본이 1건뿐이므로 avg, median, p90, p95, max가 모두 동일하다. 이 결과만으로 성능 추세나 병목을 판단하지 않는다.
- dropped iterations는 전체 세션 출력에 별도 행이 없으므로 사용자 지정 판정 기준에 따라 0으로 기록했다.
- `10m0s`는 설정된 maxDuration이며 실제 실행 시간은 출력상 약 0.1초다.
- 계산값: 없음. 9.285561 req/s는 k6 원본 출력값이다.
- 이전 테스트와 달라진 조건: 첫 기록이므로 비교 불가.
- 동일 조건의 Before/After 쌍이 없어 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
$ k6 run smoke.js
```swift
     /\      Grafana   /‾‾/  
/\  /  \     |\  __   /  /   
```

/  /    \    | |/ /  /   ‾‾\\
/          \   |   (  |  (‾)  |
/ \_\_\_\_\_\_\_\_\_\_ \  |*|\_\  \_*\_\_\_/
```yaml
 execution: local
    script: smoke.js
    output: -

 scenarios: (100.00%) 1 scenario, 1 max VUs, 10m30s max duration (incl. graceful stop):
          * default: 1 iterations shared among 1 VUs (maxDuration: 10m0s, gracefulStop: 30s)
```



█ TOTAL RESULTS
```yaml
checks_total.......: 2       18.571121/s
checks_succeeded...: 100.00% 2 out of 2
checks_failed......: 0.00%   0 out of 2

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=84.05ms  min=84.05ms  med=84.05ms  max=84.05ms  p(90)=84.05ms  p(95)=84.05ms 
  { expected_response:true }...: avg=84.05ms  min=84.05ms  med=84.05ms  max=84.05ms  p(90)=84.05ms  p(95)=84.05ms 
http_req_failed................: 0.00%  0 out of 1
http_reqs......................: 1      9.285561/s

EXECUTION
iteration_duration.............: avg=107.35ms min=107.35ms med=107.35ms max=107.35ms p(90)=107.35ms p(95)=107.35ms
iterations.....................: 1      9.285561/s

NETWORK
data_received..................: 42 kB  390 kB/s
data_sent......................: 1.7 kB 16 kB/s
```



running (00m00.1s), 0/1 VUs, 1 complete and 0 interrupted iterations
default ✓ [======================================] 1 VUs  00m00.1s/10m0s  1/1 shared iters
````

</details>

### TEST-20260904-002 — `baseline-practice.js` 기준 테스트

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |
| 실행 스크립트 | `baseline-practice.js` |
| k6 실행 방식 | local |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | `baseline-practice.js` 기준 테스트 |
| 목적 | 1.50 iterations/s 고정 도착률에서 기준 성능 측정 (스크립트·시나리오명 기준) |
| executor | `constant-arrival-rate` (k6 출력 형식에서 식별) |
| VU | maxVUs 5; `vus` 관측값 min=0, max=0 |
| rate/RPS | 설정 1.50 iterations/s, 실측 1.499974 req/s |
| duration | 실제 1m00.0s; 설정 1m0s, gracefulStop 포함 최대 1m30s |
| 총 요청 수 | 90 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 40.67ms | `http_req_duration` |
| p50/median | 36.02ms | `http_req_duration`의 `med` |
| p90 | 64.23ms | `http_req_duration` |
| p95 | 68.25ms | `http_req_duration` |
| max | 110.45ms | `http_req_duration` |
| error rate | 0.00% (0/90) | `http_req_failed` |
| dropped iterations | 0 | 전체 k6 세션 출력에 별도 행 없음 |

#### 분석/메모

- checks 180/180 성공, 실패 0건이며 HTTP 요청 90건 모두 성공했다.
- 설정 rate 1.50 iterations/s에 대해 실측 처리량은 1.499974 req/s로 거의 일치했다.
- 계산값: 목표 rate 대비 차이는 -0.000026 req/s, 달성률은 약 99.9983%다. `(1.499974 / 1.50) × 100`
- dropped iterations는 전체 세션 출력에 별도 행이 없으므로 사용자 지정 판정 기준에 따라 0으로 기록했다.
- executor는 출력의 `iterations/s`, `maxVUs` 형식을 근거로 `constant-arrival-rate`로 식별했다.
- `vus`는 원본에 min=0, max=0으로 표시되며 원인을 추측하지 않고 그대로 기록했다.
- 이전 테스트와 달라진 조건: TEST-20260904-001은 단일 shared iteration 스모크 테스트이므로 직접 비교하지 않는다.
- 동일 조건의 After 결과가 없어 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
```swift
     /\      Grafana   /‾‾/  
/\  /  \     |\  __   /  /   
```

/  /    \    | |/ /  /   ‾‾\\
/          \   |   (  |  (‾)  |
/ \_\_\_\_\_\_\_\_\_\_ \  |*|\_\  \_*\_\_\_/
```yaml
 execution: local
    script: baseline-practice.js
    output: -

 scenarios: (100.00%) 1 scenario, 5 max VUs, 1m30s max duration (incl. graceful stop):
          * baseline: 1.50 iterations/s for 1m0s (maxVUs: 5, gracefulStop: 30s)
```



█ TOTAL RESULTS
```yaml
checks_total.......: 180     2.999948/s
checks_succeeded...: 100.00% 180 out of 180
checks_failed......: 0.00%   0 out of 180

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=40.67ms min=21.51ms med=36.02ms max=110.45ms p(90)=64.23ms p(95)=68.25ms
  { expected_response:true }...: avg=40.67ms min=21.51ms med=36.02ms max=110.45ms p(90)=64.23ms p(95)=68.25ms
http_req_failed................: 0.00%  0 out of 90
http_reqs......................: 90     1.499974/s

EXECUTION
iteration_duration.............: avg=41.69ms min=22.07ms med=36.42ms max=111.32ms p(90)=64.67ms p(95)=74.39ms
iterations.....................: 90     1.499974/s
vus............................: 0      min=0       max=0
vus_max........................: 5      min=5       max=5

NETWORK
data_received..................: 3.4 MB 57 kB/s
data_sent......................: 19 kB  317 B/s
```



running (1m00.0s), 0/5 VUs, 90 complete and 0 interrupted iterations
baseline ✓ [======================================] 0/5 VUs  1m0s  1.50 iters/s
````

</details>

### TEST-20260904-003 — `baseline-before.js` 5분 기준 테스트

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |
| 실행 스크립트 | `baseline-before.js` |
| k6 실행 방식 | local |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | `baseline-before.js` 5분 기준 테스트 |
| 목적 | 1.50 iterations/s 고정 도착률을 5분간 적용한 개선 전 기준 성능 측정 |
| executor | `constant-arrival-rate` (k6 출력 형식에서 식별) |
| VU | maxVUs 5; `vus` 관측값 min=0, max=0 |
| rate/RPS | 설정 1.50 iterations/s, 실측 1.50326 req/s |
| duration | 실제 5m00.0s; 설정 5m0s, gracefulStop 포함 최대 5m30s |
| 총 요청 수 | 451 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 23.17ms | `http_req_duration` |
| p50/median | 19.1ms | `http_req_duration`의 `med` |
| p90 | 29.04ms | `http_req_duration` |
| p95 | 35.05ms | `http_req_duration` |
| max | 238.13ms | `http_req_duration` |
| error rate | 0.00% (0/451) | `http_req_failed` |
| dropped iterations | 0 | 전체 k6 세션 출력에 별도 행 없음 |

#### 분석/메모

- checks 902/902 성공, 실패 0건이며 HTTP 요청 451건 모두 성공했다.
- 설정 rate 1.50 iterations/s에 대해 실측 처리량은 1.50326 req/s다.
- 계산값: 목표 rate 대비 차이는 +0.00326 req/s, 달성률은 약 100.2173%다. `(1.50326 / 1.50) × 100`
- max 238.13ms는 p95 35.05ms의 약 6.79배다(계산값). 일부 긴 응답이 있었지만 서버 관측값이 없어 원인을 특정하지 않는다.
- dropped iterations는 전체 세션 출력에 별도 행이 없으므로 사용자 지정 판정 기준에 따라 0으로 기록했다.
- executor는 출력의 `iterations/s`, `maxVUs` 형식을 근거로 `constant-arrival-rate`로 식별했다.
- `vus`는 원본에 min=0, max=0으로 표시되며 원인을 추측하지 않고 그대로 기록했다.
- TEST-20260904-002와 설정 rate 및 maxVUs는 같지만 duration이 1분과 5분으로 달라 동일 조건 비교에서 제외한다.
- 동일 조건의 After 결과가 없어 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
$ k6 run baseline-before.js
```swift
     /\      Grafana   /‾‾/  
/\  /  \     |\  __   /  /   
```

/  /    \    | |/ /  /   ‾‾\\
/          \   |   (  |  (‾)  |
/ \_\_\_\_\_\_\_\_\_\_ \  |*|\_\  \_*\_\_\_/
```yaml
 execution: local
    script: baseline-before.js
    output: -

 scenarios: (100.00%) 1 scenario, 5 max VUs, 5m30s max duration (incl. graceful stop):
          * baseline: 1.50 iterations/s for 5m0s (maxVUs: 5, gracefulStop: 30s)
```



█ TOTAL RESULTS
```yaml
checks_total.......: 902     3.006521/s
checks_succeeded...: 100.00% 902 out of 902
checks_failed......: 0.00%   0 out of 902

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=23.17ms min=13.24ms med=19.1ms  max=238.13ms p(90)=29.04ms p(95)=35.05ms
  { expected_response:true }...: avg=23.17ms min=13.24ms med=19.1ms  max=238.13ms p(90)=29.04ms p(95)=35.05ms
http_req_failed................: 0.00% 0 out of 451
http_reqs......................: 451   1.50326/s

EXECUTION
iteration_duration.............: avg=23.73ms min=13.76ms med=19.53ms max=238.59ms p(90)=30.28ms p(95)=36.84ms
iterations.....................: 451   1.50326/s
vus............................: 0     min=0        max=0
vus_max........................: 5     min=5        max=5

NETWORK
data_received..................: 17 MB 57 kB/s
data_sent......................: 63 kB 211 B/s
```



running (5m00.0s), 0/5 VUs, 451 complete and 0 interrupted iterations
baseline ✓ [======================================] 0/5 VUs  5m0s  1.50 iters/s
````

</details>

### TEST-20260904-004 — `peak-before.js` 피크 테스트

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |
| 실행 스크립트 | `peak-before.js` |
| k6 실행 방식 | local |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | `peak-before.js` 피크 테스트 |
| 목적 | 7.00 iterations/s 고정 도착률을 10분간 적용한 개선 전 피크 성능 측정 |
| executor | `constant-arrival-rate` (k6 출력 형식에서 식별) |
| VU | maxVUs 10; `vus` 관측값 min=0, max=1 |
| rate/RPS | 설정 7.00 iterations/s, 실측 7.001519 req/s |
| duration | 실제 10m00.0s; 설정 10m0s, gracefulStop 포함 최대 10m30s |
| 총 요청 수 | 4,201 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 14.96ms | `http_req_duration` |
| p50/median | 11.57ms | `http_req_duration`의 `med` |
| p90 | 16.79ms | `http_req_duration` |
| p95 | 22.73ms | `http_req_duration` |
| max | 197.83ms | `http_req_duration` |
| error rate | 0.00% (0/4,201) | `http_req_failed` |
| dropped iterations | 0 | 전체 k6 세션 출력에 별도 행 없음 |

#### 분석/메모

- checks 8,402/8,402 성공, 실패 0건이며 HTTP 요청 4,201건 모두 성공했다.
- 설정 rate 7.00 iterations/s에 대해 실측 처리량은 7.001519 req/s다.
- 계산값: 목표 rate 대비 차이는 +0.001519 req/s, 달성률은 약 100.0217%다. `(7.001519 / 7.00) × 100`
- max 197.83ms는 p95 22.73ms의 약 8.70배다(계산값). 일부 긴 응답이 있었지만 서버 관측값이 없어 원인을 특정하지 않는다.
- dropped iterations는 전체 세션 출력에 별도 행이 없으므로 사용자 지정 판정 기준에 따라 0으로 기록했다.
- executor는 출력의 `iterations/s`, `maxVUs` 형식을 근거로 `constant-arrival-rate`로 식별했다.
- `vus`는 원본에 min=0, max=1로 표시되며 원인을 추측하지 않고 그대로 기록했다.
- 제공된 입력에 완전히 동일한 결과 블록이 두 번 포함되어 있다. 별도 실행임을 입증할 차이가 없어 테스트 1건으로 집계했으며, 원본 영역에는 두 블록을 모두 보존했다.
- 동일 조건의 After 결과가 없어 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
$ k6 run peak-before.js
```swift
     /\      Grafana   /‾‾/  
/\  /  \     |\  __   /  /   
```

/  /    \    | |/ /  /   ‾‾\\
/          \   |   (  |  (‾)  |
/ \_\_\_\_\_\_\_\_\_\_ \  |*|\_\  \_*\_\_\_/
```yaml
 execution: local
    script: peak-before.js
    output: -

 scenarios: (100.00%) 1 scenario, 10 max VUs, 10m30s max duration (incl. graceful stop):
          * peak: 7.00 iterations/s for 10m0s (maxVUs: 10, gracefulStop: 30s)
```



█ TOTAL RESULTS
```yaml
checks_total.......: 8402    14.003038/s
checks_succeeded...: 100.00% 8402 out of 8402
checks_failed......: 0.00%   0 out of 8402

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=14.96ms min=9.85ms  med=11.57ms max=197.83ms p(90)=16.79ms p(95)=22.73ms
  { expected_response:true }...: avg=14.96ms min=9.85ms  med=11.57ms max=197.83ms p(90)=16.79ms p(95)=22.73ms
http_req_failed................: 0.00%  0 out of 4201
http_reqs......................: 4201   7.001519/s

EXECUTION
iteration_duration.............: avg=15.44ms min=10.21ms med=12.02ms max=198.33ms p(90)=17.27ms p(95)=23.59ms
iterations.....................: 4201   7.001519/s
vus............................: 0      min=0         max=1 
vus_max........................: 10     min=10        max=10

NETWORK
data_received..................: 160 MB 267 kB/s
data_sent......................: 533 kB 888 B/s
```



running (10m00.0s), 00/10 VUs, 4201 complete and 0 interrupted iterations
peak ✓ [======================================] 00/10 VUs  10m0s  7.00 iters/s

$ k6 run peak-before.js
```swift
     /\      Grafana   /‾‾/  
/\  /  \     |\  __   /  /   
```

/  /    \    | |/ /  /   ‾‾\\
/          \   |   (  |  (‾)  |
/ \_\_\_\_\_\_\_\_\_\_ \  |*|\_\  \_*\_\_\_/
```yaml
 execution: local
    script: peak-before.js
    output: -

 scenarios: (100.00%) 1 scenario, 10 max VUs, 10m30s max duration (incl. graceful stop):
          * peak: 7.00 iterations/s for 10m0s (maxVUs: 10, gracefulStop: 30s)
```



█ TOTAL RESULTS
```yaml
checks_total.......: 8402    14.003038/s
checks_succeeded...: 100.00% 8402 out of 8402
checks_failed......: 0.00%   0 out of 8402

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=14.96ms min=9.85ms  med=11.57ms max=197.83ms p(90)=16.79ms p(95)=22.73ms
  { expected_response:true }...: avg=14.96ms min=9.85ms  med=11.57ms max=197.83ms p(90)=16.79ms p(95)=22.73ms
http_req_failed................: 0.00%  0 out of 4201
http_reqs......................: 4201   7.001519/s

EXECUTION
iteration_duration.............: avg=15.44ms min=10.21ms med=12.02ms max=198.33ms p(90)=17.27ms p(95)=23.59ms
iterations.....................: 4201   7.001519/s
vus............................: 0      min=0         max=1 
vus_max........................: 10     min=10        max=10

NETWORK
data_received..................: 160 MB 267 kB/s
data_sent......................: 533 kB 888 B/s
```



running (10m00.0s), 00/10 VUs, 4201 complete and 0 interrupted iterations
peak ✓ [======================================] 00/10 VUs  10m0s  7.00 iters/s
````

</details>

### TEST-20260904-005 — `capacity-20-before.js` 용량 테스트

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |
| 실행 스크립트 | `capacity-20-before.js` |
| k6 실행 방식 | local |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | `capacity-20-before.js` 용량 테스트 |
| 목적 | 20.00 iterations/s 고정 도착률을 3분간 적용한 개선 전 처리 용량 측정 |
| executor | `constant-arrival-rate` (k6 출력 형식에서 식별) |
| VU | maxVUs 20; `vus` 관측값 min=0, max=3 |
| rate/RPS | 설정 20.00 iterations/s, 실측 20.00419 req/s |
| duration | 실제 3m00.0s; 설정 3m0s, gracefulStop 포함 최대 3m30s |
| 총 요청 수 | 3,601 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 13.67ms | `http_req_duration` |
| p50/median | 11.06ms | `http_req_duration`의 `med` |
| p90 | 14.83ms | `http_req_duration` |
| p95 | 19.28ms | `http_req_duration` |
| max | 239.11ms | `http_req_duration` |
| error rate | 0.00% (0/3,601) | `http_req_failed` |
| dropped iterations | 0 | 전체 k6 세션 출력에 별도 행 없음 |

#### 분석/메모

- checks 7,202/7,202 성공, 실패 0건이며 HTTP 요청 3,601건 모두 성공했다.
- 설정 rate 20.00 iterations/s에 대해 실측 처리량은 20.00419 req/s다.
- 계산값: 목표 rate 대비 차이는 +0.00419 req/s, 달성률은 약 100.0210%다. `(20.00419 / 20.00) × 100`
- max 239.11ms는 p95 19.28ms의 약 12.40배다(계산값). 일부 긴 응답이 있었지만 서버 관측값이 없어 원인을 특정하지 않는다.
- dropped iterations는 전체 세션 출력에 별도 행이 없으므로 사용자 지정 판정 기준에 따라 0으로 기록했다.
- executor는 출력의 `iterations/s`, `maxVUs` 형식을 근거로 `constant-arrival-rate`로 식별했다.
- `vus`는 원본에 min=0, max=3으로 표시되며 원인을 추측하지 않고 그대로 기록했다.
- 동일 조건의 After 결과가 없어 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
$ k6 run capacity-20-before.js
```swift
     /\      Grafana   /‾‾/  
/\  /  \     |\  __   /  /   
```

/  /    \    | |/ /  /   ‾‾\\
/          \   |   (  |  (‾)  |
/ \_\_\_\_\_\_\_\_\_\_ \  |*|\_\  \_*\_\_\_/
```yaml
 execution: local
    script: capacity-20-before.js
    output: -

 scenarios: (100.00%) 1 scenario, 20 max VUs, 3m30s max duration (incl. graceful stop):
          * capacity20: 20.00 iterations/s for 3m0s (maxVUs: 20, gracefulStop: 30s)
```



█ TOTAL RESULTS
```yaml
checks_total.......: 7202    40.00838/s
checks_succeeded...: 100.00% 7202 out of 7202
checks_failed......: 0.00%   0 out of 7202

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=13.67ms min=9.52ms med=11.06ms max=239.11ms p(90)=14.83ms p(95)=19.28ms
  { expected_response:true }...: avg=13.67ms min=9.52ms med=11.06ms max=239.11ms p(90)=14.83ms p(95)=19.28ms
http_req_failed................: 0.00%  0 out of 3601
http_reqs......................: 3601   20.00419/s

EXECUTION
iteration_duration.............: avg=14.18ms min=9.92ms med=11.5ms  max=239.59ms p(90)=15.44ms p(95)=20.06ms
iterations.....................: 3601   20.00419/s
vus............................: 0      min=0         max=3 
vus_max........................: 20     min=20        max=20

NETWORK
data_received..................: 137 MB 762 kB/s
data_sent......................: 475 kB 2.6 kB/s
```



running (3m00.0s), 00/20 VUs, 3601 complete and 0 interrupted iterations
capacity20 ✓ [======================================] 00/20 VUs  3m0s  20.00 iters/s
````

</details>

### TEST-20260904-006 — `capacity-40-before.js` 용량 테스트

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |
| 실행 스크립트 | `capacity-40-before.js` |
| k6 실행 방식 | local |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | `capacity-40-before.js` 용량 테스트 |
| 목적 | 40.00 iterations/s 고정 도착률을 3분간 적용한 개선 전 처리 용량 측정 |
| executor | `constant-arrival-rate` (k6 출력 형식에서 식별) |
| VU | maxVUs 40; `vus` 관측값 min=0, max=18 |
| rate/RPS | 설정 40.00 iterations/s, 실측 40.002981 req/s |
| duration | 실제 3m00.0s; 설정 3m0s, gracefulStop 포함 최대 3m30s |
| 총 요청 수 | 7,201 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 15.46ms | `http_req_duration` |
| p50/median | 10.63ms | `http_req_duration`의 `med` |
| p90 | 13.6ms | `http_req_duration` |
| p95 | 16.85ms | `http_req_duration` |
| max | 620.43ms | `http_req_duration` |
| error rate | 0.00% (0/7,201) | `http_req_failed` |
| dropped iterations | 0 | 전체 k6 세션 출력에 별도 행 없음 |

#### 분석/메모

- checks 14,402/14,402 성공, 실패 0건이며 HTTP 요청 7,201건 모두 성공했다.
- 설정 rate 40.00 iterations/s에 대해 실측 처리량은 40.002981 req/s다.
- 계산값: 목표 rate 대비 차이는 +0.002981 req/s, 달성률은 약 100.0075%다. `(40.002981 / 40.00) × 100`
- max 620.43ms는 p95 16.85ms의 약 36.82배다(계산값). 긴 꼬리 응답이 있었지만 서버 관측값이 없어 원인을 특정하지 않는다.
- dropped iterations는 전체 세션 출력에 별도 행이 없으므로 사용자 지정 판정 기준에 따라 0으로 기록했다.
- executor는 출력의 `iterations/s`, `maxVUs` 형식을 근거로 `constant-arrival-rate`로 식별했다.
- `vus`는 원본에 min=0, max=18로 표시되며 원인을 추측하지 않고 그대로 기록했다.
- TEST-20260904-005와 duration은 같지만 설정 rate와 maxVUs가 달라 동일 조건 비교에서 제외한다.
- 동일 조건의 After 결과가 없어 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
$ k6 run capacity-40-before.js
```swift
     /\      Grafana   /‾‾/  
/\  /  \     |\  __   /  /   
```

/  /    \    | |/ /  /   ‾‾\\
/          \   |   (  |  (‾)  |
/ \_\_\_\_\_\_\_\_\_\_ \  |*|\_\  \_*\_\_\_/
```yaml
 execution: local
    script: capacity-40-before.js
    output: -

 scenarios: (100.00%) 1 scenario, 40 max VUs, 3m30s max duration (incl. graceful stop):
          * capacity40: 40.00 iterations/s for 3m0s (maxVUs: 40, gracefulStop: 30s)
```



█ TOTAL RESULTS
```yaml
checks_total.......: 14402   80.005962/s
checks_succeeded...: 100.00% 14402 out of 14402
checks_failed......: 0.00%   0 out of 14402

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=15.46ms min=9.17ms med=10.63ms max=620.43ms p(90)=13.6ms  p(95)=16.85ms
  { expected_response:true }...: avg=15.46ms min=9.17ms med=10.63ms max=620.43ms p(90)=13.6ms  p(95)=16.85ms
http_req_failed................: 0.00%  0 out of 7201
http_reqs......................: 7201   40.002981/s

EXECUTION
iteration_duration.............: avg=15.96ms min=9.61ms med=11.06ms max=622.23ms p(90)=14.26ms p(95)=17.88ms
iterations.....................: 7201   40.002981/s
vus............................: 0      min=0         max=18
vus_max........................: 40     min=40        max=40

NETWORK
data_received..................: 274 MB 1.5 MB/s
data_sent......................: 949 kB 5.3 kB/s
```



running (3m00.0s), 00/40 VUs, 7201 complete and 0 interrupted iterations
capacity40 ✓ [======================================] 00/40 VUs  3m0s  40.00 iters/s
````

</details>

### TEST-20260904-007 — `capacity-80-before.js` 용량 테스트

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |
| 실행 스크립트 | `capacity-80-before.js` |
| k6 실행 방식 | local |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | `capacity-80-before.js` 용량 테스트 |
| 목적 | 80.00 iterations/s 고정 도착률을 3분간 적용한 개선 전 처리 용량 측정 |
| executor | `constant-arrival-rate` (원본 경고와 k6 출력에서 확인) |
| VU | maxVUs 80; `vus` 관측값 min=0, max=80 |
| rate/RPS | 설정 80.00 iterations/s, 실측 79.816771 req/s |
| duration | 실제 3m00.0s; 설정 3m0s, gracefulStop 포함 최대 3m30s |
| 총 요청 수 | 14,368 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 27.41ms | `http_req_duration` |
| p50/median | 10.61ms | `http_req_duration`의 `med` |
| p90 | 16.54ms | `http_req_duration` |
| p95 | 26.08ms | `http_req_duration` |
| max | 1.95s | `http_req_duration` |
| error rate | 0.00% (0/14,368) | `http_req_failed`; 목표 부하 달성 여부와는 별개 |
| dropped iterations | 33 (0.183321/s) | `dropped_iterations` |

#### 분석/메모

- **측정 무효에 가까움 — load generator VU allocation이 부족했으므로 동일 80 RPS를 충분한 VU pool로 재측정해야 함.**
- `WARN[0075] Insufficient VUs`가 발생했고 active VUs가 설정 상한 80에 도달했다.
- dropped iterations가 33건 발생했으므로 이 결과를 유효한 80 RPS 기준값이나 개선 전/후 비교값으로 사용하지 않는다.
- checks 28,736/28,736 성공, 실패 0건이며 실행된 HTTP 요청 14,368건의 오류율은 0.00%다. 이는 생성하지 못한 iteration 33건을 포함하지 않는다.
- 설정 rate 80.00 iterations/s에 대해 실측 처리량은 79.816771 req/s다.
- 계산값: 목표 rate 대비 차이는 -0.183229 req/s, 달성률은 약 99.7710%다. `(79.816771 / 80.00) × 100`
- 계산값: 관측된 전체 iteration 시도 중 dropped iterations 비율은 약 0.2292%다. `33 / (14,368 + 33) × 100`
- max 1.95s는 p95 26.08ms의 약 74.77배다(계산값, 1.95s를 1,950ms로 환산).
- 동일 80 RPS를 충분한 VU pool로 재측정할 예정이다.
- 동일 조건의 유효한 After 결과가 없고 이 측정도 재측정 대상이므로 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
$ k6 run capacity-80-before.js
```swift
     /\      Grafana   /‾‾/  
/\  /  \     |\  __   /  /   
```

/  /    \    | |/ /  /   ‾‾\\
/          \   |   (  |  (‾)  |
/ \_\_\_\_\_\_\_\_\_\_ \  |*|\_\  \_*\_\_\_/
```yaml
 execution: local
    script: capacity-80-before.js
    output: -

 scenarios: (100.00%) 1 scenario, 80 max VUs, 3m30s max duration (incl. graceful stop):
          * capacity80: 80.00 iterations/s for 3m0s (maxVUs: 80, gracefulStop: 30s)
```

WARN[0075] Insufficient VUs, reached 80 active VUs and cannot initialize more  executor=constant-arrival-rate scenario=capacity80

█ TOTAL RESULTS
```yaml
checks_total.......: 28736   159.633542/s
checks_succeeded...: 100.00% 28736 out of 28736
checks_failed......: 0.00%   0 out of 28736

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=27.41ms min=8.54ms med=10.61ms max=1.95s p(90)=16.54ms p(95)=26.08ms
  { expected_response:true }...: avg=27.41ms min=8.54ms med=10.61ms max=1.95s p(90)=16.54ms p(95)=26.08ms
http_req_failed................: 0.00%  0 out of 14368
http_reqs......................: 14368  79.816771/s

EXECUTION
dropped_iterations.............: 33     0.183321/s
iteration_duration.............: avg=27.89ms min=8.96ms med=11.02ms max=1.95s p(90)=17.4ms  p(95)=26.73ms
iterations.....................: 14368  79.816771/s
vus............................: 1      min=0          max=80
vus_max........................: 80     min=80         max=80

NETWORK
data_received..................: 547 MB 3.0 MB/s
data_sent......................: 1.9 MB 11 kB/s
```



running (3m00.0s), 00/80 VUs, 14368 complete and 0 interrupted iterations
capacity80 ✓ [======================================] 00/80 VUs  3m0s  80.00 iters/s
````

</details>

### TEST-20260904-008 — `capacity-80-retry-before.js` 80 RPS 재측정

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |
| 실행 스크립트 | `capacity-80-retry-before.js` |
| k6 실행 방식 | local |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | `capacity-80-retry-before.js` 80 RPS 재측정 |
| 목적 | 충분한 VU pool로 80.00 iterations/s 개선 전 처리 용량 재측정 |
| executor | `constant-arrival-rate` (k6 출력 형식에서 식별) |
| VU | maxVUs 200; `vus` 관측값 min=1, max=99 |
| rate/RPS | 설정 80.00 iterations/s, 실측 79.997931 req/s |
| duration | 실제 3m00.0s; 설정 3m0s, gracefulStop 포함 최대 3m30s |
| 총 요청 수 | 14,401 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 36.99ms | `http_req_duration` |
| p50/median | 10.58ms | `http_req_duration`의 `med` |
| p90 | 17.33ms | `http_req_duration` |
| p95 | 59.49ms | `http_req_duration` |
| max | 3s | `http_req_duration` |
| error rate | 0.00% (0/14,401) | `http_req_failed` |
| dropped iterations | 0 | 사용자 확인값; 원본 출력에는 별도 행 없음 |

#### 분석/메모

- TEST-20260904-007에서 부족했던 load generator VU pool을 maxVUs 80에서 200으로 늘려 동일 80 RPS를 재측정했다.
- max active VUs는 99로 VU pool 200의 49.5%였고(계산값), `Insufficient VUs` 경고는 출력되지 않았다.
- checks 28,802/28,802 성공, 실패 0건이며 HTTP 요청 14,401건 모두 성공했다.
- 설정 rate 80.00 iterations/s에 대해 실측 처리량은 79.997931 req/s다.
- 계산값: 목표 rate 대비 차이는 -0.002069 req/s, 달성률은 약 99.9974%다. `(79.997931 / 80.00) × 100`
- max 3s는 p95 59.49ms의 약 50.43배다(계산값, 3s를 3,000ms로 환산). 긴 꼬리 응답이 있었지만 서버 관측값이 없어 원인을 특정하지 않는다.
- dropped iterations는 원본 출력에 별도 행이 없지만 사용자 확인에 따라 0으로 기록했다.
- TEST-20260904-007은 VU 부족 경고와 dropped iterations가 발생한 무효에 가까운 측정이므로 응답 시간 수치를 직접 비교하지 않는다.
- 이 결과는 VU 부족 경고 없이 목표 처리율에 도달한 유효한 80 RPS Before 기준 후보로 사용한다.
- 아직 After 결과가 없으므로 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
$ k6 run capacity-80-retry-before.js
```swift
     /\      Grafana   /‾‾/  
/\  /  \     |\  __   /  /   
```

/  /    \    | |/ /  /   ‾‾\\
/          \   |   (  |  (‾)  |
/ \_\_\_\_\_\_\_\_\_\_ \  |*|\_\  \_*\_\_\_/
```yaml
 execution: local
    script: capacity-80-retry-before.js
    output: -

 scenarios: (100.00%) 1 scenario, 200 max VUs, 3m30s max duration (incl. graceful stop):
          * capacity80: 80.00 iterations/s for 3m0s (maxVUs: 200, gracefulStop: 30s)
```



█ TOTAL RESULTS
```yaml
checks_total.......: 28802   159.995861/s
checks_succeeded...: 100.00% 28802 out of 28802
checks_failed......: 0.00%   0 out of 28802

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=36.99ms min=8.78ms med=10.58ms max=3s p(90)=17.33ms p(95)=59.49ms
  { expected_response:true }...: avg=36.99ms min=8.78ms med=10.58ms max=3s p(90)=17.33ms p(95)=59.49ms
http_req_failed................: 0.00%  0 out of 14401
http_reqs......................: 14401  79.997931/s

EXECUTION
iteration_duration.............: avg=37.53ms min=9.15ms med=11ms    max=3s p(90)=18.92ms p(95)=59.83ms
iterations.....................: 14401  79.997931/s
vus............................: 1      min=1          max=99 
vus_max........................: 200    min=200        max=200

NETWORK
data_received..................: 549 MB 3.1 MB/s
data_sent......................: 2.1 MB 12 kB/s
```



running (3m00.0s), 000/200 VUs, 14401 complete and 0 interrupted iterations
capacity80 ✓ [======================================] 000/200 VUs  3m0s  80.00 iters/s
````

</details>

### TEST-20260904-009 — `capacity-120-before.js` 용량 테스트

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |
| 실행 스크립트 | `capacity-120-before.js` |
| k6 실행 방식 | local |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | `capacity-120-before.js` 용량 테스트 |
| 목적 | 120.00 iterations/s 고정 도착률을 3분간 적용한 개선 전 처리 용량 측정 |
| executor | `constant-arrival-rate` (원본 경고와 k6 출력에서 확인) |
| VU | maxVUs 300; `vus` 관측값 min=1, max=300 |
| rate/RPS | 설정 120.00 iterations/s, 실측 118.615708 req/s |
| duration | 실제 3m01.0s; 설정 3m0s, gracefulStop 포함 최대 3m30s |
| 총 요청 수 | 21,473 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 533.32ms | `http_req_duration` |
| p50/median | 18.37ms | `http_req_duration`의 `med` |
| p90 | 1.82s | `http_req_duration` |
| p95 | 2.87s | `http_req_duration` |
| max | 12.76s | `http_req_duration` |
| error rate | 0.00% (0/21,473) | `http_req_failed`; 목표 부하 달성 여부와는 별개 |
| dropped iterations | 128 (0.707065/s) | `dropped_iterations` |

#### 분석/메모

- **측정 무효에 가까움 — load generator가 maxVUs 300에 도달하고 dropped iterations 128건이 발생했으므로 동일 120 RPS를 충분한 VU pool로 재측정해야 함.**
- `WARN[0121] Insufficient VUs`가 발생했고 active VUs가 설정 상한 300에 도달했다.
- 이 결과를 유효한 120 RPS 기준값이나 개선 전/후 비교값으로 사용하지 않는다.
- checks 42,946/42,946 성공, 실패 0건이며 실행된 HTTP 요청 21,473건의 오류율은 0.00%다. 이는 생성하지 못한 iteration 128건을 포함하지 않는다.
- 설정 rate 120.00 iterations/s에 대해 실측 처리량은 118.615708 req/s다.
- 계산값: 목표 rate 대비 차이는 -1.384292 req/s, 달성률은 약 98.8464%다. `(118.615708 / 120.00) × 100`
- 계산값: 관측된 전체 iteration 시도 중 dropped iterations 비율은 약 0.5926%다. `128 / (21,473 + 128) × 100`
- p95 2.87s는 median 18.37ms의 약 156.23배다(계산값, 2.87s를 2,870ms로 환산). 응답 시간 분포의 긴 꼬리가 크게 증가했지만 서버 관측값이 없어 원인을 특정하지 않는다.
- 동일 120 RPS를 충분한 VU pool로 재측정해야 한다.
- 동일 조건의 유효한 After 결과가 없고 이 측정도 재측정 대상이므로 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
$ k6 run capacity-120-before.js
```swift
     /\      Grafana   /‾‾/  
/\  /  \     |\  __   /  /   
```

/  /    \    | |/ /  /   ‾‾\\
/          \   |   (  |  (‾)  |
/ \_\_\_\_\_\_\_\_\_\_ \  |*|\_\  \_*\_\_\_/
```yaml
 execution: local
    script: capacity-120-before.js
    output: -

 scenarios: (100.00%) 1 scenario, 300 max VUs, 3m30s max duration (incl. graceful stop):
          * capacity120: 120.00 iterations/s for 3m0s (maxVUs: 300, gracefulStop: 30s)
```

WARN[0121] Insufficient VUs, reached 300 active VUs and cannot initialize more  executor=constant-arrival-rate scenario=capacity120

█ TOTAL RESULTS
```yaml
checks_total.......: 42946   237.231417/s
checks_succeeded...: 100.00% 42946 out of 42946
checks_failed......: 0.00%   0 out of 42946

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=533.32ms min=8.4ms  med=18.37ms max=12.76s p(90)=1.82s p(95)=2.87s
  { expected_response:true }...: avg=533.32ms min=8.4ms  med=18.37ms max=12.76s p(90)=1.82s p(95)=2.87s
http_req_failed................: 0.00%  0 out of 21473
http_reqs......................: 21473  118.615708/s

EXECUTION
dropped_iterations.............: 128    0.707065/s
iteration_duration.............: avg=534.62ms min=8.87ms med=18.8ms  max=12.76s p(90)=1.83s p(95)=2.87s
iterations.....................: 21473  118.615708/s
vus............................: 12     min=1          max=300
vus_max........................: 300    min=300        max=300

NETWORK
data_received..................: 819 MB 4.5 MB/s
data_sent......................: 3.1 MB 17 kB/s
```



running (3m01.0s), 000/300 VUs, 21473 complete and 0 interrupted iterations
capacity120 ✓ [======================================] 000/300 VUs  3m0s  120.00 iters/s
````

</details>

### TEST-20260904-010 — `capacity-100-before.js` 용량 테스트

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |
| 실행 스크립트 | `capacity-100-before.js` |
| k6 실행 방식 | local |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | `capacity-100-before.js` 용량 테스트 |
| 목적 | 100.00 iterations/s 고정 도착률을 3분간 적용한 개선 전 처리 용량 측정 |
| executor | `constant-arrival-rate` (k6 출력 형식에서 식별) |
| VU | maxVUs 400; `vus` 관측값 min=1, max=97 |
| rate/RPS | 설정 100.00 iterations/s, 실측 99.99412 req/s |
| duration | 실제 3m00.0s; 설정 3m0s, gracefulStop 포함 최대 3m30s |
| 총 요청 수 | 18,001 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 46.77ms | `http_req_duration` |
| p50/median | 13.16ms | `http_req_duration`의 `med` |
| p90 | 32.75ms | `http_req_duration` |
| p95 | 219.85ms | `http_req_duration` |
| max | 2.02s | `http_req_duration` |
| error rate | 0.00% (0/18,001) | `http_req_failed` |
| dropped iterations | 0 | 전체 k6 세션 출력에 별도 행 없음 |

#### 분석/메모

- checks 36,002/36,002 성공, 실패 0건이며 HTTP 요청 18,001건 모두 성공했다.
- 설정 rate 100.00 iterations/s에 대해 실측 처리량은 99.99412 req/s다.
- 계산값: 목표 rate 대비 차이는 -0.00588 req/s, 달성률은 99.9941%다. `(99.99412 / 100.00) × 100`
- max active VUs는 97로 VU pool 400의 24.25%였고(계산값), `Insufficient VUs` 경고는 출력되지 않았다.
- p95 219.85ms는 median 13.16ms의 약 16.71배다(계산값). 긴 꼬리 응답이 관측됐지만 서버 관측값이 없어 원인을 특정하지 않는다.
- dropped iterations는 전체 세션 출력에 별도 행이 없으므로 사용자 지정 판정 기준에 따라 0으로 기록했다.
- 이 결과는 VU 부족 경고 없이 목표 처리율에 도달한 유효한 100 RPS Before 기준 후보로 사용한다.
- 아직 After 결과가 없으므로 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
$ k6 run capacity-100-before.js
```swift
     /\      Grafana   /‾‾/  
/\  /  \     |\  __   /  /   
```

/  /    \    | |/ /  /   ‾‾\\
/          \   |   (  |  (‾)  |
/ \_\_\_\_\_\_\_\_\_\_ \  |*|\_\  \_*\_\_\_/
```yaml
 execution: local
    script: capacity-100-before.js
    output: -

 scenarios: (100.00%) 1 scenario, 400 max VUs, 3m30s max duration (incl. graceful stop):
          * capacity100: 100.00 iterations/s for 3m0s (maxVUs: 400, gracefulStop: 30s)
```



█ TOTAL RESULTS
```yaml
checks_total.......: 36002   199.98824/s
checks_succeeded...: 100.00% 36002 out of 36002
checks_failed......: 0.00%   0 out of 36002

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=46.77ms min=8.17ms med=13.16ms max=2.02s p(90)=32.75ms p(95)=219.85ms
  { expected_response:true }...: avg=46.77ms min=8.17ms med=13.16ms max=2.02s p(90)=32.75ms p(95)=219.85ms
http_req_failed................: 0.00%  0 out of 18001
http_reqs......................: 18001  99.99412/s

EXECUTION
iteration_duration.............: avg=47.4ms  min=8.61ms med=13.7ms  max=2.02s p(90)=33.79ms p(95)=220.23ms
iterations.....................: 18001  99.99412/s
vus............................: 2      min=1          max=97 
vus_max........................: 400    min=400        max=400

NETWORK
data_received..................: 687 MB 3.8 MB/s
data_sent......................: 2.8 MB 16 kB/s
```



running (3m00.0s), 000/400 VUs, 18001 complete and 0 interrupted iterations
capacity100 ✓ [======================================] 000/400 VUs  3m0s  100.00 iters/s
````

</details>

### TEST-20260904-011 — 110 RPS 용량 테스트

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | 110 RPS 용량 테스트 |
| 목적 | 110.00 iterations/s 개선 전 처리 용량 측정 |
| VU | maxVUs 600; `vus` 관측값 min=1, max=183 |
| rate/RPS | 설정 110.00 iterations/s, 실측 109.998623 req/s |
| duration | 실제 3m00.0s |
| 총 요청 수 | 19,801 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 97.32ms | `http_req_duration` |
| p50/median | 13.64ms | `http_req_duration`의 `med` |
| p90 | 85.89ms | `http_req_duration` |
| p95 | 460.86ms | `http_req_duration` |
| max | 4.62s | `http_req_duration` |
| error rate | 0.00% (0/19,801) | `http_req_failed` |
| dropped iterations | 0 | 전체 k6 세션 출력에 별도 행 없음 |

#### 분석/메모

- checks 39,602/39,602 성공, 실패 0건이며 HTTP 요청 19,801건 모두 성공했다.
- 설정 rate 110.00 iterations/s에 대해 실측 처리량은 109.998623 req/s다.
- 계산값: 목표 rate 대비 차이는 -0.001377 req/s, 달성률은 약 99.9987%다. `(109.998623 / 110.00) × 100`
- max active VUs는 183으로 VU pool 600의 30.50%다(계산값).
- p95 460.86ms는 median 13.64ms의 약 33.79배다(계산값). 서버 관측값이 없어 긴 꼬리 응답의 원인은 특정하지 않는다.
- dropped iterations는 전체 세션 출력에 별도 행이 없으므로 0으로 기록했다.
- 이 결과는 목표 처리율에 도달한 유효한 110 RPS Before 기준 후보로 사용한다.
- 아직 After 결과가 없으므로 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
\================ 110 RPS ================
█ TOTAL RESULTS
```yaml
checks_total.......: 39602   219.997246/s
checks_succeeded...: 100.00% 39602 out of 39602
checks_failed......: 0.00%   0 out of 39602

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=97.32ms min=8.24ms med=13.64ms max=4.62s p(90)=85.89ms p(95)=460.86ms
  { expected_response:true }...: avg=97.32ms min=8.24ms med=13.64ms max=4.62s p(90)=85.89ms p(95)=460.86ms
http_req_failed................: 0.00%  0 out of 19801
http_reqs......................: 19801  109.998623/s

EXECUTION
iteration_duration.............: avg=98.06ms min=8.73ms med=14.07ms max=4.62s p(90)=88.34ms p(95)=462.59ms
iterations.....................: 19801  109.998623/s
vus............................: 1      min=1          max=183
vus_max........................: 600    min=600        max=600

NETWORK
data_received..................: 756 MB 4.2 MB/s
data_sent......................: 3.4 MB 19 kB/s
```



running (3m00.0s), 000/600 VUs, 19801 complete and 0 interrupted iterations
capacity ✓ [ 100% ] 000/600 VUs  3m0s  110.00 iters/s
````

</details>

### TEST-20260904-012 — 115 RPS 용량 테스트

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | 115 RPS 용량 테스트 |
| 목적 | 115.00 iterations/s 개선 전 처리 용량 측정 |
| VU | maxVUs 600; `vus` 관측값 min=1, max=156 |
| rate/RPS | 설정 115.00 iterations/s, 실측 114.997821 req/s |
| duration | 실제 3m00.0s |
| 총 요청 수 | 20,701 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 68.5ms | `http_req_duration` |
| p50/median | 14.72ms | `http_req_duration`의 `med` |
| p90 | 89.67ms | `http_req_duration` |
| p95 | 308.72ms | `http_req_duration` |
| max | 3.1s | `http_req_duration` |
| error rate | 0.00% (0/20,701) | `http_req_failed` |
| dropped iterations | 0 | 전체 k6 세션 출력에 별도 행 없음 |

#### 분석/메모

- checks 41,402/41,402 성공, 실패 0건이며 HTTP 요청 20,701건 모두 성공했다.
- 설정 rate 115.00 iterations/s에 대해 실측 처리량은 114.997821 req/s다.
- 계산값: 목표 rate 대비 차이는 -0.002179 req/s, 달성률은 약 99.9981%다. `(114.997821 / 115.00) × 100`
- max active VUs는 156으로 VU pool 600의 26.00%다(계산값).
- p95 308.72ms는 median 14.72ms의 약 20.97배다(계산값). 서버 관측값이 없어 긴 꼬리 응답의 원인은 특정하지 않는다.
- dropped iterations는 전체 세션 출력에 별도 행이 없으므로 0으로 기록했다.
- 이 결과는 목표 처리율에 도달한 유효한 115 RPS Before 기준 후보로 사용한다.
- 아직 After 결과가 없으므로 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
\================ 115 RPS ================
█ TOTAL RESULTS
```yaml
checks_total.......: 41402   229.995641/s
checks_succeeded...: 100.00% 41402 out of 41402
checks_failed......: 0.00%   0 out of 41402

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=68.5ms  min=8.3ms  med=14.72ms max=3.1s p(90)=89.67ms p(95)=308.72ms
  { expected_response:true }...: avg=68.5ms  min=8.3ms  med=14.72ms max=3.1s p(90)=89.67ms p(95)=308.72ms
http_req_failed................: 0.00%  0 out of 20701
http_reqs......................: 20701  114.997821/s

EXECUTION
iteration_duration.............: avg=69.26ms min=8.64ms med=15.16ms max=3.1s p(90)=92.51ms p(95)=310.23ms
iterations.....................: 20701  114.997821/s
vus............................: 1      min=1          max=156
vus_max........................: 600    min=600        max=600

NETWORK
data_received..................: 790 MB 4.4 MB/s
data_sent......................: 3.5 MB 19 kB/s
```



running (3m00.0s), 000/600 VUs, 20701 complete and 0 interrupted iterations
capacity ✓ [ 100% ] 000/600 VUs  3m0s  115.00 iters/s
````

</details>

### TEST-20260904-013 — 120 RPS 재측정

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | 120 RPS 재측정 |
| 목적 | VU pool 600으로 120.00 iterations/s 개선 전 처리 용량 재측정 |
| VU | maxVUs 600; `vus` 관측값 min=2, max=203 |
| rate/RPS | 설정 120.00 iterations/s, 실측 119.995243 req/s |
| duration | 실제 3m00.0s |
| 총 요청 수 | 21,600 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 126.64ms | `http_req_duration` |
| p50/median | 15.62ms | `http_req_duration`의 `med` |
| p90 | 222.82ms | `http_req_duration` |
| p95 | 706.98ms | `http_req_duration` |
| max | 6.04s | `http_req_duration` |
| error rate | 0.00% (0/21,600) | `http_req_failed` |
| dropped iterations | 0 | 전체 k6 세션 출력에 별도 행 없음 |

#### 분석/메모

- TEST-20260904-009에서 부족했던 VU pool을 maxVUs 300에서 600으로 늘려 120 RPS를 재측정했다.
- checks 43,200/43,200 성공, 실패 0건이며 HTTP 요청 21,600건 모두 성공했다.
- 설정 rate 120.00 iterations/s에 대해 실측 처리량은 119.995243 req/s다.
- 계산값: 목표 rate 대비 차이는 -0.004757 req/s, 달성률은 약 99.9960%다. `(119.995243 / 120.00) × 100`
- max active VUs는 203으로 VU pool 600의 약 33.83%다(계산값).
- p95 706.98ms는 median 15.62ms의 약 45.26배다(계산값). 서버 관측값이 없어 긴 꼬리 응답의 원인은 특정하지 않는다.
- dropped iterations는 전체 세션 출력에 별도 행이 없으므로 0으로 기록했다.
- TEST-20260904-009는 maxVUs 300 도달과 dropped iterations가 발생한 무효에 가까운 측정이므로 응답 시간 수치를 직접 비교하지 않는다.
- 이 결과는 목표 처리율에 도달한 유효한 120 RPS Before 기준 후보로 사용한다.
- 아직 After 결과가 없으므로 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
\================ 120 RPS ================
█ TOTAL RESULTS
```yaml
checks_total.......: 43200   239.990487/s
checks_succeeded...: 100.00% 43200 out of 43200
checks_failed......: 0.00%   0 out of 43200

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=126.64ms min=8.51ms med=15.62ms max=6.04s p(90)=222.82ms p(95)=706.98ms
  { expected_response:true }...: avg=126.64ms min=8.51ms med=15.62ms max=6.04s p(90)=222.82ms p(95)=706.98ms
http_req_failed................: 0.00%  0 out of 21600
http_reqs......................: 21600  119.995243/s

EXECUTION
iteration_duration.............: avg=127.4ms  min=8.93ms med=16.04ms max=6.04s p(90)=226.7ms  p(95)=707.48ms
iterations.....................: 21600  119.995243/s
vus............................: 2      min=2          max=203
vus_max........................: 600    min=600        max=600

NETWORK
data_received..................: 824 MB 4.6 MB/s
data_sent......................: 3.6 MB 20 kB/s
```



running (3m00.0s), 000/600 VUs, 21600 complete and 0 interrupted iterations
capacity ✓ [ 100% ] 000/600 VUs  3m0s  120.00 iters/s
````

</details>

### TEST-20260904-014 — 130 RPS 용량 테스트

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | 130 RPS 용량 테스트 |
| 목적 | 130 RPS 목표의 개선 전 처리 용량 측정 |
| VU | maxVUs 600; `vus` 관측값 min=1, max=344 |
| rate/RPS | 목표 130 RPS, 실측 129.993871 req/s |
| duration | 요약 집계 구간 약 180.01s(계산값) |
| 총 요청 수 | 23,400 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 878.79ms | `http_req_duration` |
| p50/median | 329.57ms | `http_req_duration`의 `med` |
| p90 | 2.55s | `http_req_duration` |
| p95 | 3.38s | `http_req_duration` |
| max | 7.69s | `http_req_duration` |
| error rate | 0.00% (0/23,400) | `http_req_failed` |
| dropped iterations | 0 | 전체 k6 세션 출력에 별도 행 없음 |

#### 분석/메모

- checks 46,800/46,800 성공, 실패 0건이며 HTTP 요청 23,400건 모두 성공했다.
- 목표 130 RPS에 대해 실측 처리량은 129.993871 req/s다.
- 계산값: 목표 rate 대비 차이는 -0.006129 req/s, 달성률은 약 99.9953%다. `(129.993871 / 130) × 100`
- 계산값: 총 요청 수와 실측 RPS로 산출한 요약 집계 구간은 약 180.01초다. `23,400 / 129.993871`
- max active VUs는 344로 VU pool 600의 약 57.33%다(계산값).
- p95 3.38s는 median 329.57ms의 약 10.26배다(계산값). 목표 처리율은 달성했지만 응답 지연이 크게 증가했다.
- dropped iterations는 전체 세션 출력에 별도 행이 없으므로 0으로 기록했다.
- 이 결과는 목표 처리율에 도달한 유효한 130 RPS Before 기준 후보로 사용한다.
- 아직 After 결과가 없으므로 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
\================ 130 RPS ================
█ TOTAL RESULTS
```yaml
checks_total.......: 46800   259.987742/s
checks_succeeded...: 100.00% 46800 out of 46800
checks_failed......: 0.00%   0 out of 46800

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=878.79ms min=8.96ms med=329.57ms max=7.69s p(90)=2.55s p(95)=3.38s
  { expected_response:true }...: avg=878.79ms min=8.96ms med=329.57ms max=7.69s p(90)=2.55s p(95)=3.38s
http_req_failed................: 0.00%  0 out of 23400
http_reqs......................: 23400  129.993871/s

EXECUTION
iteration_duration.............: avg=879.46ms min=9.37ms med=330.03ms max=7.69s p(90)=2.55s p(95)=3.38s
iterations.....................: 23400  129.993871/s
vus............................: 2      min=1          max=344
vus_max........................: 600    min=600        max=600

NETWORK
data_received..................: 893 MB 5.0 MB/s
data_sent......................: 3.8 MB 21 kB/s
```
````

</details>

### TEST-20260904-015 — 140 RPS 용량 테스트

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | 140 RPS 용량 테스트 |
| 목적 | 140 RPS 목표의 개선 전 처리 용량 측정 |
| VU | maxVUs 600; `vus` 관측값 min=40, max=600 |
| rate/RPS | 목표 140 RPS, 실측 135.075594 req/s |
| duration | 요약 집계 구간 약 184.02s(계산값) |
| 총 요청 수 | 24,857 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 3.64s | `http_req_duration` |
| p50/median | 3.96s | `http_req_duration`의 `med` |
| p90 | 5.67s | `http_req_duration` |
| p95 | 5.85s | `http_req_duration` |
| max | 9.9s | `http_req_duration` |
| error rate | 0.00% (0/24,857) | `http_req_failed`; 목표 부하 달성 여부와는 별개 |
| dropped iterations | 344 (1.869333/s) | `dropped_iterations` |

#### 분석/메모

- **측정 무효에 가까움 — max active VUs가 VU pool 600에 도달하고 dropped iterations 344건이 발생해 목표 140 RPS를 유지하지 못했다.**
- checks 49,714/49,714 성공, 실패 0건이며 실행된 HTTP 요청 24,857건의 오류율은 0.00%다. 이는 생성하지 못한 iteration 344건을 포함하지 않는다.
- 목표 140 RPS에 대해 실측 처리량은 135.075594 req/s다.
- 계산값: 목표 rate 대비 차이는 -4.924406 req/s, 달성률은 약 96.4826%다. `(135.075594 / 140) × 100`
- 계산값: 관측된 전체 iteration 시도 중 dropped iterations 비율은 약 1.3650%다. `344 / (24,857 + 344) × 100`
- 계산값: 총 요청 수와 실측 RPS로 산출한 요약 집계 구간은 약 184.02초다. `24,857 / 135.075594`
- median 3.96s, p95 5.85s로 요청 대부분의 지연이 초 단위까지 증가했다.
- 서버 관측값이 없어 처리율 제한과 지연 증가의 원인을 특정하지 않는다.
- 이 결과를 유효한 140 RPS 기준값이나 개선 전/후 비교값으로 사용하지 않는다.
- 아직 After 결과가 없으므로 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
\================ 140 RPS ================
█ TOTAL RESULTS
```yaml
checks_total.......: 49714   270.151188/s
checks_succeeded...: 100.00% 49714 out of 49714
checks_failed......: 0.00%   0 out of 49714

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=3.64s min=25.38ms med=3.96s max=9.9s p(90)=5.67s p(95)=5.85s
  { expected_response:true }...: avg=3.64s min=25.38ms med=3.96s max=9.9s p(90)=5.67s p(95)=5.85s
http_req_failed................: 0.00%  0 out of 24857
http_reqs......................: 24857  135.075594/s

EXECUTION
dropped_iterations.............: 344    1.869333/s
iteration_duration.............: avg=3.64s min=39.78ms med=3.96s max=9.9s p(90)=5.67s p(95)=5.85s
iterations.....................: 24857  135.075594/s
vus............................: 41     min=40         max=600
vus_max........................: 600    min=600        max=600

NETWORK
data_received..................: 948 MB 5.2 MB/s
data_sent......................: 4.0 MB 22 kB/s
```
````

</details>

### TEST-20260904-016 — 150 RPS 용량 테스트

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | 150 RPS 용량 테스트 |
| 목적 | 150 RPS 목표의 개선 전 처리 용량 측정 |
| VU | maxVUs 600; `vus` 관측값 min=29, max=600 |
| rate/RPS | 목표 150 RPS, 실측 135.589667 req/s |
| duration | 요약 집계 구간 약 184.98s(계산값) |
| 총 요청 수 | 25,081 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 4.05s | `http_req_duration` |
| p50/median | 4.22s | `http_req_duration`의 `med` |
| p90 | 5.84s | `http_req_duration` |
| p95 | 6.06s | `http_req_duration` |
| max | 10.95s | `http_req_duration` |
| error rate | 0.00% (0/25,081) | `http_req_failed`; 목표 부하 달성 여부와는 별개 |
| dropped iterations | 1,920 (10.379656/s) | `dropped_iterations` |

#### 분석/메모

- **측정 무효에 가까움 — max active VUs가 VU pool 600에 도달하고 dropped iterations 1,920건이 발생해 목표 150 RPS를 유지하지 못했다.**
- checks 50,162/50,162 성공, 실패 0건이며 실행된 HTTP 요청 25,081건의 오류율은 0.00%다. 이는 생성하지 못한 iteration 1,920건을 포함하지 않는다.
- 목표 150 RPS에 대해 실측 처리량은 135.589667 req/s다.
- 계산값: 목표 rate 대비 차이는 -14.410333 req/s, 달성률은 약 90.3931%다. `(135.589667 / 150) × 100`
- 계산값: 관측된 전체 iteration 시도 중 dropped iterations 비율은 약 7.1108%다. `1,920 / (25,081 + 1,920) × 100`
- 계산값: 총 요청 수와 실측 RPS로 산출한 요약 집계 구간은 약 184.98초다. `25,081 / 135.589667`
- median 4.22s, p95 6.06s로 요청 대부분의 지연이 초 단위까지 증가했다.
- 실측 처리량은 140 RPS 테스트의 135.075594 req/s와 비슷하지만 목표 rate 증가분만큼 dropped iterations가 크게 늘었다. 두 실행 모두 VU pool 상한에 도달했으므로 서버의 정확한 최대 처리량으로 단정하지 않는다.
- 서버 관측값이 없어 처리율 제한과 지연 증가의 원인을 특정하지 않는다.
- 이 결과를 유효한 150 RPS 기준값이나 개선 전/후 비교값으로 사용하지 않는다.
- 아직 After 결과가 없으므로 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
\================ 150 RPS ================
█ TOTAL RESULTS
```yaml
checks_total.......: 50162   271.179333/s
checks_succeeded...: 100.00% 50162 out of 50162
checks_failed......: 0.00%   0 out of 50162

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=4.05s min=18.15ms med=4.22s max=10.95s p(90)=5.84s p(95)=6.06s
  { expected_response:true }...: avg=4.05s min=18.15ms med=4.22s max=10.95s p(90)=5.84s p(95)=6.06s
http_req_failed................: 0.00%  0 out of 25081
http_reqs......................: 25081  135.589667/s

EXECUTION
dropped_iterations.............: 1920   10.379656/s
iteration_duration.............: avg=4.06s min=28.57ms med=4.22s max=10.95s p(90)=5.84s p(95)=6.06s
iterations.....................: 25081  135.589667/s
vus............................: 29     min=29         max=600
vus_max........................: 600    min=600        max=600

NETWORK
data_received..................: 957 MB 5.2 MB/s
data_sent......................: 4.0 MB 22 kB/s
```
````

</details>

### TEST-20260904-017 — `capacity-before.js` 125 RPS 용량 테스트

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |
| 실행 스크립트 | `capacity-before.js` |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | `capacity-before.js` 125 RPS 용량 테스트 |
| 목적 | 125 RPS 목표의 개선 전 처리 용량 측정 |
| VU | `PRE_VUS=600`; `vus` 관측값 min=1, max=191 |
| rate/RPS | `RATE=125`, 실측 124.99713 req/s |
| duration | 요약 집계 구간 약 180.01s(계산값) |
| 총 요청 수 | 22,501 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 194.18ms | `http_req_duration` |
| p50/median | 19.35ms | `http_req_duration`의 `med` |
| p90 | 639.57ms | `http_req_duration` |
| p95 | 1.22s | `http_req_duration` |
| max | 4.32s | `http_req_duration` |
| error rate | 0.00% (0/22,501) | `http_req_failed` |
| dropped iterations | 0 | 전체 k6 세션 출력에 별도 행 없음 |

#### 분석/메모

- checks 45,002/45,002 성공, 실패 0건이며 HTTP 요청 22,501건 모두 성공했다.
- 목표 125 RPS에 대해 실측 처리량은 124.99713 req/s다.
- 계산값: 목표 rate 대비 차이는 -0.00287 req/s, 달성률은 약 99.9977%다. `(124.99713 / 125) × 100`
- 계산값: 총 요청 수와 실측 RPS로 산출한 요약 집계 구간은 약 180.01초다. `22,501 / 124.99713`
- max active VUs는 191로 VU pool 600의 약 31.83%다(계산값).
- p95 1.22s는 median 19.35ms의 약 63.05배다(계산값, 1.22s를 1,220ms로 환산). 서버 관측값이 없어 긴 꼬리 응답의 원인은 특정하지 않는다.
- dropped iterations는 전체 세션 출력에 별도 행이 없으므로 0으로 기록했다.
- 이 결과는 목표 처리율에 도달한 유효한 125 RPS Before 기준 후보로 사용한다.
- 아직 After 결과가 없으므로 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
RATE=125 PRE\_VUS=600 \
k6 run --quiet capacity-before.js \
\| tee results/capacity-125-before.txt$ $ > >

█ TOTAL RESULTS
```yaml
checks_total.......: 45002   249.994261/s
checks_succeeded...: 100.00% 45002 out of 45002
checks_failed......: 0.00%   0 out of 45002

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=194.18ms min=8.72ms med=19.35ms max=4.32s p(90)=639.57ms p(95)=1.22s
  { expected_response:true }...: avg=194.18ms min=8.72ms med=19.35ms max=4.32s p(90)=639.57ms p(95)=1.22s
http_req_failed................: 0.00%  0 out of 22501
http_reqs......................: 22501  124.99713/s

EXECUTION
iteration_duration.............: avg=194.89ms min=9.22ms med=19.76ms max=4.32s p(90)=644.18ms p(95)=1.22s
iterations.....................: 22501  124.99713/s
vus............................: 1      min=1          max=191
vus_max........................: 600    min=600        max=600

NETWORK
data_received..................: 859 MB 4.8 MB/s
data_sent......................: 3.7 MB 21 kB/s
```



$
````

</details>

### TEST-20260904-018 — CPU 관측용 100 RPS 테스트

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |
| CPU 관측 시작 | 2026-09-04T12:31:15Z |
| CPU 관측 종료 | 2026-09-04T12:34:15Z |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | CPU 관측용 100 RPS 테스트 |
| 목적 | 100 RPS 부하 구간의 서버 CPU 지표 확인 |
| VU | maxVUs 600; `vus` 관측값 min=1, max=51 |
| rate/RPS | `RATE=100`, 실측 99.999477 req/s |
| duration | 3m0s (`START_UTC`~`END_UTC`) |
| 총 요청 수 | 18,001 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 19.32ms | `http_req_duration` |
| p50/median | 10.16ms | `http_req_duration`의 `med` |
| p90 | 17.73ms | `http_req_duration` |
| p95 | 29.14ms | `http_req_duration` |
| max | 1.11s | `http_req_duration` |
| error rate | 0.00% (0/18,001) | `http_req_failed` |
| dropped iterations | 0 | 전체 k6 세션 출력에 별도 행 없음 |

#### 서버 관측값

관측 구간: `2026-09-04T12:31:15Z`~`2026-09-04T12:34:15Z`

| 지표 | Avg | Peak | Samples |
|---|---:|---:|---:|
| `cpu_usage_active` | 56.48144294010355% | 72.07298530031755% | 19 |
| `cpu_usage_user` | 40.38244784203341% | 54.33350228082943% | 19 |
| `cpu_usage_system` | 8.975864090393697% | 10.12658227824225% | 19 |
| `cpu_usage_iowait` | 0.024058248046483054% | 0.20273694880950366% | 19 |
| `mem_used_percent` | 62.3050137444646% | 63.036151584396016% | 19 |
| `swap_used_percent` | 23.364824388570625% | 23.5363455511962% | 19 |

#### 분석/메모

- 서버 CPU 지표 확인을 위해 UTC 구간 2026-09-04T12:31:15Z~2026-09-04T12:34:15Z를 기록했다.
- checks 36,002/36,002 성공, 실패 0건이며 HTTP 요청 18,001건 모두 성공했다.
- 목표 100 RPS에 대해 실측 처리량은 99.999477 req/s다.
- 계산값: 목표 rate 대비 차이는 -0.000523 req/s, 달성률은 약 99.9995%다. `(99.999477 / 100) × 100`
- max active VUs는 51로 VU pool 600의 8.50%다(계산값).
- p95 29.14ms는 median 10.16ms의 약 2.87배다(계산값).
- dropped iterations는 전체 세션 출력에 별도 행이 없으므로 0으로 기록했다.
- k6 출력에는 서버 CPU 값이 포함되지 않으며, 위 UTC 구간을 AWS 지표와 매칭해야 한다.
- 이후 제공된 하드웨어 지표의 관측 구간이 테스트 UTC 구간과 일치하며, `cpu_usage_active`는 평균 56.48144294010355%, 최고 72.07298530031755%였다.
- 아직 After 결과가 없으므로 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
\================ 100 RPS TIME ================
RATE=100
START\_UTC=2026-09-04T12:31:15Z
END\_UTC=2026-09-04T12:34:15Z

\================ 100 RPS RESULT ================
█ TOTAL RESULTS
```yaml
checks_total.......: 36002   199.998954/s
checks_succeeded...: 100.00% 36002 out of 36002
checks_failed......: 0.00%   0 out of 36002

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=19.32ms min=8.34ms med=10.16ms max=1.11s p(90)=17.73ms p(95)=29.14ms
  { expected_response:true }...: avg=19.32ms min=8.34ms med=10.16ms max=1.11s p(90)=17.73ms p(95)=29.14ms
http_req_failed................: 0.00%  0 out of 18001
http_reqs......................: 18001  99.999477/s

EXECUTION
iteration_duration.............: avg=20.01ms min=8.71ms med=10.58ms max=1.11s p(90)=18.96ms p(95)=30.82ms
iterations.....................: 18001  99.999477/s
vus............................: 1      min=1          max=51 
vus_max........................: 600    min=600        max=600

NETWORK
data_received..................: 688 MB 3.8 MB/s
data_sent......................: 3.2 MB 18 kB/s
```
````

</details>

### TEST-20260904-019 — CPU 관측용 120 RPS 테스트

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |
| CPU 관측 시작 | 2026-09-04T12:36:15Z |
| CPU 관측 종료 | 2026-09-04T12:39:16Z |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | CPU 관측용 120 RPS 테스트 |
| 목적 | 120 RPS 부하 구간의 서버 CPU 지표 확인 |
| VU | maxVUs 600; `vus` 관측값 min=1, max=89 |
| rate/RPS | `RATE=120`, 실측 119.997562 req/s |
| duration | CPU 관측 구간 3m1s; k6 요약 집계 구간 약 180.01s(계산값) |
| 총 요청 수 | 21,601 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 26.04ms | `http_req_duration` |
| p50/median | 12.33ms | `http_req_duration`의 `med` |
| p90 | 23.92ms | `http_req_duration` |
| p95 | 51.08ms | `http_req_duration` |
| max | 1.63s | `http_req_duration` |
| error rate | 0.00% (0/21,601) | `http_req_failed` |
| dropped iterations | 0 | 전체 k6 세션 출력에 별도 행 없음 |

#### 서버 관측값

관측 구간: `2026-09-04T12:36:15Z`~`2026-09-04T12:39:16Z`

| 지표 | Avg | Peak | Samples |
|---|---:|---:|---:|
| `cpu_usage_active` | 66.98039887654825% | 80.14184396987453% | 19 |
| `cpu_usage_user` | 48.25143739755402% | 54.42908346191776% | 19 |
| `cpu_usage_system` | 11.189052635779776% | 12.749615975555066% | 19 |
| `cpu_usage_iowait` | 0.026780982352065146% | 0.25329280648119745% | 19 |
| `mem_used_percent` | 61.91740130689879% | 62.34633119568868% | 19 |
| `swap_used_percent` | 23.34105275605878% | 23.34561032411637% | 19 |

#### 분석/메모

- 서버 CPU 지표 확인을 위해 UTC 구간 2026-09-04T12:36:15Z~2026-09-04T12:39:16Z를 기록했다.
- checks 43,202/43,202 성공, 실패 0건이며 HTTP 요청 21,601건 모두 성공했다.
- 목표 120 RPS에 대해 실측 처리량은 119.997562 req/s다.
- 계산값: 목표 rate 대비 차이는 -0.002438 req/s, 달성률은 약 99.9980%다. `(119.997562 / 120) × 100`
- 계산값: 총 요청 수와 실측 RPS로 산출한 k6 요약 집계 구간은 약 180.01초다. `21,601 / 119.997562`
- max active VUs는 89로 VU pool 600의 약 14.83%다(계산값).
- p95 51.08ms는 median 12.33ms의 약 4.14배다(계산값).
- dropped iterations는 전체 세션 출력에 별도 행이 없으므로 0으로 기록했다.
- k6 출력에는 서버 CPU 값이 포함되지 않으며, 위 UTC 구간을 AWS 지표와 매칭해야 한다.
- 이후 제공된 하드웨어 지표의 관측 구간이 테스트 UTC 구간과 일치하며, `cpu_usage_active`는 평균 66.98039887654825%, 최고 80.14184396987453%였다.
- 아직 After 결과가 없으므로 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
\================ 120 RPS TIME ================
RATE=120
START\_UTC=2026-09-04T12:36:15Z
END\_UTC=2026-09-04T12:39:16Z

\================ 120 RPS RESULT ================
█ TOTAL RESULTS
```yaml
checks_total.......: 43202   239.995124/s
checks_succeeded...: 100.00% 43202 out of 43202
checks_failed......: 0.00%   0 out of 43202

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=26.04ms min=8.27ms med=12.33ms max=1.63s p(90)=23.92ms p(95)=51.08ms
  { expected_response:true }...: avg=26.04ms min=8.27ms med=12.33ms max=1.63s p(90)=23.92ms p(95)=51.08ms
http_req_failed................: 0.00%  0 out of 21601
http_reqs......................: 21601  119.997562/s

EXECUTION
iteration_duration.............: avg=26.69ms min=8.62ms med=12.74ms max=1.63s p(90)=25.25ms p(95)=55.42ms
iterations.....................: 21601  119.997562/s
vus............................: 2      min=1          max=89 
vus_max........................: 600    min=600        max=600

NETWORK
data_received..................: 825 MB 4.6 MB/s
data_sent......................: 3.6 MB 20 kB/s
```
````

</details>

### TEST-20260904-020 — CPU 관측용 125 RPS 테스트

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |
| CPU 관측 시작 | 2026-09-04T12:41:16Z |
| CPU 관측 종료 | 2026-09-04T12:44:16Z |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | CPU 관측용 125 RPS 테스트 |
| 목적 | 125 RPS 부하 구간의 서버 CPU 지표 확인 |
| VU | maxVUs 600; `vus` 관측값 min=1, max=208 |
| rate/RPS | `RATE=125`, 실측 124.99706 req/s |
| duration | CPU 관측 구간 3m0s; k6 요약 집계 구간 약 180.01s(계산값) |
| 총 요청 수 | 22,501 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 61.62ms | `http_req_duration` |
| p50/median | 12.23ms | `http_req_duration`의 `med` |
| p90 | 30.32ms | `http_req_duration` |
| p95 | 132.52ms | `http_req_duration` |
| max | 4.03s | `http_req_duration` |
| error rate | 0.00% (0/22,501) | `http_req_failed` |
| dropped iterations | 0 | 전체 k6 세션 출력에 별도 행 없음 |

#### 서버 관측값

관측 구간: `2026-09-04T12:41:16Z`~`2026-09-04T12:44:16Z`

| 지표 | Avg | Peak | Samples |
|---|---:|---:|---:|
| `cpu_usage_active` | 68.31170737076653% | 88.12531581562047% | 19 |
| `cpu_usage_user` | 49.181311069959484% | 61.79888832731956% | 19 |
| `cpu_usage_system` | 11.737569658459682% | 14.805457301631918% | 19 |
| `cpu_usage_iowait` | 0.0026825473470066218% | 0.05096839959312581% | 19 |
| `mem_used_percent` | 62.93041167010009% | 64.04661867203944% | 19 |
| `swap_used_percent` | 23.335441124904168% | 23.33759944457902% | 19 |

#### 분석/메모

- 서버 CPU 지표 확인을 위해 UTC 구간 2026-09-04T12:41:16Z~2026-09-04T12:44:16Z를 기록했다.
- checks 45,002/45,002 성공, 실패 0건이며 HTTP 요청 22,501건 모두 성공했다.
- 목표 125 RPS에 대해 실측 처리량은 124.99706 req/s다.
- 계산값: 목표 rate 대비 차이는 -0.00294 req/s, 달성률은 약 99.9976%다. `(124.99706 / 125) × 100`
- 계산값: 총 요청 수와 실측 RPS로 산출한 k6 요약 집계 구간은 약 180.01초다. `22,501 / 124.99706`
- max active VUs는 208로 VU pool 600의 약 34.67%다(계산값).
- p95 132.52ms는 median 12.23ms의 약 10.84배다(계산값).
- dropped iterations는 전체 세션 출력에 별도 행이 없으므로 0으로 기록했다.
- k6 출력에는 서버 CPU 값이 포함되지 않으며, 위 UTC 구간을 AWS 지표와 매칭해야 한다.
- 이후 제공된 하드웨어 지표의 관측 구간이 테스트 UTC 구간과 일치하며, `cpu_usage_active`는 평균 68.31170737076653%, 최고 88.12531581562047%였다.
- 아직 After 결과가 없으므로 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
\================ 125 RPS TIME ================
RATE=125
START\_UTC=2026-09-04T12:41:16Z
END\_UTC=2026-09-04T12:44:16Z

\================ 125 RPS RESULT ================
█ TOTAL RESULTS
```yaml
checks_total.......: 45002   249.99412/s
checks_succeeded...: 100.00% 45002 out of 45002
checks_failed......: 0.00%   0 out of 45002

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=61.62ms min=8.29ms med=12.23ms max=4.03s p(90)=30.32ms p(95)=132.52ms
  { expected_response:true }...: avg=61.62ms min=8.29ms med=12.23ms max=4.03s p(90)=30.32ms p(95)=132.52ms
http_req_failed................: 0.00%  0 out of 22501
http_reqs......................: 22501  124.99706/s

EXECUTION
iteration_duration.............: avg=62.27ms min=8.74ms med=12.63ms max=4.03s p(90)=31.25ms p(95)=133.57ms
iterations.....................: 22501  124.99706/s
vus............................: 2      min=1          max=208
vus_max........................: 600    min=600        max=600

NETWORK
data_received..................: 859 MB 4.8 MB/s
data_sent......................: 3.7 MB 21 kB/s
```
````

</details>

### TEST-20260904-021 — CPU 관측용 130 RPS 테스트

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |
| CPU 관측 시작 | 2026-09-04T12:46:16Z |
| CPU 관측 종료 | 2026-09-04T12:49:17Z |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | CPU 관측용 130 RPS 테스트 |
| 목적 | 130 RPS 부하 구간의 서버 CPU 지표 확인 |
| VU | maxVUs 600; `vus` 관측값 min=1, max=212 |
| rate/RPS | `RATE=130`, 실측 129.996368 req/s |
| duration | CPU 관측 구간 3m1s; k6 요약 집계 구간 약 180.01s(계산값) |
| 총 요청 수 | 23,401 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 63.49ms | `http_req_duration` |
| p50/median | 12.3ms | `http_req_duration`의 `med` |
| p90 | 36.43ms | `http_req_duration` |
| p95 | 228.25ms | `http_req_duration` |
| max | 3.73s | `http_req_duration` |
| error rate | 0.00% (0/23,401) | `http_req_failed` |
| dropped iterations | 0 | 전체 k6 세션 출력에 별도 행 없음 |

#### 서버 관측값

관측 구간: `2026-09-04T12:46:16Z`~`2026-09-04T12:49:17Z`

| 지표 | Avg | Peak | Samples |
|---|---:|---:|---:|
| `cpu_usage_active` | 70.51793531265405% | 90.18715225117707% | 19 |
| `cpu_usage_user` | 51.381847752926284% | 66.66666666658692% | 19 |
| `cpu_usage_system` | 12.073701780623841% | 15.730905412228893% | 19 |
| `cpu_usage_iowait` | 0.002638174383361286% | 0.05012531328386443% | 19 |
| `mem_used_percent` | 62.41287664725587% | 63.436448069977146% | 19 |
| `swap_used_percent` | 23.330190886811394% | 23.333975475264502% | 19 |

#### 분석/메모

- 서버 CPU 지표 확인을 위해 UTC 구간 2026-09-04T12:46:16Z~2026-09-04T12:49:17Z를 기록했다.
- checks 46,802/46,802 성공, 실패 0건이며 HTTP 요청 23,401건 모두 성공했다.
- 목표 130 RPS에 대해 실측 처리량은 129.996368 req/s다.
- 계산값: 목표 rate 대비 차이는 -0.003632 req/s, 달성률은 약 99.9972%다. `(129.996368 / 130) × 100`
- 계산값: 총 요청 수와 실측 RPS로 산출한 k6 요약 집계 구간은 약 180.01초다. `23,401 / 129.996368`
- max active VUs는 212로 VU pool 600의 약 35.33%다(계산값).
- p95 228.25ms는 median 12.3ms의 약 18.56배다(계산값).
- dropped iterations는 전체 세션 출력에 별도 행이 없으므로 0으로 기록했다.
- k6 출력에는 서버 CPU 값이 포함되지 않으며, 위 UTC 구간을 AWS 지표와 매칭해야 한다.
- 이후 제공된 하드웨어 지표의 관측 구간이 테스트 UTC 구간과 일치하며, `cpu_usage_active`는 평균 70.51793531265405%, 최고 90.18715225117707%였다.
- 아직 After 결과가 없으므로 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
\================ 130 RPS TIME ================
RATE=130
START\_UTC=2026-09-04T12:46:16Z
END\_UTC=2026-09-04T12:49:17Z

\================ 130 RPS RESULT ================
█ TOTAL RESULTS
```yaml
checks_total.......: 46802   259.992737/s
checks_succeeded...: 100.00% 46802 out of 46802
checks_failed......: 0.00%   0 out of 46802

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=63.49ms min=8.51ms med=12.3ms  max=3.73s p(90)=36.43ms p(95)=228.25ms
  { expected_response:true }...: avg=63.49ms min=8.51ms med=12.3ms  max=3.73s p(90)=36.43ms p(95)=228.25ms
http_req_failed................: 0.00%  0 out of 23401
http_reqs......................: 23401  129.996368/s

EXECUTION
iteration_duration.............: avg=64.13ms min=8.95ms med=12.71ms max=3.73s p(90)=37.57ms p(95)=228.68ms
iterations.....................: 23401  129.996368/s
vus............................: 1      min=1          max=212
vus_max........................: 600    min=600        max=600

NETWORK
data_received..................: 893 MB 5.0 MB/s
data_sent......................: 3.8 MB 21 kB/s
```
````

</details>

### TEST-20260904-022 — CPU 관측용 140 RPS 재측정

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |
| CPU 관측 시작 | 2026-09-04T13:00:12Z |
| CPU 관측 종료 | 2026-09-04T13:03:12Z |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | CPU 관측용 140 RPS 재측정 |
| 목적 | 충분한 VU pool에서 140 RPS 처리 성능과 서버 하드웨어 사용량 확인 |
| VU | maxVUs 1,000; `vus` 관측값 min=1, max=202 |
| rate/RPS | `RATE=140`, 실측 139.979746 req/s |
| duration | CPU 관측 구간 3m0s; k6 요약 집계 구간 약 180.03s(계산값) |
| 총 요청 수 | 25,201 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 117.61ms | `http_req_duration` |
| p50/median | 13.08ms | `http_req_duration`의 `med` |
| p90 | 213.18ms | `http_req_duration` |
| p95 | 779.04ms | `http_req_duration` |
| max | 3.8s | `http_req_duration` |
| error rate | 0.00% (0/25,201) | `http_req_failed` |
| dropped iterations | 0 | 전체 k6 세션 출력에 별도 행 없음 |

#### 서버 관측값

관측 구간: `2026-09-04T13:00:12Z`~`2026-09-04T13:03:12Z`

| 지표 | Avg | Peak | Samples |
|---|---:|---:|---:|
| `cpu_usage_active` | 82.92525141171855% | 99.85000000102445% | 19 |
| `cpu_usage_user` | 61.45566448619649% | 78.94999999998618% | 19 |
| `cpu_usage_system` | 13.650928876446573% | 16.274411617259165% | 19 |
| `cpu_usage_iowait` | 0.04871326660288886% | 0.8746355685102625% | 19 |
| `mem_used_percent` | 62.71447033269061% | 63.64366036839562% | 19 |
| `swap_used_percent` | 22.87038848649891% | 22.875829459818764% | 19 |

#### 분석/메모

- TEST-20260904-015에서 부족했던 load generator VU pool을 1,000으로 늘려 140 RPS를 재측정했다.
- checks 50,402/50,402 성공, 실패 0건이며 HTTP 요청 25,201건 모두 성공했다.
- 목표 140 RPS에 대해 실측 처리량은 139.979746 req/s다.
- 계산값: 목표 rate 대비 차이는 -0.020254 req/s, 달성률은 약 99.9855%다. `(139.979746 / 140) × 100`
- 계산값: 총 요청 수와 실측 RPS로 산출한 k6 요약 집계 구간은 약 180.03초다. `25,201 / 139.979746`
- max active VUs는 202로 VU pool 1,000의 20.2%다(계산값).
- p95 779.04ms는 median 13.08ms의 약 59.56배다(계산값).
- VU 부족 경고와 `dropped_iterations` 행이 없으므로 dropped iterations는 0으로 기록했다.
- 하드웨어 지표의 관측 구간은 테스트 UTC 구간과 일치하며, `cpu_usage_active`는 평균 82.92525141171855%, 최고 99.85000000102445%였다.
- load generator VU 부족은 해소됐지만 CPU active 최고값이 약 99.85%까지 관측됐다.
- 아직 After 결과가 없으므로 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
\================ 140 RPS TIME ================
RATE=140
START\_UTC=2026-09-04T13:00:12Z
END\_UTC=2026-09-04T13:03:12Z

\================ 140 RPS RESULT ================
█ TOTAL RESULTS
```yaml
checks_total.......: 50402   279.959492/s
checks_succeeded...: 100.00% 50402 out of 50402
checks_failed......: 0.00%   0 out of 50402

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=117.61ms min=8.97ms med=13.08ms max=3.8s p(90)=213.18ms p(95)=779.04ms
  { expected_response:true }...: avg=117.61ms min=8.97ms med=13.08ms max=3.8s p(90)=213.18ms p(95)=779.04ms
http_req_failed................: 0.00%  0 out of 25201
http_reqs......................: 25201  139.979746/s

EXECUTION
iteration_duration.............: avg=118.41ms min=9.29ms med=13.51ms max=3.8s p(90)=214.98ms p(95)=779.41ms
iterations.....................: 25201  139.979746/s
vus............................: 5      min=1          max=202 
vus_max........................: 1000   min=1000       max=1000

NETWORK
data_received..................: 964 MB 5.4 MB/s
data_sent......................: 4.7 MB 26 kB/s
```
````

</details>

### TEST-20260904-023 — CPU 관측용 150 RPS 재측정

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |
| CPU 관측 시작 | 2026-09-04T13:05:12Z |
| CPU 관측 종료 | 2026-09-04T13:08:13Z |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | CPU 관측용 150 RPS 재측정 |
| 목적 | 충분한 VU pool에서 150 RPS 처리 성능과 서버 하드웨어 사용량 확인 |
| VU | maxVUs 1,000; `vus` 관측값 min=2, max=277 |
| rate/RPS | `RATE=150`, 실측 149.995549 req/s |
| duration | CPU 관측 구간 3m1s; k6 요약 집계 구간 약 180.01s(계산값) |
| 총 요청 수 | 27,001 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 177.77ms | `http_req_duration` |
| p50/median | 18.34ms | `http_req_duration`의 `med` |
| p90 | 568.69ms | `http_req_duration` |
| p95 | 1.06s | `http_req_duration` |
| max | 4.7s | `http_req_duration` |
| error rate | 0.00% (0/27,001) | `http_req_failed` |
| dropped iterations | 0 | 전체 k6 세션 출력에 별도 행 없음 |

#### 서버 관측값

관측 구간: `2026-09-04T13:05:12Z`~`2026-09-04T13:08:13Z`

| 지표 | Avg | Peak | Samples |
|---|---:|---:|---:|
| `cpu_usage_active` | 86.08746649744988% | 99.90004997491829% | 19 |
| `cpu_usage_user` | 63.89943962322531% | 74.10938284105018% | 19 |
| `cpu_usage_system` | 14.351852631842085% | 17.511289513566425% | 19 |
| `cpu_usage_iowait` | 0.002635532245707589% | 0.050075112668444194% | 19 |
| `mem_used_percent` | 62.32360336453803% | 63.73231938540668% | 19 |
| `swap_used_percent` | 22.860450177298432% | 22.867055639373092% | 19 |

#### 분석/메모

- TEST-20260904-016에서 부족했던 load generator VU pool을 1,000으로 늘려 150 RPS를 재측정했다.
- checks 54,002/54,002 성공, 실패 0건이며 HTTP 요청 27,001건 모두 성공했다.
- 목표 150 RPS에 대해 실측 처리량은 149.995549 req/s다.
- 계산값: 목표 rate 대비 차이는 -0.004451 req/s, 달성률은 약 99.9970%다. `(149.995549 / 150) × 100`
- 계산값: 총 요청 수와 실측 RPS로 산출한 k6 요약 집계 구간은 약 180.01초다. `27,001 / 149.995549`
- max active VUs는 277로 VU pool 1,000의 27.7%다(계산값).
- 계산값: p95 1.06s(1,060ms)는 median 18.34ms의 약 57.80배다.
- VU 부족 경고와 `dropped_iterations` 행이 없으므로 dropped iterations는 0으로 기록했다.
- 하드웨어 지표의 관측 구간은 테스트 UTC 구간과 일치하며, `cpu_usage_active`는 평균 86.08746649744988%, 최고 99.90004997491829%였다.
- load generator VU 부족은 해소됐지만 CPU active 최고값이 약 99.90%까지 관측됐다.
- 아직 After 결과가 없으므로 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
\================ 150 RPS TIME ================
RATE=150
START\_UTC=2026-09-04T13:05:12Z
END\_UTC=2026-09-04T13:08:13Z

\================ 150 RPS RESULT ================
█ TOTAL RESULTS
```yaml
checks_total.......: 54002   299.991099/s
checks_succeeded...: 100.00% 54002 out of 54002
checks_failed......: 0.00%   0 out of 54002

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=177.77ms min=8.55ms med=18.34ms max=4.7s p(90)=568.69ms p(95)=1.06s
  { expected_response:true }...: avg=177.77ms min=8.55ms med=18.34ms max=4.7s p(90)=568.69ms p(95)=1.06s
http_req_failed................: 0.00%  0 out of 27001
http_reqs......................: 27001  149.995549/s

EXECUTION
iteration_duration.............: avg=178.67ms min=9.08ms med=18.87ms max=4.7s p(90)=569.05ms p(95)=1.06s
iterations.....................: 27001  149.995549/s
vus............................: 2      min=2          max=277 
vus_max........................: 1000   min=1000       max=1000

NETWORK
data_received..................: 1.0 GB 5.7 MB/s
data_sent......................: 4.9 MB 27 kB/s
```
````

</details>

### TEST-20260904-024 — 150 RPS 프로세스별 CPU 진단 테스트

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |
| 프로세스 관측 시작 | 2026-09-04T13:15:04Z |
| 프로세스 관측 종료 | 2026-09-04T13:18:29Z |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | `capacity-150-process-before.txt` 프로세스별 CPU 진단 테스트 |
| 목적 | 150 RPS 부하 전후의 앱·DB 컨테이너별 CPU 및 메모리 사용량 확인 |
| VU | maxVUs 1,000; `vus` 관측값 min=1, max=290 |
| rate/RPS | 목표 150 RPS(결과 파일명 기준), 실측 148.916199 req/s |
| duration | k6 요약 집계 구간 약 181.31s(계산값) |
| 총 요청 수 | 27,000 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 106.34ms | `http_req_duration` |
| p50/median | 14.08ms | `http_req_duration`의 `med` |
| p90 | 117.12ms | `http_req_duration` |
| p95 | 504.45ms | `http_req_duration` |
| max | 5.2s | `http_req_duration` |
| error rate | 0.00% (0/27,000) | `http_req_failed` |
| dropped iterations | 0 | 전체 k6 세션 출력에 별도 행 없음 |

#### 서버 관측값

관측 구간: `2026-09-04T13:15:04Z`~`2026-09-04T13:18:29Z` (18회)

| 대상 | CPU Avg | CPU Peak | 메모리 Avg | 메모리 범위 | 메모리 사용률 Avg | 메모리 사용률 범위 | 비고 |
|---|---:|---:|---:|---:|---:|---:|---|
| `baedalondo-app-1` | 76.7417% | 103.01% | 723.0778MiB | 716~732.4MiB | 37.9022% | 37.53~38.39% | 전체 18개 관측값 기준 계산값 |
| `baedalondo-db-1` | 52.8411% | 71.54% | 105.4278MiB | 105.2~105.6MiB | 5.5278% | 5.51~5.54% | 전체 18개 관측값 기준 계산값 |

##### 프로세스별 원본 시계열

| 시각(UTC) | 앱 CPU | 앱 메모리 | 앱 메모리 사용률 | DB CPU | DB 메모리 | DB 메모리 사용률 |
|---|---:|---:|---:|---:|---:|---:|
| 2026-09-04T13:15:04Z | 0.19% | 716MiB / 1.863GiB | 37.53% | 6.25% | 105.4MiB / 1.863GiB | 5.53% |
| 2026-09-04T13:15:16Z | 84.91% | 717.3MiB / 1.863GiB | 37.60% | 63.32% | 105.6MiB / 1.863GiB | 5.53% |
| 2026-09-04T13:15:28Z | 94.33% | 717.5MiB / 1.863GiB | 37.61% | 61.73% | 105.5MiB / 1.863GiB | 5.53% |
| 2026-09-04T13:15:40Z | 89.20% | 717.4MiB / 1.863GiB | 37.61% | 59.53% | 105.2MiB / 1.863GiB | 5.52% |
| 2026-09-04T13:15:52Z | 86.23% | 717.3MiB / 1.863GiB | 37.60% | 60.55% | 105.3MiB / 1.863GiB | 5.52% |
| 2026-09-04T13:16:04Z | 103.01% | 723.2MiB / 1.863GiB | 37.91% | 71.54% | 105.4MiB / 1.863GiB | 5.53% |
| 2026-09-04T13:16:16Z | 90.43% | 723MiB / 1.863GiB | 37.90% | 59.06% | 105.3MiB / 1.863GiB | 5.52% |
| 2026-09-04T13:16:28Z | 89.42% | 722.9MiB / 1.863GiB | 37.89% | 58.81% | 105.4MiB / 1.863GiB | 5.52% |
| 2026-09-04T13:16:40Z | 94.50% | 723.3MiB / 1.863GiB | 37.91% | 62.24% | 105.6MiB / 1.863GiB | 5.54% |
| 2026-09-04T13:16:53Z | 92.71% | 723.2MiB / 1.863GiB | 37.91% | 61.66% | 105.5MiB / 1.863GiB | 5.53% |
| 2026-09-04T13:17:05Z | 98.44% | 723.6MiB / 1.863GiB | 37.93% | 70.17% | 105.4MiB / 1.863GiB | 5.53% |
| 2026-09-04T13:17:17Z | 90.05% | 723.8MiB / 1.863GiB | 37.94% | 63.43% | 105.4MiB / 1.863GiB | 5.52% |
| 2026-09-04T13:17:29Z | 86.23% | 723.9MiB / 1.863GiB | 37.94% | 62.32% | 105.5MiB / 1.863GiB | 5.53% |
| 2026-09-04T13:17:41Z | 88.58% | 724MiB / 1.863GiB | 37.95% | 61.04% | 105.6MiB / 1.863GiB | 5.54% |
| 2026-09-04T13:17:53Z | 92.42% | 724MiB / 1.863GiB | 37.95% | 59.51% | 105.6MiB / 1.863GiB | 5.54% |
| 2026-09-04T13:18:05Z | 100.36% | 732.4MiB / 1.863GiB | 38.39% | 68.38% | 105.6MiB / 1.863GiB | 5.54% |
| 2026-09-04T13:18:17Z | 0.16% | 731.4MiB / 1.863GiB | 38.34% | 0.73% | 105.2MiB / 1.863GiB | 5.52% |
| 2026-09-04T13:18:29Z | 0.18% | 731.2MiB / 1.863GiB | 38.33% | 0.87% | 105.2MiB / 1.863GiB | 5.51% |

#### 분석/메모

- 프로세스 관측 출력은 부하 전후를 포함해 2026-09-04T13:15:04Z~2026-09-04T13:18:29Z에 18회 수집됐다.
- 서버 관측 요약의 평균과 범위는 제공된 18개 시계열 전체를 대상으로 계산했으므로, 시작 전·종료 후로 보이는 낮은 CPU 관측값도 포함한다.
- `baedalondo-app-1` CPU는 최고 103.01%, `baedalondo-db-1` CPU는 최고 71.54%로 관측됐다. 100% 초과 값은 원본 그대로 보존했으며 수집 도구의 CPU 백분율 정의는 별도 확인이 필요하다.
- checks 54,000/54,000 성공, 실패 0건이며 HTTP 요청 27,000건 모두 성공했다.
- 목표 150 RPS(결과 파일명 기준)에 대해 실측 처리량은 148.916199 req/s다.
- 계산값: 목표 rate 대비 차이는 -1.083801 req/s, 달성률은 약 99.2775%다. `(148.916199 / 150) × 100`
- 계산값: 총 요청 수와 실측 RPS로 산출한 k6 요약 집계 구간은 약 181.31초다. `27,000 / 148.916199`
- max active VUs는 290으로 VU pool 1,000의 29.0%다(계산값).
- p95 504.45ms는 median 14.08ms의 약 35.83배다(계산값).
- `dropped_iterations` 행이 없으므로 dropped iterations는 0으로 기록했다.
- TEST-20260904-023과 같은 150 RPS·maxVUs 1,000의 Before 결과지만, 실행 및 관측 조건 전체가 확인되지 않아 직접 비교표에는 연결하지 않았다.
- 아직 After 결과가 없으므로 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
$ cd \~/k6

sed -n '/TOTAL RESULTS/,$p' \
results/diagnostic/capacity-150-process-before.txt$ $ >
█ TOTAL RESULTS
```yaml
checks_total.......: 54000   297.832397/s
checks_succeeded...: 100.00% 54000 out of 54000
checks_failed......: 0.00%   0 out of 54000

✓ status is 200
✓ dashboard rendered

HTTP
http_req_duration..............: avg=106.34ms min=8.68ms med=14.08ms max=5.2s p(90)=117.12ms p(95)=504.45ms
  { expected_response:true }...: avg=106.34ms min=8.68ms med=14.08ms max=5.2s p(90)=117.12ms p(95)=504.45ms
http_req_failed................: 0.00%  0 out of 27000
http_reqs......................: 27000  148.916199/s

EXECUTION
iteration_duration.............: avg=107.17ms min=9.11ms med=14.48ms max=5.2s p(90)=120.98ms p(95)=505.34ms
iterations.....................: 27000  148.916199/s
vus............................: 126    min=1          max=290 
vus_max........................: 1000   min=1000       max=1000

NETWORK
data_received..................: 1.0 GB 5.7 MB/s
data_sent......................: 4.9 MB 27 kB/s
```



$
````

</details>

### TEST-20260904-025 — 140 RPS 정각 회피 재측정

#### 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-09-04 |
| 구분 | Before (사용자 지정 기본 분류) |
| 관측 시작 | 2026-09-04T13:45:25Z |
| 관측 종료 | 2026-09-04T13:48:26Z |

#### 테스트 조건

| 항목 | 값 |
|---|---|
| 테스트명 | 140 RPS 정각 회피 재측정 |
| 목적 | TEST-20260904-022의 측정 창이 정각 직후라 예보 사전 적재 및 기상청 기준 시각 전환과 겹쳤을 가능성을 배제한 140 RPS 재측정 |
| executor | `constant-arrival-rate` |
| VU | maxVUs 1,000; `vus` 관측값 min=1, max=208 |
| rate/RPS | 설정 140.00 iterations/s, 실측 139.994995 req/s |
| duration | `START_UTC`~`END_UTC` 3m01s; k6 집계 구간 계산값 약 180.01s |
| 총 요청 수 | 25,201 |

#### 핵심 결과

| 지표 | 값 | 출처/비고 |
|---|---:|---|
| avg | 88.81ms | `http_req_duration` |
| p50/median | 13.72ms | `http_req_duration`의 `med` |
| p90 | 139.22ms | `http_req_duration` |
| p95 | 498.4ms | `http_req_duration` |
| max | 4.02s | `http_req_duration` |
| error rate | 0.00% (0/25,201) | `http_req_failed` |
| dropped iterations | 0 | 전체 k6 세션 출력에 별도 행 없음 |

#### 분석/메모

- checks 50,402/50,402 성공, 실패 0건이며 HTTP 요청 25,201건 모두 성공했다.
- 설정 rate 140.00 iterations/s에 대해 실측 처리량은 139.994995 req/s다.
- 계산값: 목표 rate 대비 차이는 -0.005005 req/s, 달성률은 약 99.9964%다. `(139.994995 / 140.00) × 100`
- 계산값: 총 요청 수와 실측 RPS로 산출한 k6 요약 집계 구간은 약 180.01초다. `25,201 / 139.994995`
- max active VUs는 208로 VU pool 1,000의 20.8%다(계산값). `Insufficient VUs` 경고는 출력되지 않았다.
- 계산값: p95 498.4ms는 median 13.72ms의 약 36.33배, max 4.02s는 약 292.99배다.
- 계산값: `iteration_duration` avg 89.58ms와 `http_req_duration` avg 88.81ms의 차이는 0.77ms다. `http_req_blocked`·DNS·TLS 핸드셰이크는 `http_req_duration`에 포함되지 않고 `iteration_duration`에만 포함되므로, 두 값의 차이가 1ms 미만이라는 것은 측정된 지연이 부하 생성기 측 대기가 아니라 서버 측 시간이라는 뜻이다.
- 재측정 사유: TEST-20260904-022의 관측 창이 2026-09-04T13:00:12Z~13:03:12Z로 정각 12초 뒤에 시작해, 매시 정각에 실행되는 예보 사전 적재 스케줄러 및 기상청 기준 시각 전환 직후 구간과 겹칠 가능성이 있었다. 이번 회차는 13:45:25Z에 시작해 해당 구간을 피했다.
- 계산값: TEST-20260904-022 대비 avg는 117.61ms → 88.81ms로 약 24.49%, p90은 213.18ms → 139.22ms로 약 34.69%, p95는 779.04ms → 498.4ms로 약 36.03% 감소했다. median은 13.08ms → 13.72ms로 거의 변하지 않았고 max active VU도 202 → 208로 유사하다.
- 위 차이는 정각 구간 겹침이 상위 구간 지연에 기여했음을 시사한다. 다만 median과 max(3.80s → 4.02s)가 거의 변하지 않았으므로, 초 단위 단발 지연은 정각 구간과 무관하게 존재한다.
- 계산값: TEST-20260904-021(130 RPS) 대비 p90은 36.43ms → 139.22ms로 약 3.82배, p95는 228.25ms → 498.4ms로 약 2.18배다. TEST-20260904-022로 계산한 5.85배·3.41배보다 완만하지만 130~140 RPS 사이에서 상위 구간 지연이 급격히 증가하는 경향 자체는 유지된다.
- 이 회차에는 서버 CPU 관측값을 함께 기록하지 않았다. 위 UTC 구간으로 AWS 지표를 매칭해야 하며, TEST-20260904-022의 CPU 값을 이 회차에 그대로 적용하지 않는다.
- `dropped_iterations` 행이 없으므로 dropped iterations는 0으로 기록했다.
- 140 RPS 구간의 canonical 측정으로는 정각 구간을 피한 이 회차를 사용하고, TEST-20260904-022는 정각 겹침 가능성이 있는 측정으로 남긴다.
- 아직 After 결과가 없으므로 개선 전/후 비교표와 핵심 성과는 갱신하지 않았다.

#### 원본 k6 출력

<details>
<summary>원본 출력 펼치기</summary>

````text
echo "START_UTC=$START_UTC"
echo "END_UTC=$END_UTC"$ $ $ $ > >


  █ TOTAL RESULTS

    checks_total.......: 50402   279.989991/s
    checks_succeeded...: 100.00% 50402 out of 50402
    checks_failed......: 0.00%   0 out of 50402

    ✓ status is 200
    ✓ dashboard rendered

    HTTP
    http_req_duration..............: avg=88.81ms min=8.41ms med=13.72ms max=4.02s p(90)=139.22ms p(95)=498.4ms 
      { expected_response:true }...: avg=88.81ms min=8.41ms med=13.72ms max=4.02s p(90)=139.22ms p(95)=498.4ms 
    http_req_failed................: 0.00%  0 out of 25201
    http_reqs......................: 25201  139.994995/s

    EXECUTION
    iteration_duration.............: avg=89.58ms min=8.91ms med=14.14ms max=4.02s p(90)=144.48ms p(95)=499.94ms
    iterations.....................: 25201  139.994995/s
    vus............................: 1      min=1          max=208 
    vus_max........................: 1000   min=1000       max=1000

    NETWORK
    data_received..................: 964 MB 5.4 MB/s
    data_sent......................: 4.7 MB 26 kB/s



$ $ $ $ START_UTC=2026-09-04T13:45:25Z
$ END_UTC=2026-09-04T13:48:26Z
$
````

</details>
