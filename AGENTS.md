# Project Notes

## 기본 응답 규칙

- 기본 응답 언어는 한국어다.
- 사용자가 명시적으로 영어를 요청한 경우에만 영어로 답한다.
- 기술 용어(API, Database, Framework, Spring Security 등)는 일반적인 표기 그대로 사용해도 된다.
- 답변은 실용적인 실행 방법, 코드, 주의사항을 우선한다.

## 프로젝트 실행 기준

- 프로젝트 루트: `C:\Users\USER\Desktop\Military`
- Spring Boot + Gradle 프로젝트다.
- 기본 서버 주소는 `http://localhost:8080`이다.
- 실행 명령:

```powershell
.\gradlew.bat bootRun
```

- DB는 MySQL `military_db`를 사용한다.
- `.env`의 DB 설정을 기준으로 동작한다.
- OpenAI 추천 API 호출에는 `OPENAI_API_KEY`가 필요하다.

## Postman 테스트 대상 API

현재 공개된 근무 추천 API:

- `POST /api/work-schedules/preview`
- `POST /api/work-schedules/confirm`
- `GET /api/work-schedules/units/{unitId}/setting`
- `PUT /api/work-schedules/units/{unitId}/setting`

Preview 요청 예시:

```json
{
  "unitId": 2,
  "dutyDate": "2026-06-27",
  "dutyType": "불침번"
}
```

Confirm 요청은 `/preview` 응답의 `assignments`, `requestJson`, `responseJson`을 기반으로 만든다.
현재 구현은 `/preview`가 `ai_recommendation` 기록을 먼저 저장하고 `recommendationId`를 반환한다. `/confirm`은 `recommendationId`와 `assignments`만으로 확정할 수 있으며, 프론트가 `requestJson`, `responseJson`을 직접 관리하지 않아도 된다.

## 사용자가 제공한 DB 샘플 데이터 기준

사용자가 제공한 샘플 데이터는 앞으로 API 테스트 설명에서 기본 fixture로 간주한다.

### Unit 계층

- `1`: 1대대, 대대
- `2`: 1중대, 중대
- `3`: 1소대, 소대, parent `2`
- `4`: 2소대, 소대, parent `2`
- `5`: 1분대, 분대, parent `3`
- `6`: 2분대, 분대, parent `3`
- `7`: 1분대, 분대, parent `4`
- `8`: 2분대, 분대, parent `4`

근무 추천 요청은 보통 `unitId: 2`를 기준으로 잡는다. 코드의 `findAllSubUnitIds(2)`는 `2, 3, 4, 5, 6, 7, 8` 하위 부대를 포함한다.

### Users

- 총 50명, `user_id`는 `1`부터 `50`까지 존재한다.
- 실제 병사는 분대 단위 `unit_id` `5`, `6`, `7`, `8`에 소속되어 있다.
- 역할 값 예시:
  - `운전병`
  - `취사병`
  - `경계병`
  - `소총수`
- 상태 값 예시:
  - 정상 근무 가능: `부대내`
  - 제외 상태: `휴가`, `외출`, `외박`, `교육`, `훈련`, `입원`, `외진`

### 기존 근무 이력

`ai_recommendation`, `duty_assignment` 샘플 이력이 있다.

- `2026-06-24`
  - `불침번`: recommendation `1`, 4명
  - `위병소 근무`: recommendation `2`, 16건
- `2026-06-25`
  - `불침번`: recommendation `3`, 4명
  - `위병소 근무`: recommendation `4`, 16건
- `2026-06-26`
  - `불침번`: recommendation `5`, 4명
  - `위병소 근무`: recommendation `6`, 16건

기존 `duty_assignment.status` 값은 `승인`이다.

### 테스트 날짜 선택

- `2026-06-24`부터 `2026-06-26`까지는 이미 샘플 근무가 들어 있다.
- 새 추천 테스트는 충돌을 피하려면 `2026-06-27` 이후 날짜를 우선 사용한다.
- 단, 코드의 `DutyStatus.APPROVED` 값과 DB의 `승인` 문자열이 일치하는지 확인해야 한다. enum/상수 값이 다르면 연속 근무, 기존 승인 근무 검증이 의도와 다르게 동작할 수 있다.

## 중요한 주의사항

- 사용자가 제공한 SQL에는 `unit_setting` insert가 포함되어 있지 않다.
- `/api/work-schedules/preview`와 `/api/work-schedules/confirm`은 `unit_setting`이 반드시 필요하다.
- `unitId: 2`로 테스트하려면 `unit_setting`에 `unit_id = 2` 설정이 있어야 한다.
- `dutyType`은 `unit_setting.duty_type`과 정확히 같아야 한다.
- `assignments` 개수는 `requiredCount`와 같아야 한다.
- `confirm` 요청의 `role`은 DB의 `users.role`과 정확히 같아야 한다.

`unit_setting` 예시:

```sql
INSERT INTO unit_setting (
    unit_id,
    duty_type,
    required_count,
    start_time,
    end_time,
    lookback_days,
    prevent_consecutive,
    max_duty_count,
    exclude_statuses
)
VALUES (
    2,
    '불침번',
    4,
    '22:00:00',
    '06:30:00',
    7,
    true,
    5,
    '휴가,외출,외박,교육,훈련,입원,외진'
);
```

프론트 연동용 설정 API 예시:

```http
GET http://localhost:8080/api/work-schedules/units/2/setting
```

```http
PUT http://localhost:8080/api/work-schedules/units/2/setting
Content-Type: application/json
```

```json
{
  "dutyType": "불침번",
  "requiredCount": 4,
  "startTime": "22:00:00",
  "endTime": "06:30:00",
  "lookbackDays": 7,
  "preventConsecutive": true,
  "maxDutyCount": 5,
  "excludeStatuses": ["휴가", "외출", "외박", "교육", "훈련", "입원", "외진"]
}
```
