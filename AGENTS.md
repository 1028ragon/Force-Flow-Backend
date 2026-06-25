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
- `GET /api/work-schedules/candidates?unitId={unitId}&dutyDate={yyyy-MM-dd}&dutyType={dutyType}&slotOrder={slotOrder}&keyword={keyword}`
- `GET /api/work-schedules?unitId={unitId}&dutyDate={yyyy-MM-dd}`
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

## 추가된 날짜별 근무표 조회 API

프론트에서 날짜를 선택했을 때 기존에 확정 저장된 근무표를 조회하기 위한 API가 추가되어 있다.

- `GET /api/work-schedules?unitId={unitId}&dutyDate={yyyy-MM-dd}`
- `GET /api/work-schedules?unitId={unitId}&dutyDate={yyyy-MM-dd}&dutyType={dutyType}`

예시:

```http
GET http://localhost:8080/api/work-schedules?unitId=2&dutyDate=2026-06-27&dutyType=불침번
```

Postman Params 예시:

```text
unitId=2
dutyDate=2026-06-27
dutyType=불침번
```

`dutyType`은 선택값이다. 생략하면 해당 날짜의 모든 확정 근무를 조회한다.

응답 예시:

```json
{
  "unitId": 2,
  "dutyDate": "2026-06-27",
  "dutyType": "불침번",
  "assignmentCount": 4,
  "assignments": [
    {
      "dutyId": 7,
      "userId": 10,
      "unitId": 2,
      "name": "홍길동",
      "rankName": "상병",
      "role": "소총수",
      "dutyDate": "2026-06-27",
      "dutyType": "불침번",
      "startTime": "22:00:00",
      "endTime": "06:30:00",
      "status": "승인",
      "aiReason": "최근 근무 횟수가 적고 제외 상태가 아님",
      "approvedAt": "2026-06-25T14:00:00",
      "createdAt": "2026-06-25T14:00:00"
    }
  ]
}
```

해당 날짜에 확정된 근무가 없으면 `assignmentCount`는 `0`, `assignments`는 빈 배열이다.

현재 근무표 생성 흐름은 다음과 같다.

```text
날짜 선택
→ POST /api/work-schedules/preview
→ AI 추천 생성 및 ai_recommendation 미리보기 기록 저장
→ recommendationId와 assignments 반환
→ POST /api/work-schedules/confirm
→ duty_assignment에 승인 근무 저장
→ GET /api/work-schedules로 날짜별 기존 근무표 조회
```

## 슬롯 기반 부대 근무 초기설정

부대 근무 초기설정은 `workSchedule` 전용 테이블 기준으로 확장했다. 공통 Entity 파일은 수정하지 않는다.

추가된 전용 테이블:

- `work_schedule_setting`
- `work_schedule_time_slot`
- `work_schedule_slot_role`

설정 저장 API:

```http
PUT http://localhost:8080/api/work-schedules/units/2/setting
Content-Type: application/json
```

요청 예시:

```json
{
  "dutyType": "불침번",
  "description": "생활관 야간 경계 근무",
  "timeSlots": [
    {
      "slotOrder": 1,
      "startTime": "22:00:00",
      "endTime": "00:00:00",
      "requiredCount": 1,
      "allowedRoles": ["소총수", "취사병", "운전병"]
    },
    {
      "slotOrder": 2,
      "startTime": "00:00:00",
      "endTime": "02:00:00",
      "requiredCount": 1,
      "allowedRoles": ["소총수", "취사병", "운전병"]
    },
    {
      "slotOrder": 3,
      "startTime": "02:00:00",
      "endTime": "04:00:00",
      "requiredCount": 1,
      "allowedRoles": ["소총수", "취사병", "운전병"]
    },
    {
      "slotOrder": 4,
      "startTime": "04:00:00",
      "endTime": "06:00:00",
      "requiredCount": 1,
      "allowedRoles": ["소총수", "취사병", "운전병"]
    }
  ],
  "lookbackDays": 7,
  "preventConsecutive": true,
  "maxDutyCount": 5,
  "excludeStatuses": ["휴가", "외출", "외박", "교육", "훈련", "입원", "외진"]
}
```

전체 `requiredCount`, 전체 `startTime`, 전체 `endTime`은 프론트가 직접 보내지 않는다. 백엔드는 `timeSlots` 기준으로 다음 값을 계산한다.

- 전체 필요 인원: 모든 `timeSlots.requiredCount` 합계
- 전체 시작 시간: 가장 앞 `slotOrder`의 `startTime`
- 전체 종료 시간: 가장 뒤 `slotOrder`의 `endTime`

`POST /api/work-schedules/preview`는 저장된 슬롯 설정을 읽어 초번별 `slotOrder`, `startTime`, `endTime`, `requiredCount`, `allowedRoles` 조건에 맞춰 추천 결과를 만든다. `POST /api/work-schedules/confirm`은 프론트가 보낸 초번별 배정이 설정의 시간대별 필요 인원과 허용 역할에 맞는지 검증한 뒤 `duty_assignment`에 병사별 실제 분할 시간으로 저장한다.

병사 수정용 후보 검색 API:

```http
GET http://localhost:8080/api/work-schedules/candidates?unitId=2&dutyDate=2026-06-30&dutyType=불침번&slotOrder=1&keyword=배
```

프론트는 이름으로 후보를 검색하되, 최종 confirm 요청에는 반드시 선택한 병사의 `userId`를 보낸다. `slotOrder`를 함께 보내면 해당 시간대의 `allowedRoles` 기준으로 후보가 필터링된다.

Confirm 요청은 `/preview` 응답의 `assignments`, `requestJson`, `responseJson`을 기반으로 만든다.
현재 구현은 `/preview`가 `ai_recommendation` 기록을 먼저 저장하고 `recommendationId`를 반환한다. `requestJson`, `responseJson`은 백엔드 DB 기록용으로만 저장하며 preview 응답에는 내려주지 않는다. `/confirm`은 `recommendationId`와 `assignments`만으로 확정할 수 있으며, 프론트가 `requestJson`, `responseJson`을 직접 관리하지 않아도 된다.

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
- `confirm` 요청의 assignment에는 `role`, `unitId`, `dutyDate`, `dutyType`을 보내지 않아도 된다. 백엔드는 상위 confirm 값과 `userId`로 조회한 DB의 `users.role`을 기준으로 검증한다.

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
