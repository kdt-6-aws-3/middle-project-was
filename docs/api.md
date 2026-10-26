# PetClinic REST API

WAS(Spring Boot)는 화면을 제공하지 않고 JSON API만 제공합니다. DB 구조는 `src/main/resources/db/mysql/schema.sql`(병원·수의사·보호자·반려동물·진료 기록·진료 예약)을 따릅니다.

- 기본 경로: `/api` (WEB의 Apache가 `/api` 요청만 내부 ALB를 거쳐 WAS로 전달)
- 형식: 요청·응답 모두 `application/json`, 문자 인코딩 UTF-8
- 날짜: `yyyy-MM-dd` (예: `2026-10-21`)
- 헬스 체크: `GET /actuator/health` → `{"status":"UP"}` (내부 ALB 대상 그룹용, `/api` 아님)

## 목록

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/owners?name={이름}&page={번호}&size={개수}` | 보호자 검색 (이름으로 시작, 페이지) |
| GET | `/api/owners/{ownerId}` | 보호자 상세 (반려동물·진료 기록 포함) |
| POST | `/api/owners` | 보호자 등록 |
| PUT | `/api/owners/{ownerId}` | 보호자 수정 |
| DELETE | `/api/owners/{ownerId}` | 보호자 삭제 (반려동물·진료 기록·예약도 함께 삭제) |
| GET | `/api/owners/{ownerId}/pets/{petId}` | 반려동물 상세 (진료 기록 포함) |
| POST | `/api/owners/{ownerId}/pets` | 반려동물 등록 |
| PUT | `/api/owners/{ownerId}/pets/{petId}` | 반려동물 수정 |
| DELETE | `/api/owners/{ownerId}/pets/{petId}` | 반려동물 삭제 (진료 기록·예약도 함께 삭제) |
| GET | `/api/owners/{ownerId}/pets/{petId}/visits` | 진료 기록 목록 |
| POST | `/api/owners/{ownerId}/pets/{petId}/visits` | 진료 기록 등록 |
| PUT | `/api/owners/{ownerId}/pets/{petId}/visits/{visitId}` | 진료 기록 수정 |
| DELETE | `/api/owners/{ownerId}/pets/{petId}/visits/{visitId}` | 진료 기록 삭제 |
| GET | `/api/pettypes` | 동물 종류 목록 |
| GET | `/api/hospitals` | 병원 목록 |
| GET | `/api/hospitals/{hospitalId}` | 병원 상세 |
| GET | `/api/hospitals/{hospitalId}/vets` | 병원 소속 수의사 목록 |
| GET | `/api/vets` | 수의사 목록 (병원·전문 분야 포함, 캐시) |
| GET | `/api/appointments?hospitalId=&vetId=&petId=&status=&page=&size=` | 진료 예약 목록 (조건은 모두 선택, 페이지 단위) |
| GET | `/api/appointments/{appointmentId}` | 진료 예약 상세 |
| POST | `/api/appointments` | 진료 예약 등록 |
| PUT | `/api/appointments/{appointmentId}` | 진료 예약 변경 (병원·반려동물·수의사·일시·사유) |
| PATCH | `/api/appointments/{appointmentId}/status` | 진료 예약 상태 변경 |
| DELETE | `/api/appointments` | 진료 예약 **전체** 삭제 |
| DELETE | `/api/appointments/{appointmentId}` | 진료 예약 삭제 |
| GET | `/api/vets/{vetId}/appointments?date=` | 수의사의 하루 예약 일정 |

병원·수의사·동물 종류·전문 분야는 화면에서 고르는 기준 데이터라 조회만 제공합니다. 데이터는 DB 담당이 관리합니다.

## 보호자

### GET `/api/owners`

| 파라미터 | 기본값 | 설명 |
| --- | --- | --- |
| `name` | 빈 값 (전체) | 이름이 이 값으로 시작하는 보호자 (예: `김` → 성이 김인 보호자) |
| `page` | `1` | 1부터 시작 |
| `size` | `5` | 한 페이지 개수 (최대 100) |

`page`가 1보다 작으면 1로, `size`는 1~100 범위로 보정합니다. 마지막 페이지를 넘으면 `content`가 빈 배열입니다.

응답 `200`

```json
{
  "content": [
    {
      "id": 1,
      "name": "김지훈",
      "address": "서울특별시 강남구 가상로 201",
      "phone": "000-0000-2001",
      "pets": ["나비", "초코"]
    }
  ],
  "page": 1,
  "size": 5,
  "totalElements": 1,
  "totalPages": 1
}
```

- 결과가 없으면 `content`가 빈 배열입니다. (화면에서 "찾을 수 없음" 안내)
- 결과가 1건일 때 상세 화면으로 바로 이동하는 동작은 화면(React)에서 처리합니다.

### GET `/api/owners/{ownerId}`

응답 `200`

```json
{
  "id": 1,
  "name": "김지훈",
  "address": "서울특별시 강남구 가상로 201",
  "phone": "000-0000-2001",
  "pets": [
    {
      "id": 1,
      "name": "초코",
      "birthDate": "2021-03-15",
      "type": { "id": 1, "name": "개" },
      "visits": [
        { "id": 1, "date": "2026-09-01", "description": "정기 건강검진 실시. 체중과 전반적인 건강 상태 확인.", "prescription": null }
      ]
    }
  ]
}
```

없는 보호자면 `404`.

### POST `/api/owners`

요청

```json
{ "name": "정현성", "address": "서울특별시 강남구", "phone": "010-1234-5678" }
```

| 필드 | 규칙 |
| --- | --- |
| `name` | 필수, 최대 255자 |
| `address` | 필수, 최대 255자 |
| `phone` | 필수, `숫자2~3-숫자3~4-숫자4` 형식 (예: `010-1234-5678`, `02-123-4567`) |

응답 `201`, `Location: /api/owners/{새 ID}`, 본문은 보호자 상세와 같은 형식

### PUT `/api/owners/{ownerId}`

요청·규칙은 등록과 같습니다. 응답 `200`(보호자 상세), 없는 보호자면 `404`.

### DELETE `/api/owners/{ownerId}`

보호자의 반려동물과 그 진료 기록·진료 예약까지 함께 삭제합니다. 응답 `204`(본문 없음), 없는 보호자면 `404`.

## 동물 종류

### GET `/api/pettypes`

응답 `200`: 이름순

```json
[ { "id": 3, "name": "토끼" }, { "id": 4, "name": "햄스터" } ]
```

## 반려동물

### GET `/api/owners/{ownerId}/pets/{petId}`

응답 `200`: 보호자 상세의 `pets` 항목과 같은 형식. 보호자나 반려동물이 없으면 `404`.

### POST `/api/owners/{ownerId}/pets`

요청

```json
{ "name": "초코", "birthDate": "2021-03-15", "typeId": 1 }
```

| 필드 | 규칙 |
| --- | --- |
| `name` | 필수, 최대 255자 (같은 보호자 안에서도 이름 중복 허용) |
| `birthDate` | 필수, 오늘 이후 날짜 불가 |
| `typeId` | 필수, `/api/pettypes`의 ID |

응답 `201`, `Location: /api/owners/{ownerId}/pets/{새 ID}`, 본문은 반려동물 상세. 없는 보호자면 `404`.

### PUT `/api/owners/{ownerId}/pets/{petId}`

요청·규칙은 등록과 같습니다. 응답 `200`(반려동물 상세).

### DELETE `/api/owners/{ownerId}/pets/{petId}`

반려동물과 그 진료 기록·진료 예약을 함께 삭제합니다. 응답 `204`, 보호자나 반려동물이 없으면 `404`.

## 진료 기록

### GET `/api/owners/{ownerId}/pets/{petId}/visits`

응답 `200`: 날짜순

```json
[
  { "id": 1, "date": "2026-09-01", "description": "정기 건강검진 실시.", "prescription": null },
  { "id": 3, "date": "2026-09-03", "description": "귀 가려움과 분비물로 내원. …", "prescription": "수로란(Surolan)" }
]
```

### POST `/api/owners/{ownerId}/pets/{petId}/visits`

요청

```json
{ "date": "2026-10-22", "description": "외이염 진료", "prescription": "수로란(Surolan)" }
```

| 필드 | 규칙 |
| --- | --- |
| `date` | 필수, **오늘 또는 과거** 날짜 (진료 후 기록하므로 미래 날짜는 `400` `visit.dateInFuture`). 종이 기록을 나중에 입력할 수 있도록 과거 날짜는 제한 없음 |
| `description` | 필수, 최대 255자 |
| `prescription` | 선택, 최대 255자. 처방 약품 내역. 비우거나 공백만 보내면 "처방 없음"(`null`)으로 저장 |

응답 `201`, 본문은 등록된 진료 기록.

### PUT `/api/owners/{ownerId}/pets/{petId}/visits/{visitId}`

요청·규칙은 등록과 같습니다(날짜는 오늘 또는 과거). 응답 `200`(진료 기록), 없으면 `404`.

### DELETE `/api/owners/{ownerId}/pets/{petId}/visits/{visitId}`

응답 `204`, 없으면 `404`.

## 병원

### GET `/api/hospitals`

응답 `200`: ID순

```json
[
  { "id": 1, "name": "펫클리닉 강남점", "address": "서울특별시 강남구 테스트로 101", "phone": "000-0000-1001" }
]
```

### GET `/api/hospitals/{hospitalId}`

응답 `200`: 위 목록 항목과 같은 형식. 없으면 `404`.

### GET `/api/hospitals/{hospitalId}/vets`

응답 `200`: 수의사 목록과 같은 형식. 병원이 없으면 `404`.

## 수의사

### GET `/api/vets`

응답 `200`

```json
[
  {
    "id": 1,
    "name": "김민준",
    "hospital": { "id": 1, "name": "펫클리닉 강남점", "address": "서울특별시 강남구 테스트로 101", "phone": "000-0000-1001" },
    "specialties": [ { "id": 1, "name": "내과" }, { "id": 3, "name": "피부과" } ]
  }
]
```

## 진료 예약

- 일시: `yyyy-MM-ddTHH:mm:ss` (예: `2026-10-28T10:30:00`), 서버 시간대(Asia/Seoul) 기준
- **30분 단위**: 예약은 정각 또는 30분(`:00`, `:30`)에만 시작합니다.
- **중복 금지**: 같은 수의사, 같은 반려동물은 같은 시각에 예약이 하나만 가능합니다. 취소(`CANCELLED`)된 예약은 빼고 따지므로, 취소된 시간은 다시 예약할 수 있습니다.
- 상태: `REQUESTED`(요청) · `CONFIRMED`(확정) · `CANCELLED`(취소) · `COMPLETED`(완료). `CANCELLED`·`COMPLETED`는 최종 상태라 더 이상 변경할 수 없습니다.

### GET `/api/appointments`

| 파라미터 | 설명 |
| --- | --- |
| `hospitalId` | 이 병원의 예약만 |
| `vetId` | 이 수의사의 예약만 |
| `petId` | 이 반려동물의 예약만 |
| `status` | 이 상태의 예약만 (위 4가지 값, 그 외 값은 `400`) |
| `page` | 페이지 번호, 1부터. 기본 `1` (1보다 작으면 1) |
| `size` | 페이지 크기. 기본 `20`, 1~100 (범위를 벗어나면 가까운 값으로 조정) |

조건을 주지 않으면 전체 예약을 페이지로 나눠 돌려줍니다. 응답 `200`: 예약 일시순, 보호자 목록과 같은 페이지 형식

```json
{
  "content": [
    {
      "id": 4,
      "hospitalId": 2,
      "hospitalName": "펫클리닉 서초점",
      "petId": 3,
      "petName": "보리",
      "vetId": 2,
      "vetName": "이서연",
      "appointmentAt": "2026-10-28T10:30:00",
      "status": "CONFIRMED",
      "reason": "관절 상태 재검진",
      "createdAt": "2026-10-15T09:00:00"
    }
  ],
  "page": 1,
  "size": 20,
  "totalElements": 10,
  "totalPages": 1
}
```

### GET `/api/appointments/{appointmentId}`

응답 `200`: 위 목록의 `content` 항목과 같은 형식. 없으면 `404`.

### POST `/api/appointments`

요청

```json
{ "hospitalId": 2, "petId": 3, "vetId": 2, "appointmentAt": "2026-10-28T10:30:00", "reason": "관절 상태 재검진" }
```

| 필드 | 규칙 |
| --- | --- |
| `hospitalId` | 필수, `/api/hospitals`의 ID |
| `petId` | 필수, 반려동물 ID |
| `vetId` | 필수, **예약 병원 소속** 수의사 (`/api/hospitals/{hospitalId}/vets`) |
| `appointmentAt` | 필수, 정각 또는 30분, **아직 끝나지 않은 시간대** (당일 예약 가능, 10:10이면 10:00 시간대까지 가능) |
| `reason` | 선택, 최대 255자 |

새 예약은 `REQUESTED` 상태로 만들어지고 `createdAt`은 서버가 채웁니다. 응답 `201`, `Location: /api/appointments/{새 ID}`, 본문은 예약 상세.

| 오류 코드 (`errors[].code`) | 경우 |
| --- | --- |
| `notFound` | 없는 병원·반려동물·수의사 ID |
| `appointment.vetNotInHospital` | 수의사가 그 병원 소속이 아님 |
| `appointment.slotPassed` | 이미 끝난 시간대 (예: 10:40에 10:00 예약) |
| `appointment.notOnSlot` | 정각·30분이 아닌 일시 (예: `10:15`) |
| `appointment.vetTimeTaken` | 수의사가 그 시각에 이미 예약됨 |
| `appointment.petTimeTaken` | 반려동물이 그 시각에 이미 예약됨 |

두 요청이 같은 시각을 **동시에** 예약하면 위 검사를 둘 다 통과할 수 있는데, 이때는 DB의 고유 인덱스가 두 번째 요청을 막고 `409 Conflict`(`"detail": "The request conflicts with existing data"`)로 응답합니다. 화면에서는 "방금 다른 예약이 먼저 잡혔습니다"처럼 안내하고 목록을 새로 불러오면 됩니다.

### PUT `/api/appointments/{appointmentId}`

요청·규칙은 등록과 같고(자기 자신의 기존 시각과는 겹쳐도 됨) 상태는 그대로 유지됩니다. 응답 `200`(예약 상세). `CANCELLED`·`COMPLETED` 예약이면 `400`(`appointment.closed`), 없으면 `404`.

### PATCH `/api/appointments/{appointmentId}/status`

요청

```json
{ "status": "CONFIRMED" }
```

응답 `200`(예약 상세). 이미 `CANCELLED`·`COMPLETED`인 예약이면 `400`(`appointment.closed`).

### GET `/api/vets/{vetId}/appointments?date=2026-10-23`

수의사 한 명의 그날(00:00~24:00) 예약 일정입니다. 예약 화면에서 이미 잡힌 시간을 보여 줄 때 씁니다.

| 파라미터 | 설명 |
| --- | --- |
| `vetId` (경로) | 수의사 ID. 없으면 `404` |
| `date` | **필수**, `yyyy-MM-dd`. 없거나 형식이 틀리면 `400` |

응답 `200`: 예약 일시순, 형식은 예약 목록과 같음. 취소(`CANCELLED`)된 예약도 상태와 함께 포함되므로, 빈 시간을 계산할 때는 취소 건을 빼고 봅니다. 예약이 없으면 빈 배열(`[]`).

| 경우 | 응답 |
| --- | --- |
| `date` 없음 (`/api/vets/1/appointments`) | `400` |
| `date` 형식 오류 (`date=2026/10/23`) | `400` |
| 없는 수의사 (`/api/vets/99/appointments?date=2026-10-23`) | `404`, `"detail": "Vet 99 not found"` |

```json
{
  "title": "Not Found",
  "status": 404,
  "detail": "Vet 99 not found",
  "instance": "/api/vets/99/appointments"
}
```

### DELETE `/api/appointments`

진료 예약을 **모두** 지웁니다. 부하 테스트 사이에 예약 데이터를 초기화할 때 씁니다. 보호자·반려동물·진료 기록은 그대로 둡니다. 되돌릴 수 없습니다.

응답 `200`: 지운 예약 수. 지울 예약이 없으면 `{"deleted": 0}`.

```json
{ "deleted": 10 }
```

### DELETE `/api/appointments/{appointmentId}`

응답 `204`, 없으면 `404`. (기록을 남기려면 삭제 대신 상태를 `CANCELLED`로 바꿉니다)

## 오류 응답

RFC 9457 Problem Details 형식(`application/problem+json`)입니다.

| 상태 코드 | 경우 |
| --- | --- |
| `400` | 입력값 검증 실패, JSON 형식 오류, 잘못된 날짜 형식 |
| `409` | 동시에 들어온 요청이 DB 제약(예: 같은 예약 시각)에 걸림 |
| `404` | 보호자·반려동물·진료 기록·병원·수의사·예약이 없음, 없는 경로 |
| `500` | 서버 내부 오류 |

검증 실패 예 (`400`)

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/owners",
  "errors": [
    { "field": "phone", "code": "Pattern", "message": "Phone number must look like 010-1234-5678" }
  ]
}
```

없는 리소스 예 (`404`)

```json
{
  "title": "Not Found",
  "status": 404,
  "detail": "Owner 99999 not found",
  "instance": "/api/owners/99999"
}
```

검증 오류의 `message`는 요청의 `Accept-Language` 헤더를 따릅니다. 예를 들어 `Accept-Language: ko`면 "전화번호는 010-1234-5678 형식이어야 합니다"처럼 한국어로 나오고, 헤더가 없으면 영어입니다.

## 개발 참고

- 운영에서는 React와 API가 같은 도메인(Apache 경유)이라 CORS 설정이 필요 없습니다.
- 로컬 개발 중 React 개발 서버에서 호출할 때는 React 개발 서버의 프록시 기능으로 `/api`를 `http://localhost:8080`에 연결하세요.
- 로컬 실행(H2)은 `db/h2`의 스키마·데이터를 쓰며, 내용은 `db/mysql`과 같습니다.
