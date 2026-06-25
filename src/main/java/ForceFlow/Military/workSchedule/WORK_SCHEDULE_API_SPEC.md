# WorkSchedule API 명세서

기본 주소:

```http
http://localhost:8080
```

공통 기준:

- `unitId: 2`는 1중대 기준 예시다.
- `dutyDate`는 `yyyy-MM-dd` 형식이다.
- `role`은 confirm 요청에서 생략 가능하다. 백엔드가 `userId`로 DB의 실제 역할을 조회한다.
- `requestJson`, `responseJson`은 백엔드 DB 기록용이며 preview 응답에는 내려주지 않는다.

## 1. 부대 근무 초기설정 저장

부대별, 근무 종류별 시간대 분할 설정을 저장한다.

```http
PUT /api/work-schedules/units/{unitId}/setting
Content-Type: application/json
```

예시:

```http
PUT /api/work-schedules/units/2/setting
```

Request:

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
    }
  ],
  "lookbackDays": 7,
  "preventConsecutive": true,
  "maxDutyCount": 5,
  "excludeStatuses": ["휴가", "외출", "외박", "교육", "훈련", "입원", "외진"]
}
```

Response:

```json
{
  "settingId": 1,
  "unitId": 2,
  "dutyType": "불침번",
  "description": "생활관 야간 경계 근무",
  "requiredCount": 2,
  "startTime": "22:00:00",
  "endTime": "02:00:00",
  "timeSlots": [
    {
      "slotId": 1,
      "slotOrder": 1,
      "startTime": "22:00:00",
      "endTime": "00:00:00",
      "requiredCount": 1,
      "allowedRoles": ["소총수", "취사병", "운전병"]
    }
  ],
  "lookbackDays": 7,
  "preventConsecutive": true,
  "maxDutyCount": 5,
  "excludeStatuses": ["휴가", "외출", "외박", "교육", "훈련", "입원", "외진"],
  "createdAt": "2026-06-25T18:00:00",
  "updatedAt": null
}
```

설명:

- `timeSlots[].requiredCount`: 해당 시간대에 필요한 총 인원 수
- `timeSlots[].allowedRoles`: 해당 시간대에 배정 가능한 역할 목록
- 전체 `requiredCount`는 모든 slot의 `requiredCount` 합계로 계산된다.
- 전체 `startTime`은 가장 앞 `slotOrder`의 `startTime`이다.
- 전체 `endTime`은 가장 뒤 `slotOrder`의 `endTime`이다.

## 2. 부대 근무 초기설정 조회

```http
GET /api/work-schedules/units/{unitId}/setting
```

예시:

```http
GET /api/work-schedules/units/2/setting
```

Response:

```json
{
  "settingId": 1,
  "unitId": 2,
  "dutyType": "불침번",
  "description": "생활관 야간 경계 근무",
  "requiredCount": 4,
  "startTime": "22:00:00",
  "endTime": "06:00:00",
  "timeSlots": [],
  "lookbackDays": 7,
  "preventConsecutive": true,
  "maxDutyCount": 5,
  "excludeStatuses": ["휴가", "외출", "외박", "교육", "훈련", "입원", "외진"],
  "createdAt": "2026-06-25T18:00:00",
  "updatedAt": "2026-06-25T18:10:00"
}
```

주의:

- 현재 조회 API는 `unitId` 기준 대표 설정을 조회한다.
- 여러 근무 종류를 저장할 수 있지만, 다중 설정 조회 API는 아직 별도로 없다.

## 3. AI 미리보기 생성

저장된 근무 초기설정을 기준으로 AI 추천 근무표를 생성한다.

```http
POST /api/work-schedules/preview
Content-Type: application/json
```

Request:

```json
{
  "unitId": 2,
  "dutyDate": "2026-06-30",
  "dutyType": "불침번"
}
```

Response:

```json
{
  "recommendationId": 21,
  "unitId": 2,
  "dutyDate": "2026-06-30",
  "dutyType": "불침번",
  "requiredCount": 4,
  "startTime": "22:00:00",
  "endTime": "06:00:00",
  "status": "미리보기",
  "warningMessage": null,
  "assignments": [
    {
      "slotOrder": 1,
      "userId": 35,
      "unitId": 2,
      "name": "배지율",
      "rankName": "일병",
      "role": "소총수",
      "dutyDate": "2026-06-30",
      "dutyType": "불침번",
      "startTime": "22:00:00",
      "endTime": "00:00:00",
      "status": "미리보기",
      "aiReason": "최근 근무 횟수가 적고 일정 충돌이 없습니다."
    }
  ]
}
```

설명:

- preview 호출 시 `ai_recommendation` 기록이 먼저 저장되고 `recommendationId`가 반환된다.
- 프론트는 confirm 시 `recommendationId`를 사용한다.
- `assignments[].slotOrder`, `startTime`, `endTime`은 초기설정의 slot 기준으로 내려간다.

## 4. 병사 후보 검색

프론트에서 이름으로 병사를 검색하고, 선택한 병사의 `userId`를 confirm에 사용하기 위한 API다.

```http
GET /api/work-schedules/candidates
```

Query Parameters:

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `unitId` | O | 부대 ID |
| `dutyDate` | O | 근무 날짜 |
| `dutyType` | O | 근무 종류 |
| `slotOrder` | X | 특정 시간대 번호 |
| `keyword` | X | 이름 검색어 |

예시:

```http
GET /api/work-schedules/candidates?unitId=2&dutyDate=2026-06-30&dutyType=불침번&slotOrder=1&keyword=배
```

Response:

```json
{
  "unitId": 2,
  "dutyDate": "2026-06-30",
  "dutyType": "불침번",
  "slotOrder": 1,
  "startTime": "22:00:00",
  "endTime": "00:00:00",
  "requiredCount": 1,
  "allowedRoles": ["소총수", "취사병", "운전병"],
  "candidates": [
    {
      "userId": 35,
      "unitId": 5,
      "name": "배지율",
      "rankName": "일병",
      "role": "소총수",
      "currentStatus": "부대내",
      "recentDutyCount": 1,
      "workedYesterday": false,
      "hasScheduleConflict": false,
      "eligible": true
    }
  ]
}
```

프론트 처리:

- 화면에는 `name`, `rankName`, `role`, `currentStatus`, `eligible`을 표시한다.
- 사용자가 병사를 선택하면 `userId`를 저장한다.
- confirm 요청에는 이름이 아니라 `userId`를 보낸다.
- `eligible=false`인 병사는 선택 불가 처리하는 것을 권장한다.

## 5. 미리보기 승인

미리보기 결과를 확정 저장한다. 프론트에서 병사를 수정했다면 수정된 `userId`를 보내면 된다.

```http
POST /api/work-schedules/confirm
Content-Type: application/json
```

Request:

```json
{
  "recommendationId": 21,
  "unitId": 2,
  "dutyDate": "2026-06-30",
  "dutyType": "불침번",
  "requiredCount": 4,
  "startTime": "22:00:00",
  "endTime": "06:00:00",
  "assignments": [
    {
      "slotOrder": 1,
      "userId": 35,
      "startTime": "22:00:00",
      "endTime": "00:00:00",
      "aiReason": "AI 추천 배정"
    },
    {
      "slotOrder": 2,
      "userId": 34,
      "startTime": "00:00:00",
      "endTime": "02:00:00",
      "aiReason": "AI 추천 배정"
    }
  ]
}
```

Response:

```json
{
  "recommendationId": 21,
  "unitId": 2,
  "dutyDate": "2026-06-30",
  "dutyType": "불침번",
  "requiredCount": 4,
  "startTime": "22:00:00",
  "endTime": "06:00:00",
  "status": "승인",
  "warningMessage": null,
  "assignments": [],
  "createdAt": "2026-06-25T18:30:00"
}
```

검증 규칙:

- `recommendationId`가 존재해야 한다.
- `unitId`, `dutyDate`, `dutyType`, `requiredCount`, `startTime`, `endTime`이 preview 기록과 일치해야 한다.
- 같은 confirm 요청 안에서 동일 병사를 중복 배정할 수 없다.
- 병사는 요청 부대 또는 하위 부대 소속이어야 한다.
- 병사의 DB `role`이 해당 slot의 `allowedRoles`에 포함되어야 한다.
- 제외 상태 병사는 승인할 수 없다.
- 일정 충돌 병사는 승인할 수 없다.
- 같은 날짜에 이미 승인된 근무가 있는 병사는 승인할 수 없다.
- `preventConsecutive=true`이면 전날 승인 근무자는 승인할 수 없다.

## 6. 날짜별 확정 근무표 조회

```http
GET /api/work-schedules
```

Query Parameters:

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `unitId` | O | 부대 ID |
| `dutyDate` | O | 근무 날짜 |
| `dutyType` | X | 근무 종류 |

예시:

```http
GET /api/work-schedules?unitId=2&dutyDate=2026-06-30&dutyType=불침번
```

Response:

```json
{
  "unitId": 2,
  "dutyDate": "2026-06-30",
  "dutyType": "불침번",
  "assignmentCount": 4,
  "assignments": [
    {
      "dutyId": 77,
      "userId": 35,
      "unitId": 2,
      "name": "배지율",
      "rankName": "일병",
      "role": "소총수",
      "dutyDate": "2026-06-30",
      "dutyType": "불침번",
      "startTime": "22:00:00",
      "endTime": "00:00:00",
      "status": "승인",
      "aiReason": "AI 추천 배정",
      "approvedAt": "2026-06-25T18:30:00",
      "createdAt": "2026-06-25T18:30:00"
    }
  ]
}
```

설명:

- 같은 날짜/근무종류에 여러 승인 기록이 있으면 가장 최신 승인 추천건의 배정만 반환한다.
- `dutyType`을 생략하면 해당 날짜의 최신 승인 추천건 기준으로 조회된다.

## 7. 오류 응답 예시

### 추천 기록 없음

```json
{
  "timestamp": "2026-06-25T18:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "AI 추천 기록을 찾을 수 없습니다."
}
```

원인:

- confirm의 `recommendationId`가 preview 응답값과 다르다.
- DB에 해당 추천 기록이 없다.

### 시간대별 필요 인원 불일치

```json
{
  "status": 400,
  "message": "승인 배정의 시간대별 필요 인원이 부대 근무 설정과 일치하지 않습니다."
}
```

원인:

- slot의 `requiredCount`보다 적거나 많은 병사를 보냈다.

### 시간대별 허용 역할 불일치

```json
{
  "status": 400,
  "message": "승인 배정의 시간대별 허용 역할이 부대 근무 설정과 일치하지 않습니다."
}
```

원인:

- slot의 `allowedRoles`에 없는 역할의 병사를 보냈다.

### 동일 병사 중복

```json
{
  "status": 400,
  "message": "동일 병사가 중복 배정되었습니다."
}
```

원인:

- 같은 confirm 요청 안에 같은 `userId`가 2번 이상 들어갔다.
