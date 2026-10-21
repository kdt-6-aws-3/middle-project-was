# PetClinic REST API (초안)

WAS(Spring Boot)는 화면을 제공하지 않고 JSON API만 제공합니다. 기존 PetClinic의 도메인과 업무 규칙을 그대로 사용합니다.

- 기본 경로: `/api` (WEB의 Apache가 `/api` 요청만 내부 ALB를 거쳐 WAS로 전달)
- 형식: 요청·응답 모두 `application/json`, 문자 인코딩 UTF-8
- 날짜: `yyyy-MM-dd` (예: `2026-10-21`)
- 헬스 체크: `GET /actuator/health` → `{"status":"UP"}` (내부 ALB 대상 그룹용, `/api` 아님)

## 목록

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/owners?lastName={성}&page={번호}&size={개수}` | 보호자 검색 (성으로 시작, 페이지) |
| GET | `/api/owners/{ownerId}` | 보호자 상세 (반려동물·방문 기록 포함) |
| POST | `/api/owners` | 보호자 등록 |
| PUT | `/api/owners/{ownerId}` | 보호자 수정 |
| GET | `/api/pettypes` | 동물 종류 목록 |
| GET | `/api/owners/{ownerId}/pets/{petId}` | 반려동물 상세 (방문 기록 포함) |
| POST | `/api/owners/{ownerId}/pets` | 반려동물 등록 |
| PUT | `/api/owners/{ownerId}/pets/{petId}` | 반려동물 수정 |
| GET | `/api/owners/{ownerId}/pets/{petId}/visits` | 방문(진료) 기록 목록 |
| POST | `/api/owners/{ownerId}/pets/{petId}/visits` | 방문(진료) 기록 등록 |
| GET | `/api/vets` | 수의사 목록 (전문 분야 포함, 캐시) |

삭제 API는 기존 PetClinic에 없는 기능이라 만들지 않았습니다.

## 보호자

### GET `/api/owners`

| 파라미터 | 기본값 | 설명 |
| --- | --- | --- |
| `lastName` | 빈 값 (전체) | 성이 이 값으로 시작하는 보호자 |
| `page` | `1` | 1부터 시작 |
| `size` | `5` | 한 페이지 개수 (최대 100) |

`page`가 1보다 작으면 1로, `size`는 1~100 범위로 보정합니다. 마지막 페이지를 넘으면 `content`가 빈 배열입니다.

응답 `200`

```json
{
  "content": [
    {
      "id": 1,
      "firstName": "George",
      "lastName": "Franklin",
      "address": "110 W. Liberty St.",
      "city": "Madison",
      "telephone": "6085551023",
      "pets": ["Leo"]
    }
  ],
  "page": 1,
  "size": 5,
  "totalElements": 10,
  "totalPages": 2
}
```

- 결과가 없으면 `content`가 빈 배열입니다. (화면에서 "찾을 수 없음" 안내)
- 결과가 1건일 때 상세 화면으로 바로 이동하는 동작은 화면(React)에서 처리합니다.

### GET `/api/owners/{ownerId}`

응답 `200`

```json
{
  "id": 1,
  "firstName": "George",
  "lastName": "Franklin",
  "address": "110 W. Liberty St.",
  "city": "Madison",
  "telephone": "6085551023",
  "pets": [
    {
      "id": 1,
      "name": "Leo",
      "birthDate": "2010-09-07",
      "type": { "id": 1, "name": "cat" },
      "visits": [
        { "id": 1, "date": "2013-01-01", "description": "rabies shot" }
      ]
    }
  ]
}
```

없는 보호자면 `404`.

### POST `/api/owners`

요청

```json
{
  "firstName": "Jeong",
  "lastName": "Hyeon",
  "address": "Seoul",
  "city": "Seoul",
  "telephone": "0101234567"
}
```

| 필드 | 규칙 |
| --- | --- |
| `firstName`, `lastName` | 필수, 최대 30자 |
| `address` | 필수, 최대 255자 |
| `city` | 필수, 최대 80자 |
| `telephone` | 필수, 숫자 10자리 |

응답 `201`, `Location: /api/owners/{새 ID}`, 본문은 보호자 상세와 같은 형식

### PUT `/api/owners/{ownerId}`

요청·규칙은 등록과 같습니다. 응답 `200`(보호자 상세), 없는 보호자면 `404`.

## 동물 종류

### GET `/api/pettypes`

응답 `200`: 이름순

```json
[ { "id": 5, "name": "bird" }, { "id": 1, "name": "cat" } ]
```

## 반려동물

### GET `/api/owners/{ownerId}/pets/{petId}`

응답 `200`: 보호자 상세의 `pets` 항목과 같은 형식. 보호자나 반려동물이 없으면 `404`.

### POST `/api/owners/{ownerId}/pets`

요청

```json
{ "name": "Javi", "birthDate": "2020-01-01", "typeId": 2 }
```

| 필드 | 규칙 |
| --- | --- |
| `name` | 필수, 최대 30자 (같은 보호자 안에서도 이름 중복 허용) |
| `birthDate` | 필수, 오늘 이후 날짜 불가 |
| `typeId` | 필수, `/api/pettypes`의 ID |

응답 `201`, `Location: /api/owners/{ownerId}/pets/{새 ID}`, 본문은 반려동물 상세. 없는 보호자면 `404`.

### PUT `/api/owners/{ownerId}/pets/{petId}`

요청·규칙은 등록과 같습니다. 응답 `200`(반려동물 상세).

## 방문(진료) 기록

### GET `/api/owners/{ownerId}/pets/{petId}/visits`

응답 `200`: 날짜순

```json
[ { "id": 1, "date": "2013-01-01", "description": "rabies shot" } ]
```

### POST `/api/owners/{ownerId}/pets/{petId}/visits`

요청

```json
{ "date": "2026-10-22", "description": "rabies shot" }
```

| 필드 | 규칙 |
| --- | --- |
| `date` | 필수, **내일 이후** 날짜 (기존 PetClinic 규칙: 방문 예약) |
| `description` | 필수, 최대 255자 |

응답 `201`, 본문은 등록된 방문 기록.

## 수의사

### GET `/api/vets`

응답 `200`

```json
[
  {
    "id": 2,
    "firstName": "Helen",
    "lastName": "Leary",
    "specialties": [ { "id": 1, "name": "radiology" } ]
  }
]
```

## 오류 응답

RFC 9457 Problem Details 형식(`application/problem+json`)입니다.

| 상태 코드 | 경우 |
| --- | --- |
| `400` | 입력값 검증 실패, JSON 형식 오류, 잘못된 날짜 형식 |
| `404` | 보호자·반려동물이 없음, 없는 경로 |
| `500` | 서버 내부 오류 |

검증 실패 예 (`400`)

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/owners",
  "errors": [
    { "field": "telephone", "code": "Pattern", "message": "Telephone must be a 10-digit number" }
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

검증 오류의 `message`는 요청의 `Accept-Language` 헤더를 따릅니다. 예를 들어 `Accept-Language: ko`면 "전화번호는 10자리 숫자여야 합니다"처럼 한국어로 나오고, 헤더가 없으면 영어입니다.

## 개발 참고

- 운영에서는 React와 API가 같은 도메인(Apache 경유)이라 CORS 설정이 필요 없습니다.
- 로컬 개발 중 React 개발 서버에서 호출할 때는 React 개발 서버의 프록시 기능으로 `/api`를 `http://localhost:8080`에 연결하세요.
