# 배달온도 v2 설계

작성 2026-08-17 · 상태 **설계 확정, 착수 전**

---

## 요약

v1은 서울시 상권 추정매출과 문헌 기반 가중치를 더해 0~100 점수를 직접 계산한다.
실제 배달 주문량을 target으로 학습한 값이 아니라 proxy다.

v2는 **먼저 주문량을 예측하고 그 다음에 점수로 바꾼다.**

```
주문 CSV → 수요 예측 모델 → 예상 주문건수 → percentile → 배달온도
```

점수는 예측 target이 아니라 표현 계층의 값이 된다.

매장 데이터가 쌓이는 정도에 따라 개인화 수준을 올리고, **백테스트를 통과한 모델만** 실제 서비스에 올린다.

---

## 전제 — 착수 전 확인 필요

이 둘이 확정되기 전에는 구현을 시작하지 않는다.

### ① 쿠팡이츠 CSV의 `time` 컬럼

`baedal-pnl`의 쿠팡이츠 importer가 49개 컬럼을 `date(0)`, `time(1)`, `order_no(2)`, `txn_type(8)`, `order_amt(10)`, `payout(45)`로 매핑한다. 정산금액 검증이 행별·월합계 오차 0으로 맞춰져 있어 실파일 기준 맵으로 신뢰할 수 있다.

다만 importer는 `time`을 **정의만 하고 사용하지 않는다**(손익계산에 불필요). 따라서 코드로는 확인할 수 없다.

**실물 파일에서 확인할 것**

| 확인 항목 | 판별 방법 |
|---|---|
| 형식 | `18:42:17` / `18:42` / 엑셀 시리얼 숫자 중 무엇인가 |
| 의미 | **주문 시각**인가 정산 처리 시각인가 |

두 번째가 핵심이다. `time` 컬럼의 **고유값 개수**를 세면 즉시 판별된다. 배치 처리 시각이면 값이 몇 개로 뭉쳐 있다.

**시각이 없거나 일 단위면 이 설계는 성립하지 않는다.** target을 일 단위로 내리고 시간대는 v1 prior를 유지하는 별도 설계가 필요하다.

### ② 과거 초단기예보 보관 기간

**API는 존재한다.** 기상청 API허브의 초단기예보 격자 API가 발표시각(`tmfc`)과 예보대상시각(`tmef`)을 파라미터로 받고, +6시간까지 `T1H / RN1 / PTY / REH / WSD`를 제공한다.

**미확인인 것은 보관 기간이다.** 파라미터로 과거 발표시각을 지정할 수 있다는 것과, 12개월 전 자료가 실제로 남아 있다는 것은 다르다.

**확인 방법**: 우리 인증키로 12·9·6·3개월 전 `tmfc`를 지정해 실제 응답이 오는지 integration test. 어느 시점부터 비는지가 Weather Model의 학습 가능 범위를 결정한다.

확보 실패 시 대응은 §4에 정의되어 있다.

---

## 1. 모델 계층

### 승격 조건

| 단계 | 조건 |
|---|---|
| v1 | CSV 없음 |
| CREDIBILITY_BASELINE | 매장 주문이 조금이라도 존재 |
| Time Model 후보 | 3개월+ |
| Weather Model 후보 | 12개월+ |

기간만으로 판단하지 않는다. **영업일 수, 총 주문건수, hour별 표본 수**를 함께 검사한다(§3, §5).

### 서비스 fallback

```
Weather Model → Time Model → Credibility Baseline → v1
```

### v1의 위치

v1은 **주문건수를 예측하지 않는다.** 0~100 환경지수라 WAPE 비교 대상이 될 수 없다.

따라서 v1은 **통계 경쟁 바깥의 cold-start 전용 UI 모델**이다.

Credibility Baseline은 매장 평균 주문률 `μ`만 있으면 동작하고 그건 일주일이면 잡히므로, 실질적으로 **v1은 CSV 업로드 이전 구간 전담**이다.

**백테스트 대상**: Credibility / Time / Weather 세 개.

---

## 2. Credibility Baseline

상권 prior에서 시작해 매장 주문이 쌓일수록 매장 자신의 이력으로 자동 이동한다.

### 2.1 prior shape

`data-processing/output/*-audit.csv`의 `time_index`, `day_index`를 그대로 쓴다.
전처리 검증용으로 남긴 중간 산출물인데, 이미 **평균 대비 비율** 형태다.

```
time_index = hourly_average / overall_hourly_average × 100
day_index  = weekday_daily_average / weekly_daily_average × 100
```

```
rawShape(d,h) = timeIndex(h)/100 × dayIndex(d)/100
```

v1이 쓰는 `demand_level`(VERY_HIGH…)과 `day_weight`(−6~+6)는 이 비율을 **구간화한 결과**다.
v1은 구간화된 값을, v2는 원본 비율을 쓴다. 같은 파이프라인에서 둘 다 나오므로 재작업이 없다.

> **v1의 점수(`+14`, `+8` 등)를 prior로 쓰지 않는다.** 주문량 비율이 아니기 때문이다.

### 2.2 매장 평균 주문률

```
μ = 전체 유효 주문수 / 전체 영업 노출시간
```

### 2.3 노출 가중 정규화

영업시간이 24시간이 아니면 `rawShape`의 영업시간 평균이 1이 아니다.
그대로 곱하면 매장 전체 규모가 틀어진다. (예: 치킨집이 `time_index`가 높은 17~24시만 영업하면 prior 총량이 실제보다 커진다.)

```
Z = Σ[E(d,h) × rawShape(d,h)] / Σ[E(d,h)]

MarketPriorRate(d,h) = μ × rawShape(d,h) / Z
```

`Z`는 **노출 가중 평균**이어야 한다. 단순 평균을 쓰면 셀별 노출시간이 다를 때 어긋난다
(평일 11–22시 + 주말 11–24시인 매장에서 `(토, 23시)`는 주 1회뿐인데 `(월, 19시)`와 같은 무게를 받는다).

노출 가중이면 항등식이 정확히 성립한다.

```
Σ[E × MarketPriorRate] / Σ[E] = μ
```

### 2.4 Gamma-Poisson shrinkage

```
α = priorStrengthOrders          ← 설정값. prior가 주문 몇 건어치의 신뢰도를 갖는가
β = α / MarketPriorRate
Y = 해당 (요일, 시간) 셀의 누적 유효 주문수
E = 해당 셀의 누적 영업 노출시간

posteriorRate = (α + Y) / (β + E)
```

prior 혼합 가중치는 다음과 같다.

```
prior 가중 = β / (β + E) = α / (α + MarketPriorRate × E)
```

> 가중치는 **사전 기대 노출량 `MarketPriorRate × E`** 로 결정된다.
> 관측된 `Y`가 아니다. `Y`를 쓰면 우연히 주문이 몰린 셀이 그 이유만으로
> shrinkage를 덜 받는 순환 구조가 된다.

**`α`를 셀마다 동일하게 고정**해야 의도한 동작이 나온다. `α = 5`일 때:

| 셀 | prior rate | E | prior 가중 |
|---|---|---|---|
| 금요일 19시 | 20건/시간 | 12h | **2.0%** |
| 화요일 15시 | 1건/시간 | 12h | **29.4%** |

바쁜 셀은 prior를 거의 버리고, 한산한 셀은 prior에 기댄다.
`β`(가상 시간)를 고정하면 두 셀 가중치가 같아져 이 효과가 사라진다.

**제품 가치가 몰린 바쁜 시간대일수록 shrinkage가 빨리 풀린다.** 설계 거동이 제품 목적과 같은 방향이다.

### 2.5 rate floor — 필수, 그리고 적용 순서가 중요하다

`MarketPriorRate`가 0이면 `β`가 발산하고, **관측이 아무리 쌓여도 posterior가 0에 고정된다.**

실제 데이터에 존재하는 문제다.

```
time_index == 0 : 967 / 27,774 셀 (3.5%)
```

**`MarketPriorRate`를 만든 다음 floor를 씌우면 §2.3의 항등식이 깨진다.**
정규화가 끝난 값에 손을 대면 `Σ[E × rate]/Σ[E] = μ`가 더 이상 성립하지 않는다.

**순서를 고정한다.**

```
1. Local index가 0 또는 결측
     ↓
2. City·업종 prior로 fallback
     ↓
3. 그래도 0이면 rawShape에 작은 floor 적용
     ↓
4. Z를 재계산               ← floor 적용 후의 rawShape 기준
     ↓
5. MarketPriorRate 생성
```

floor는 **`rawShape` 단계에서만** 적용한다. 이후 `Z`를 다시 계산하므로 항등식이 그대로 유지된다.

floor 값은 설정파일에서 관리한다.

> **선행 작업**: 현재 `time-weight-city.csv` / `day-weight-city.csv`는 구간화된 값
> (`demand_level`, `weight`)만 담고 있고, **index 형태는 `*-audit.csv`의 local 레벨에만 있다.**
> City fallback을 쓰려면 전처리에서 **city 레벨 index를 audit 출력에 추가**해야 한다.
> city 집계 자체는 이미 하고 있으므로 출력 항목 추가 수준의 변경이다.

### 2.6 Spring이 저장할 것

Credibility Baseline은 회귀 계수가 없다. **해결된 rate 테이블을 저장한다.**

```
(dayOfWeek, hour) → posteriorRate      최대 168행
```

`α`, `μ`, `Z`를 저장해 Spring이 재계산하게 하면 Python과 결과가 갈라질 수 있다.
**최종 테이블을 저장하면 verificationCases 검증도 단순해진다.**

---

## 3. Time Model

3개월 이상부터 후보 생성.

### Feature

- `hour` — categorical
- `dayOfWeek` — categorical
- `holiday` — 이진

**초기에 쓰지 않는 것**: `hour × dayOfWeek` 전체 교차(84칸, 데이터 규모 대비 과다), `trend`, lag feature.

> `trend`를 뺀 이유: lag/이동평균은 CSV가 월 1회 들어오므로 추론 시점의 값이 최대 한 달 묵는다.
> 학습은 신선한 lag, 서빙은 묵은 lag이 되어 train/serve skew가 생긴다.
> 최근성은 **12개월 rolling window + 월 1회 재학습**으로 대신한다(§8).

### hour 표본 검사

영업시간이 요일마다 다르면 특정 hour의 표본이 심하게 적어진다.
(평일 11–22시 + 주말 11–24시 → `hour=23`은 주 2회뿐)

**hour별 표본 하한을 두고 미달 시 인접 시간과 병합한다.**

> Credibility Baseline은 shrinkage가 저표본 셀을 prior로 당겨주므로 병합이 불필요하다.
> 회귀 계수를 추정하는 Time Model에만 해당한다.

### 후보 모델

- Poisson Regression
- Negative Binomial Regression (NB2)

주문건수는 과산포가 흔하므로 **NB2를 주력 후보**로 두되, 백테스트가 결정한다.
Poisson이 더 좋거나 충분히 비슷하면 더 단순한 쪽을 택한다.

---

## 4. Weather Model

12개월 이상부터 후보 생성.

### Feature

Time Model feature에 다음을 더한다.

| 항목 | 처리 |
|---|---|
| T1H 기온 | `temperature` + `temperature²` |
| RN1 강수 | `NO_RAIN / LIGHT / HEAVY` 소수 bucket |
| PTY 강수형태 | categorical, 소수 분류 |
| REH 습도 | 연속 |
| WSD 풍속 | 연속 |

희귀 날씨를 무리하게 세분화하지 않는다. 12개월이어도 눈·폭우 표본은 수십 시간 수준이다.

초기에는 interaction을 넣지 않는다. 이후 별도 candidate로 검증 후 추가한다.

### 예측 시점 구조 — `asOf` / `forecastAt` / `leadHour`

**"주문 시각 당시의 예보"만으로는 부족하다.** 그렇게 학습하면 20시 주문에 19:50 발표 예보가 붙어 사실상 nowcast를 학습하게 되는데, 실서비스는 18시에 +2시간 예보로 20시를 맞힌다. **예보 오차 수준이 학습과 서빙에서 달라진다.**

정확한 값으로 학습한 계수가 오차 있는 입력에 그대로 적용되어 **날씨 효과가 과대 적용된다.**

```
asOf       = 예측을 수행한 시각
forecastAt = 예측 대상 시각
leadHour   = forecastAt − asOf     (1 ~ 6)
```

**학습 데이터를 (asOf, forecastAt) 쌍으로 구성한다.**

```
asOf 18시  →  19시(+1) 실제 주문   +  18시에 이용 가능했던 예보
asOf 18시  →  20시(+2) 실제 주문   +  18시에 이용 가능했던 예보
   …
asOf 18시  →  24시(+6) 실제 주문   +  18시에 이용 가능했던 예보
```

`tmfc`(발표시각) ≤ `asOf` 조건을 만족하는 가장 최근 발표분을 쓴다.

**`leadHour`를 categorical feature로 추가한다.**

#### 파생되는 두 가지

**① 학습 행이 6배로 늘지만 독립 관측이 아니다.**
같은 `forecastAt`을 target으로 하는 6개 행은 오차가 강하게 상관된다. 표준오차를 순진하게 계산하면 과소추정된다. **`forecastAt` 기준 clustered / robust SE**를 쓴다.

**② 진짜 필요한 것은 `leadHour × weather` 상호작용이다.**
예보 정확도가 lead time에 따라 떨어지므로, 날씨 계수도 `leadHour`가 클수록 약해져야 한다. `leadHour` 단독 categorical은 절편 이동만 잡는다.

다만 파라미터가 크게 늘어나므로 **초기에는 `leadHour` 단독으로 두고, 상호작용은 별도 candidate로 백테스트한다.**

#### Time Model은 해당 없음

`hour` / `dayOfWeek` / `holiday`는 전부 미래에도 완벽히 알려진 값이다.
20시 예측이 18시에 묻든 19시에 묻든 동일하므로 **Time Model은 lead time에 불변**이다.
`leadHour`는 Weather Model에만 들어간다.

### 과거 예보 확보 실패 시

**ASOS 관측으로 대체하지 않는다.**

관측으로 학습하면 백테스트도 관측으로 하게 되어, **백테스트는 좋게 나오고 실전만 나쁜** 상황이 생긴다. 검증이 검증 역할을 못 한다.

```
보류
→ 현재 ACTIVE 모델 유지
→ 확보 가능한 범위까지만 학습 (§전제 ②의 integration test 결과에 따름)
→ 부족분은 아카이빙으로 축적 후 재시도
```

> API허브에 `tmfc` 지정 조회가 있으므로 **전량 소급이 불가능한 상황은 아니다.**
> 관건은 보관 기간이고, 그것이 12개월에 못 미치는 만큼만 아카이빙으로 메우면 된다.
>
> 아카이빙은 여전히 유효한 보험이다. `forecast_weather_record`가 이미 존재하므로
> **삭제·덮어쓰기만 하지 않으면 된다.** 캐시 보존 기간 정리 작업과 충돌하지 않도록 주의한다.

---

## 5. 주문 데이터 정제

```
order_no 기준 dedupe
→ 결제/취소 lifecycle 정리
→ 최종 유효 주문만 사용
→ 주문 시각은 원 결제행 시각
```

취소행 시각은 취소 시점이지 수요 발생 시점이 아니다.

**Target**: 시간당 최종 유효 주문 건수.

취소율은 별도 feature 후보로만 남긴다.

### 제외 규칙

- 영업시간 밖 데이터
- 영업 예정일인데 하루 총 주문이 0건 → 임시휴업 후보로 판단해 제외

### 영업시간 정보

`E`(노출시간) 계산이 영업시간에 전적으로 의존한다.

CSV에서 영업 여부를 판별할 수 없으면 **`Store`에 요일별 영업시간을 별도로 저장**한다.

---

## 6. Backtest

랜덤 split 금지. **시간순 rolling / expanding** 백테스트를 쓴다.

```
과거 → 미래
과거 → 다음 미래
과거 → 다음 미래
```

**Credibility Baseline도 반드시 백테스트한다.** ACTIVE가 될 수 있으므로 OOS 예측 분포가 필요하다(§9).

세 모델 모두 다음을 생성한다.

- OOS prediction
- WAPE
- MAE
- Direction Accuracy
- Percentile Calibration

### prior의 시간적 leakage 차단

**백테스트 시점보다 미래의 상권 데이터를 prior에 쓰지 않는다.**

현재 audit은 2023~2025년 12개 분기를 전부 집계한 결과다. 이걸 그대로 쓰면 2025년 fold를 평가하면서 2025년 정보가 섞인 prior를 쓰게 된다. **백테스트가 심판 역할을 하려면 이걸 막아야 한다.**

```
2025년 fold 평가  →  2024년까지 집계한 prior
2026년 fold 평가  →  2025년까지 집계한 prior
```

`MarketDemandPrior`를 **cutoff별 snapshot으로 버전 관리**한다.
`preprocess_*.py`가 연도 상수를 받고 있으므로 범위를 제한해 재실행하면 된다.

> **트레이드오프**: cutoff가 이를수록 prior의 원천 분기 수가 줄어 노이즈가 커진다.
> 3년치뿐이라 2023 cutoff는 1년만 남는다.
> **최소 분기 수 하한을 두고, 미달하면 그 fold는 prior 기반 모델 판정에서 제외한다.**

### Weather Model 추가 규칙

- **전 계절 평가를 원칙으로 한다.** 날씨 계수는 계절마다 다르게 작동한다
- **강수 등 날씨 variation이 지나치게 부족한 fold는 승격 판정에서 제외한다**
- **`leadHour`별로도 성능을 남긴다.** +1시간에서만 이기고 +6시간에서 지는 모델은
  예보 스트립 용도로 쓸 수 없다

> 비가 거의 없던 fold에서는 Weather Model이 Time Model과 사실상 같은 예측을 낸다.
> 승패가 노이즈로 결정되므로 "승리"로 세면 판정이 오염된다.

Time Model에는 전 계절 요건을 걸지 않는다. 3개월 데이터로는 성립할 수 없고, `hour`/`dayOfWeek`는 그 정도로 계절에 휘둘리지 않는다.

---

## 7. 모델 승격

Candidate는 항상 **현재 ACTIVE count model**과 경쟁한다. 바로 아래 단계가 아니다.

> Time이 탈락해도 12개월 후 Weather가 현재 ACTIVE인 Credibility와 직접 경쟁할 수 있다.
> 중간 단계 실패가 상위 단계 진입을 막지 않는다.

### 승격 조건 — 모두 만족

1. 유효 fold의 **2/3 이상**에서 승리
2. OOS WAPE **절대 2%p 이상 AND 상대 10% 이상** 개선
3. 최악 fold에서도 현재 ACTIVE 대비 **상대 10% 이상 악화 없음**
4. Direction Accuracy 현재 ACTIVE 대비 **3%p 이상 하락 없음**
5. Percentile Calibration 설정된 허용범위 이상 악화 없음
6. **verificationCases 통과**

> **위 숫자는 통계 법칙이 아니다.** 초기 보수적 운영값이며 실제 CSV 시뮬레이션 후 조정한다.
> 전부 설정파일에서 관리한다.

절대·상대 기준을 병용하는 이유: 상대만 쓰면 원래 잘 맞는 매장(WAPE 15%)이 0.75%p 개선만으로 통과한다. 절대만 쓰면 WAPE가 높은 매장에서 지나치게 엄격해진다.

조건 3이 특히 중요하다. 평균은 좋은데 특정 기간에 크게 망가지는 모델이 가장 위험하다. 사장님은 평균이 아니라 그날그날을 경험한다.

---

## 8. 재학습

```
학습 범위   최근 12개월 rolling window
재학습 주기 월 1회
```

```
새 Candidate 생성 → Backtest → verificationCases
  통과 → ACTIVE 교체
  실패 → 기존 ACTIVE 유지
```

**모델이 바뀌면 다음을 전부 새 모델 기준으로 재생성한다.**

- coefficients
- preprocessing
- validation metrics
- verificationCases
- **OOS prediction distribution**

마지막 항목을 빠뜨리면 옛 분포에 새 모델 예측을 매핑하게 되어 배달온도가 조용히 어긋난다.

---

## 9. 배달온도 변환

모델이 **먼저 예상 주문량**을 낸다.

```
19시  11.3건
20시  14.8건
21시  12.2건
```

이를 **실제 주문량 분포에 넣지 않는다.**
현재 ACTIVE 모델의 **백테스트 OOS 예측값 분포**에 percentile mapping한다.

```
현재 예측이 OOS 예측 중 86 percentile → 배달온도 86
```

의미: *"현재 예상 수요는 이 가게에 대해 이 모델이 냈던 예측 중 상위 약 14% 수준"*

### 왜 실측 분포가 아닌가

예측값은 조건부 기대값이라 실측보다 분산이 작다.

```
Var(y) = E[Var(y|X)] + Var(ŷ)
                       └─ 항상 Var(y)보다 작다
```

실측 분포에 매핑하면 점수가 가운데로 몰린다. v1이 겪은 "40~73에 뭉친다"와 같은 병이다.

### CLOSED

영업하지 않는 시간은 **모델 예측 없이 CLOSED로 처리한다.** 낮은 점수와 별개 상태다.

---

## 10. Python / Spring 분리

| Python (학습 시에만) | Spring Boot (상시) |
|---|---|
| 전처리 | CSV 업로드 |
| Credibility 계산·검증 | 주문 정제 |
| Poisson / NB2 학습 | 과거 날씨 JOIN |
| Backtest | DB 관리 |
| 모델 비교·선택 | ACTIVE 모델 관리 |
| coefficient 생성 | **실시간 +1~+6시간 추론** |
| OOS prediction distribution 생성 | |
| verificationCases 생성 | |

스택: `pandas`, `statsmodels`, `scikit-learn`

실서비스에서는 DB에 저장된 coefficient와 preprocessing 정보로 **Spring이 직접 예측한다.** 추론 경로에 Python이 없다.

### verificationCases — 구현 일치 보증

전처리를 두 언어로 두 번 구현하게 되므로, RN1 버킷 경계·표준화 상수·categorical 인코딩 순서 중 **하나만 어긋나도 조용히 틀린 예측이 나오고 일반 테스트로는 잡히지 않는다.**

```
Python 학습 완료
→ (입력 feature, expectedPrediction) 30~50개 생성
→ Spring이 동일 입력으로 동일 결과를 내는지 검사
→ 불일치 시 ACTIVE 승격 거부
```

---

## 11. 저장 스키마 — `StoreDemandModel`

| 필드 | 설명 |
|---|---|
| `storeId` | |
| `status` | `CANDIDATE` / `ACTIVE` / `REJECTED` |
| `modelType` | `CREDIBILITY_BASELINE` / `POISSON` / `NEGATIVE_BINOMIAL` |
| `featureSet` | |
| `modelVersion` | |
| `trainingPeriodStart` / `End` | |
| `coefficients` | JSON. Credibility는 `(dow, hour) → rate` 테이블 |
| `intercept` | |
| `dispersionAlpha` | NB2 과산포 모수 |
| `preprocessing` | JSON. 버킷 경계·표준화 상수 등 |
| `validationMetrics` | JSON. WAPE / MAE / Direction / Calibration |
| `verificationCases` | JSON |
| `oosPredictionDistribution` | percentile 매핑용 |
| `createdAt` | |

새 모델 학습 시 **기존 ACTIVE를 즉시 삭제하지 않는다.** 검증 통과 시에만 교체한다.

### `dispersionAlpha`의 용도

NB2에서 `alpha`는 **분산에만 관여하고 기대값 계산에는 들어가지 않는다.** 점 추정만 낸다면 저장해도 추론에 쓰이지 않는다.

두 선택지 중 하나를 명시한다.

- **예측구간을 낸다** — `"예상 12.4건"` → `"예상 8~17건"`. 사장님에게 더 정직하고 제품으로도 강하다
- **모델 진단용으로만 보관한다**

---

## 12. 알려진 한계

### prior의 출처

`time_index` / `day_index`는 **서울 상권 매출 기반이지 순수 배달 데이터가 아니다.** v1이 안고 있던 proxy 한계가 prior에도 그대로 남는다.

역할은 **초기 모양 제공까지**다. 쿠팡이츠 주문이 쌓이면 Gamma-Poisson과 Time Model이 빠르게 덮어쓴다.

### 독립 가정

`rawShape = timeIndex × dayIndex`는 **요일 효과와 시간대 효과가 독립**이라고 가정한다. 실제로는 주말에 점심 피크가 늦게 서는 등 상호작용이 있다.

Time Model도 `hour × dayOfWeek` 교차를 쓰지 않으므로 **가정이 일관된다.** 데이터가 충분해지면 `weekend × hour` 정도만 candidate로 추가해 백테스트한다.

### prior 시간 해상도

`time_index`는 6개 밴드 단위다.

```
00~06 · 06~11 · 11~14 · 14~17 · 17~21 · 21~24
```

초기에는 같은 밴드 내부가 평평하다. `17시 = 18시 = 19시 = 20시`.

**당장 해결하지 않는다.** Credibility Baseline의 목적 자체가 *데이터 부족할 때 과하게 자신 있게 말하지 않는 안전한 초기 추정*이다. 실제 주문이 쌓이면 posterior가 시간별로 갈라진다.

> 개선 경로: 서울시 빅데이터캠퍼스의 신한카드 매출데이터가 **일자별·시간대별 · 블록 단위 · 63개 업종**으로 공개되어 있다(무료, 방문 분석 + 반출심사). 기간이 2017-01~2022-12라 코로나 시기가 크게 포함되는 한계가 있다. 배포 일정 밖 작업.

### 저볼륨 셀

prior rate가 낮은 셀은 `β`가 커서 오래 prior에 붙어 있다. 통계적으로는 옳은 동작이지만(그 표본으로 0.08과 0.15를 구분할 수 없다), 한산한 시간대에 prior 편향이 오래 남는다.

---

## 13. 선행 작업

| 작업 | 시급도 |
|---|---|
| `time` 컬럼 실물 확인 (§전제 ①) | **착수 전 필수** |
| 과거 예보 **보관 기간** integration test (§전제 ②) | **착수 전 필수** |
| 예보 아카이빙 착수 | 보험. API 보관 기간이 12개월에 미달하는 만큼 필요 |
| 정산 파일 보유 개월 수 확인 | Weather Model 가능 여부 결정 |
| 전처리에 **city 레벨 index 출력 추가** (§2.5) | City fallback의 전제 |
| **prior cutoff snapshot 생성** (§6) | 백테스트 이전 |
| `Store`에 요일별 영업시간 필드 추가 | Credibility 이전 |
| `α`, floor, 승격 임계값 초기값 결정 | 백테스트 단계 |

---

## 부록 — v1에서 재사용 / 폐기

### 재사용

| 항목 | 용도 |
|---|---|
| 전처리 파이프라인 | prior 원천. **audit CSV가 그대로 `MarketDemandPrior` 재료** |
| 외부 API 클라이언트 (KMA, AirKorea) | 변경 없음 |
| 캐시·동기화 인프라 | 변경 없음 |
| `ScoreStatusLevel` **클래스와 CSS** | 재사용. **단 임계값은 아래대로 폐기** |
| `simulate_score_distribution.py` | **백테스트 하네스로 전환**. 자바 로직 이식본이 이미 있어 v1 병렬 비교가 저렴 |

#### `ScoreStatusLevel` 임계값은 폐기한다

현재 값 `64 / 56 / 42 / 37`은 **v1 휴리스틱 점수의 실측 분포**에서 뽑은 것이다.
v2에서 배달온도는 **percentile 그 자체**라 이 숫자들은 의미를 잃는다.

percentile 공간에서는 **균등 구간이 정의상 옳다.** 20점 간격이면 각 구간이 정확히 20%가 된다.

```
v1  →  64 / 56 / 42 / 37     실측 분포 기반, v2에서 무의미
v2  →  80 / 60 / 40 / 20     percentile 기반, 각 구간 20%
```

v1이 원래 쓰던 20점 균등 구간이 **v1에서는 틀렸지만 v2에서는 맞다.**
점수의 의미가 바뀌었기 때문이다.

두 엔진이 공존하는 기간에는 임계값을 **모델 종류에 따라 분기**해야 한다.

### 폐기

| 항목 | 사유 |
|---|---|
| `WeightedScoreCalculator` 가산 결합 | 모델이 대체 |
| interaction 휴리스틱 | v1에서 가장 임의적. 주석에도 통계 효과가 아니라고 명시되어 있음 |
| `normalize()` 재척도 | 불필요 |

### 보류

v1의 강수량 임계값 `0 / 1 / 3 / 15 / 30`은 **candidate 경계로만 보존한다.**

물리적 근거가 있는 몇 안 되는 값이라 버리지 않지만, **v2 초기의 실제 feature는 §4의 소수 bucket**(`NO_RAIN / LIGHT / HEAVY`)이다. 12개월이어도 상위 구간 표본이 수십 시간 수준이라 5분할은 계수가 불안정하다.

데이터가 충분해지면 이 경계들을 세분화 candidate로 올려 백테스트한다.

---

## 원칙

```
LLM              설계 · 구현 · 코드 검토 지원
통계모델          실제 계수 계산
Backtest         모델 채택 여부 판단
verificationCases 구현 일치 보증
```

LLM이 가중치를 임의로 정하는 구조가 아니다.

**최종 정의**

> 배달온도 v2는 상권·업종 prior에서 시작해 실제 매장 주문 데이터가 쌓일수록
> Gamma-Poisson shrinkage와 회귀모델로 개인화 수준을 높이고,
> 검증된 경우에만 시간·날씨 모델로 승격하며,
> 향후 6시간 예상 주문수요를 OOS 예측분포 percentile로
> 0~100 배달온도로 표현하는 개인화 수요예측 시스템이다.
