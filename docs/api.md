# PetClinic REST API

WAS(Spring Boot)는 화면을 제공하지 않고 JSON API만 제공합니다. DB 구조는 `src/main/resources/db/mysql/schema.sql`(병원·수의사·보호자·반려동물·방문 기록)을 따릅니다.

- 기본 경로: `/api` (WEB의 Apache가 `/api` 요청만 내부 ALB를 거쳐 WAS로 전달)
- 형식: 요청·응답 모두 `application/json`, 문자 인코딩 UTF-8
- 날짜: `yyyy-MM-dd` (예: `2026-10-21`)
- 헬스 체크: `GET /actuator/health` → `{"status":"UP"}` (내부 ALB 대상 그룹용, `/api` 아님)

## 목록

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/owners?name={이름}&page={번호}&size={개수}` | 보호자 검색 (이름으로 시작, 페이지) |
| GET | `/api/owners/{ownerId}` | 보호자 상세 (반려동물·방문 기록 포함) |
| POST | `/api/owners` | 보호자 등록 |
| PUT | `/api/owners/{ownerId}` | 보호자 수정 |
| DELETE | `/api/owners/{ownerId}` | 보호자 삭제 (반려동물·방문 기록도 함께 삭제) |
| GET | `/api/owners/{ownerId}/pets/{petId}` | 반려동물 상세 (방문 기록 포함) |
| POST | `/api/owners/{ownerId}/pets` | 반려동물 등록 |
| PUT | `/api/owners/{ownerId}/pets/{petId}` | 반려동물 수정 |
| DELETE | `/api/owners/{ownerId}/pets/{petId}` | 반려동물 삭제 (방문 기록도 함께 삭제) |
| GET | `/api/owners/{ownerId}/pets/{petId}/visits` | 방문(진료) 기록 목록 |
| POST | `/api/owners/{ownerId}/pets/{petId}/visits` | 방문 기록 등록 |
| PUT | `/api/owners/{ownerId}/pets/{petId}/visits/{visitId}` | 방문 기록 수정 |
| DELETE | `/api/owners/{ownerId}/pets/{petId}/visits/{visitId}` | 방문 기록 삭제 |
| GET | `/api/pettypes` | 동물 종류 목록 |
| GET | `/api/hospitals` | 병원 목록 |
| GET | `/api/hospitals/{hospitalId}` | 병원 상세 |
| GET | `/api/hospitals/{hospitalId}/vets` | 병원 소속 수의사 목록 |
| GET | `/api/vets` | 수의사 목록 (병원·전문 분야 포함, 캐시) |

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
        { "id": 1, "date": "2026-09-01", "description": "정기 건강검진 실시. 체중과 전반적인 건강 상태 확인." }
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

보호자의 반려동물과 그 방문 기록까지 함께 삭제합니다. 응답 `204`(본문 없음), 없는 보호자면 `404`.

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

반려동물과 그 방문 기록을 함께 삭제합니다. 응답 `204`, 보호자나 반려동물이 없으면 `404`.

## 방문(진료) 기록

### GET `/api/owners/{ownerId}/pets/{petId}/visits`

응답 `200`: 날짜순

```json
[ { "id": 1, "date": "2026-09-01", "description": "정기 건강검진 실시." } ]
```

### POST `/api/owners/{ownerId}/pets/{petId}/visits`

요청

```json
{ "date": "2026-10-22", "description": "예방접종" }
```

| 필드 | 규칙 |
| --- | --- |
| `date` | 필수, **내일 이후** 날짜 (기존 PetClinic 규칙: 방문 예약) |
| `description` | 필수, 최대 255자 |

응답 `201`, 본문은 등록된 방문 기록.

### PUT `/api/owners/{ownerId}/pets/{petId}/visits/{visitId}`

요청·규칙은 등록과 같습니다(날짜는 내일 이후). 응답 `200`(방문 기록), 없으면 `404`.

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

## 오류 응답

RFC 9457 Problem Details 형식(`application/problem+json`)입니다.

| 상태 코드 | 경우 |
| --- | --- |
| `400` | 입력값 검증 실패, JSON 형식 오류, 잘못된 날짜 형식 |
| `404` | 보호자·반려동물·방문 기록·병원이 없음, 없는 경로 |
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
