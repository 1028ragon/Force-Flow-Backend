# 근무표 Postman 테스트 흐름

## 전체 흐름

```text
1. 부대 근무 초기설정 저장
2. 부대 근무 초기설정 조회
3. AI 미리보기 생성
4-A. 미리보기 그대로 승인
   또는
4-B. 병사 수정 후 승인
5. 병사 이름 검색 후 userId 선택
6. 날짜별 확정 근무표 조회
```

`4-A`와 `4-B`는 둘 중 하나만 실행한다.

## 0. 서버 실행

### Request

```powershell
.\gradlew.bat bootRun
```

### Response 확인

서버가 정상 실행되면 기본 주소로 요청할 수 있다.

```http
http://localhost:8080
```

## 1. 부대 근무 초기설정 저장

불침번 시간대 분할, 시간대별 필요 역할/인원, 제외 상태, 공정성 규칙을 저장한다.

### Request

```http
PUT http://localhost:8080/api/work-schedules/units/2/setting
Content-Type: application/json
```

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

### Response 확인

```json
{
  "settingId": 1,
  "unitId": 2,
  "dutyType": "불침번",
  "description": "생활관 야간 경계 근무",
  "requiredCount": 4,
  "startTime": "22:00:00",
  "endTime": "06:00:00",
  "timeSlots": [
    {
      "slotOrder": 1,
      "startTime": "22:00:00",
      "endTime": "00:00:00",
      "requiredCount": 1,
      "allowedRoles": ["소총수", "취사병", "운전병"]
    }
  ]
}
```

확인할 값:

- `requiredCount`가 `4`
- `startTime`이 `22:00:00`
- `endTime`이 `06:00:00`
- `timeSlots`가 4개

## 2. 부대 근무 초기설정 조회

### Request

```http
GET http://localhost:8080/api/work-schedules/units/2/setting
```

### Response 확인

확인할 값:

- 저장한 `timeSlots`가 그대로 내려오는지
- `dutyType`이 `불침번`인지
- `excludeStatuses`, `lookbackDays`, `preventConsecutive`, `maxDutyCount`가 맞는지

## 3. AI 미리보기 생성

### Request

```http
POST http://localhost:8080/api/work-schedules/preview
Content-Type: application/json
```

```json
{
  "unitId": 2,
  "dutyDate": "2026-06-28",
  "dutyType": "불침번"
}
```

### Response 확인

```json
{
  "recommendationId": 16,
  "unitId": 2,
  "dutyDate": "2026-06-28",
  "dutyType": "불침번",
  "requiredCount": 4,
  "startTime": "22:00:00",
  "endTime": "06:00:00",
  "status": "미리보기",
  "assignments": [
    {
      "slotOrder": 1,
      "userId": 35,
      "unitId": 2,
      "name": "배지율",
      "rankName": "일병",
      "role": "소총수",
      "dutyDate": "2026-06-28",
      "dutyType": "불침번",
      "startTime": "22:00:00",
      "endTime": "00:00:00",
      "status": "미리보기",
      "aiReason": "최근 근무 횟수가 적고 어제 근무하지 않았으며 일정 충돌이 없습니다."
    }
  ]
}
```

확인할 값:

- `recommendationId`가 있음
- `requiredCount`가 `4`
- `assignments`가 4개
- 각 배정에 `slotOrder`가 있음
- 시간대가 아래처럼 나뉨
  - `22:00:00` ~ `00:00:00`
  - `00:00:00` ~ `02:00:00`
  - `02:00:00` ~ `04:00:00`
  - `04:00:00` ~ `06:00:00`

다음 단계에서 사용할 값:

- `recommendationId`
- `unitId`
- `dutyDate`
- `dutyType`
- `requiredCount`
- `startTime`
- `endTime`
- `assignments[].slotOrder`
- `assignments[].userId`
- `assignments[].startTime`
- `assignments[].endTime`
- `assignments[].aiReason`

## 4-A. 미리보기 그대로 승인

`preview` 응답의 값을 그대로 사용해서 승인한다.

### Request

```http
POST http://localhost:8080/api/work-schedules/confirm
Content-Type: application/json
```

`recommendationId`와 `userId`는 반드시 실제 preview 응답값으로 바꾼다.

```json
{
  "recommendationId": 16,
  "unitId": 2,
  "dutyDate": "2026-06-28",
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
    },
    {
      "slotOrder": 3,
      "userId": 38,
      "startTime": "02:00:00",
      "endTime": "04:00:00",
      "aiReason": "AI 추천 배정"
    },
    {
      "slotOrder": 4,
      "userId": 44,
      "startTime": "04:00:00",
      "endTime": "06:00:00",
      "aiReason": "AI 추천 배정"
    }
  ]
}
```

### Response 확인

확인할 값:

- `status`가 `승인`
- `assignments`가 4개
- 각 assignment의 `startTime`, `endTime`이 초번별 시간으로 저장됨
- `role`을 요청에 보내지 않아도 정상 승인됨

주의:

- `role`은 보내지 않아도 된다.
- 백엔드가 `userId`로 병사를 조회해서 DB의 `role`을 자동 확인한다.
- `role`을 보냈는데 DB의 실제 role과 다르면 승인 실패한다.

## 4-B. 병사 수정 후 승인

미리보기 병사를 프론트에서 수정하는 상황을 테스트한다.

`4-A` 대신 실행한다. `4-A`와 `4-B`를 같은 날짜에 둘 다 실행하면 같은 날짜에 승인 데이터가 중복으로 생긴다.

병사를 수정할 때는 먼저 5번 후보 검색 API로 병사를 검색하고, 선택한 병사의 `userId`를 confirm에 넣는다.

### Request

```http
POST http://localhost:8080/api/work-schedules/confirm
Content-Type: application/json
```

아래 예시는 1번초 병사를 `userId: 43`으로 바꾼 경우다.

```json
{
  "recommendationId": 16,
  "unitId": 2,
  "dutyDate": "2026-06-28",
  "dutyType": "불침번",
  "requiredCount": 4,
  "startTime": "22:00:00",
  "endTime": "06:00:00",
  "assignments": [
    {
      "slotOrder": 1,
      "userId": 43,
      "startTime": "22:00:00",
      "endTime": "00:00:00",
      "aiReason": "관리자 수정 배정"
    },
    {
      "slotOrder": 2,
      "userId": 34,
      "startTime": "00:00:00",
      "endTime": "02:00:00",
      "aiReason": "AI 추천 배정"
    },
    {
      "slotOrder": 3,
      "userId": 38,
      "startTime": "02:00:00",
      "endTime": "04:00:00",
      "aiReason": "AI 추천 배정"
    },
    {
      "slotOrder": 4,
      "userId": 44,
      "startTime": "04:00:00",
      "endTime": "06:00:00",
      "aiReason": "AI 추천 배정"
    }
  ]
}
```

### Response 확인

성공 조건:

- 변경한 `userId`가 존재함
- 요청 부대 또는 하위 부대 소속임
- DB의 `users.role`이 해당 시간대의 허용 역할에 포함됨
- 제외 상태가 아님
- 일정 충돌이 없음
- 같은 날짜에 이미 승인된 근무가 없음

실패하는 경우:

- 1번초의 `allowedRoles`에 없는 role의 userId를 넣음
- 같은 userId를 두 슬롯에 중복 배정함
- 해당 병사가 같은 날짜에 이미 승인된 근무가 있음

## 5. 병사 이름 검색 후 userId 선택

프론트에서 이름으로 검색하고, 사용자가 선택한 병사의 `userId`를 confirm 요청에 넣기 위한 API다.

### Request

특정 slot 기준으로 검색:

```http
GET http://localhost:8080/api/work-schedules/candidates?unitId=2&dutyDate=2026-06-30&dutyType=불침번&slotOrder=1&keyword=배
```

전체 허용 역할 기준으로 검색:

```http
GET http://localhost:8080/api/work-schedules/candidates?unitId=2&dutyDate=2026-06-30&dutyType=불침번&keyword=배
```

### Response 확인

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

- 화면에는 `name`, `rankName`, `role`, `currentStatus`를 보여준다.
- 사용자가 병사를 선택하면 내부적으로 `userId`를 저장한다.
- confirm 요청에는 선택한 병사의 `userId`를 넣는다.
- 가능하면 `eligible=true`인 병사만 선택 가능하게 한다.

주의:

- 이름은 중복될 수 있으므로 confirm에 이름을 보내면 안 된다.
- confirm에는 반드시 `userId`를 보내야 한다.
- `slotOrder`를 보내면 해당 시간대의 `allowedRoles` 기준으로 후보가 필터링된다.

## 6. 날짜별 확정 근무표 조회

### Request

```http
GET http://localhost:8080/api/work-schedules?unitId=2&dutyDate=2026-06-28&dutyType=불침번
```

### Response 확인

```json
{
  "unitId": 2,
  "dutyDate": "2026-06-28",
  "dutyType": "불침번",
  "assignmentCount": 4,
  "assignments": [
    {
      "userId": 35,
      "role": "소총수",
      "startTime": "22:00:00",
      "endTime": "00:00:00",
      "status": "승인"
    },
    {
      "userId": 34,
      "role": "소총수",
      "startTime": "00:00:00",
      "endTime": "02:00:00",
      "status": "승인"
    }
  ]
}
```

확인할 값:

- `assignmentCount`가 `4`
- 최신 승인 추천건 하나의 배정만 내려옴
- 각 배정의 시간이 초번별로 나뉨
- `status`가 `승인`

## 6. 자주 나는 오류

### AI 추천 기록을 찾을 수 없습니다

#### Response

```json
{
  "status": 400,
  "message": "AI 추천 기록을 찾을 수 없습니다."
}
```

#### 원인

- confirm의 `recommendationId`가 preview 응답값과 다름
- 서버 또는 DB가 바뀌어서 해당 추천 기록이 없음

#### 해결

- preview를 다시 호출한다.
- 새로 받은 `recommendationId`로 confirm을 보낸다.

### 시간대별 허용 역할 또는 필요 인원이 설정과 일치하지 않습니다

#### 원인

- 설정의 `allowedRoles`에 없는 역할의 병사를 넣음
- 특정 slot에 필요한 인원보다 많이 넣거나 적게 넣음
- `slotOrder`, `startTime`, `endTime` 중 하나가 설정과 다름

#### 해결

- 설정의 `timeSlots`와 confirm의 `assignments`를 비교한다.
- 해당 시간대의 `allowedRoles`에 포함되는 role을 가진 병사를 선택한다.

### 동일 병사가 중복 배정되었습니다

#### 원인

- 같은 `userId`가 confirm 요청의 `assignments`에 2번 이상 들어감

#### 해결

- 각 슬롯에 서로 다른 병사를 넣는다.

### 이미 승인된 동일 근무가 있는 병사가 포함되어 있습니다

#### 원인

- 해당 병사가 같은 날짜에 이미 승인된 근무를 가지고 있음

#### 해결

- 다른 병사를 선택한다.
- 또는 새 테스트 날짜를 사용한다.

## 7. 추천 테스트 날짜

기존 테스트 데이터와 충돌을 피하려면 새 날짜를 사용한다.

```text
2026-06-28
2026-06-29
2026-06-30
```

이미 테스트를 여러 번 했다면 하루씩 뒤로 미뤄서 테스트한다.
