# PetClinic WAS (REST API)

AWS 3-Tier(WEB · WAS · DB) 인프라 구축 프로젝트의 **WAS 계층** 애플리케이션입니다.
[spring-projects/spring-petclinic](https://github.com/spring-projects/spring-petclinic)을 기반으로, 화면(Thymeleaf)을 제거하고 **JSON REST API만 제공**하도록 바꿨습니다. 보호자·반려동물·방문 기록·수의사에 대한 업무 로직은 원본 PetClinic을 그대로 사용합니다.

## 구성에서의 역할

```text
브라우저 ──▶ 외부 ALB ──▶ WEB (Apache 2.4 + React 빌드 결과)
                               │  /api/* 만 전달 (mod_proxy_http)
                               ▼
                           내부 ALB ──▶ WAS (이 애플리케이션, 포트 8080) ──▶ DB (RDS for MySQL)
```

- 화면은 WEB(React)이 담당하고, 이 애플리케이션은 `/api/**` 요청만 처리합니다.
- 내부 ALB 헬스 체크 경로는 `/actuator/health`입니다.

### WEB-WAS 연동 설정

연결을 재사용하는 쪽(앞단)이 먼저 끊도록 유휴 시간을 계단식으로 맞춥니다. 어긋나면 이미 닫힌 연결로 요청을 보내 간헐적으로 502가 납니다.

| 구간 | 설정 | 값 |
| --- | --- | --- |
| Apache → 내부 ALB | `ProxyPass /api http://<내부 ALB DNS>/api ttl=50` + `ProxyPassReverse` | 50초 |
| 내부 ALB | 유휴 타임아웃 (기본값) | 60초 |
| 내부 ALB → WAS | `server.tomcat.keep-alive-timeout` (`application.properties`) | 65초 |

| 항목 | 값 |
| --- | --- |
| 대상 그룹 | HTTP 8080, 헬스 체크 `/actuator/health` (성공 코드 200) |
| 등록 취소 지연 | 30~60초 권장 |
| WAS 종료 | `server.shutdown=graceful`, 처리 중인 요청을 최대 20초 기다린 뒤 종료 (등록 취소 지연보다 짧게) |

## 기술 스택

| 항목 | 버전 |
| --- | --- |
| Java | 21 (Amazon Corretto 21) |
| Spring Boot | 4.1.1 |
| 웹 서버 | 내장 Tomcat 11 (실행 가능한 JAR) |
| 영속성 | Spring Data JPA (Hibernate) |
| DB | H2(기본, 개발용) / MySQL(`mysql` 프로파일) |
| 빌드 | Maven Wrapper(`mvnw`), Gradle Wrapper(`gradlew`) |

## API

전체 명세는 [docs/api.md](docs/api.md)에 있습니다.

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/owners?lastName=&page=&size=` | 보호자 검색 |
| GET · POST · PUT | `/api/owners`, `/api/owners/{ownerId}` | 보호자 상세·등록·수정 |
| GET | `/api/pettypes` | 동물 종류 목록 |
| GET · POST · PUT | `/api/owners/{ownerId}/pets`, `/api/owners/{ownerId}/pets/{petId}` | 반려동물 상세·등록·수정 |
| GET · POST | `/api/owners/{ownerId}/pets/{petId}/visits` | 방문 기록 목록·등록 |
| GET | `/api/vets` | 수의사 목록 |
| GET | `/actuator/health` | 헬스 체크 |

오류는 RFC 9457 Problem Details 형식으로 응답하고, 검증 오류 메시지는 요청의 `Accept-Language`(예: `ko`)를 따릅니다.

## 로컬에서 실행

JDK 21이 필요합니다.

```bash
./mvnw spring-boot:run
```

<http://localhost:8080/api/owners> 또는 <http://localhost:8080/actuator/health>로 확인합니다. 기본 DB는 H2(메모리)이며, 기동할 때 `src/main/resources/db/h2/schema.sql`과 `data.sql`로 테이블과 샘플 데이터를 만듭니다.

React 개발 서버에서 호출할 때는 React 개발 서버의 프록시 기능으로 `/api`를 `http://localhost:8080`에 연결합니다. 운영에서는 같은 도메인(Apache 경유)이라 CORS 설정이 필요 없습니다.

## 빌드와 테스트

```bash
./mvnw clean verify
```

- 결과물: `target/spring-petclinic-4.0.0-SNAPSHOT.jar` (실행 가능한 JAR)
- MySQL·PostgreSQL 통합 테스트는 Docker가 있을 때만 실행되고, 없으면 자동으로 건너뜁니다.
- **프로젝트 경로에 한글 등 영문이 아닌 문자가 있으면** checkstyle(nohttp) 단계에서 설정 파일 경로를 읽지 못해 빌드가 실패합니다. 영문 경로(예: `C:\dev\spring-petclinic`)에서 빌드하세요.

## MySQL로 실행

`mysql` 프로파일을 켜고 접속 정보는 환경 변수로 넘깁니다.

```bash
SPRING_PROFILES_ACTIVE=mysql SPRING_DATASOURCE_URL=jdbc:mysql://<DB 엔드포인트>:3306/petclinic SPRING_DATASOURCE_USERNAME=petclinic SPRING_DATASOURCE_PASSWORD=<비밀번호> java -jar target/spring-petclinic-4.0.0-SNAPSHOT.jar
```

- 기동할 때 `src/main/resources/db/mysql/schema.sql`과 `data.sql`이 실행됩니다. 모두 `IF NOT EXISTS`, `INSERT IGNORE`로 작성되어 있어 다시 기동해도 기존 데이터를 덮어쓰지 않습니다.
- 앱 전용 DB 계정에는 테이블 생성(`CREATE`)과 데이터 읽기·쓰기 권한이 필요합니다.

## 서버(EC2)에서 실행: systemd 서비스

접속 정보는 root만 읽을 수 있는 환경 파일에 둡니다.

```bash
# /etc/petclinic/petclinic.env  (chmod 600)
SPRING_PROFILES_ACTIVE=mysql
SPRING_DATASOURCE_URL=jdbc:mysql://<DB 엔드포인트>:3306/petclinic
SPRING_DATASOURCE_USERNAME=petclinic
SPRING_DATASOURCE_PASSWORD=<비밀번호>
```

```ini
# /etc/systemd/system/petclinic.service
[Unit]
Description=PetClinic WAS (Spring Boot)
After=network-online.target
Wants=network-online.target

[Service]
User=petclinic
EnvironmentFile=/etc/petclinic/petclinic.env
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/petclinic/petclinic.jar
SuccessExitStatus=143
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload && sudo systemctl enable --now petclinic
```

## 원본 프로젝트

이 프로젝트는 [spring-projects/spring-petclinic](https://github.com/spring-projects/spring-petclinic)(Apache License 2.0)에서 출발했습니다. 원본의 설계 설명은 [발표 자료](https://speakerdeck.com/michaelisvy/spring-petclinic-sample-application)를 참고하세요.

## License

The Spring PetClinic sample application is released under version 2.0 of the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
