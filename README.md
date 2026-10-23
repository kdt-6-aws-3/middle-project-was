# PetClinic WAS (REST API)

AWS 3-Tier(WEB · WAS · DB) 인프라 구축 프로젝트의 **WAS 계층** 애플리케이션입니다.
[spring-projects/spring-petclinic](https://github.com/spring-projects/spring-petclinic)을 기반으로, 화면(Thymeleaf)을 제거하고 **JSON REST API만 제공**하도록 바꿨습니다. 보호자·반려동물·진료 기록·수의사에 대한 업무 로직은 원본 PetClinic을 그대로 사용합니다.

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

## 패키지 구조

계층별로 나눴습니다. 요청은 `controller → service → repository` 순서로 흐릅니다.

```text
org.springframework.samples.petclinic
├── controller   REST 컨트롤러 (요청·응답 변환만)
├── service      업무 로직과 검증 (PetValidator 포함)
├── repository   Spring Data JPA 리포지토리
├── entity       JPA 엔티티 (Owner, Pet, PetType, Visit, Hospital, Vet, Specialty, Appointment 등)
├── dto          요청·응답 JSON (…Request, …Details, …Summary, PageResponse)
├── exception    예외와 오류 응답 처리 (RFC 9457)
└── config       캐시 설정
```

반려동물과 진료 기록은 보호자에 딸린 데이터라 별도 리포지토리 없이 `OwnerRepository`로 보호자와 함께 저장·삭제합니다. DB의 외래 키에 `ON DELETE CASCADE`가 없으므로, 보호자나 반려동물을 지울 때는 진료 예약을 먼저 지우고, 이어서 JPA가 진료 기록 → 반려동물 → 보호자 순서로 지웁니다.

## API

전체 명세는 [docs/api.md](docs/api.md)에 있습니다.

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/owners?name=&page=&size=` | 보호자 검색 (이름으로 시작) |
| GET · POST · PUT · DELETE | `/api/owners`, `/api/owners/{ownerId}` | 보호자 상세·등록·수정·삭제 |
| GET · POST · PUT · DELETE | `/api/owners/{ownerId}/pets`, `/api/owners/{ownerId}/pets/{petId}` | 반려동물 상세·등록·수정·삭제 |
| GET · POST · PUT · DELETE | `/api/owners/{ownerId}/pets/{petId}/visits`, `…/visits/{visitId}` | 진료 기록 목록·등록·수정·삭제 |
| GET | `/api/hospitals`, `/api/hospitals/{hospitalId}`, `/api/hospitals/{hospitalId}/vets` | 병원 목록·상세·소속 수의사 |
| GET | `/api/vets` | 수의사 목록 (병원 포함) |
| GET · POST · PUT · PATCH · DELETE | `/api/appointments`, `/api/appointments/{appointmentId}`, `…/status` | 진료 예약 목록·상세·등록·변경·상태 변경·삭제 |
| GET | `/api/pettypes` | 동물 종류 목록 |
| GET | `/actuator/health` | 헬스 체크 |

오류는 RFC 9457 Problem Details 형식으로 응답하고, 검증 오류 메시지는 요청의 `Accept-Language`(예: `ko`)를 따릅니다.

## 로컬에서 실행

JDK 21이 필요합니다.

```bash
./mvnw spring-boot:run
```

<http://localhost:8080/api/owners> 또는 <http://localhost:8080/actuator/health>로 확인합니다. 기본 DB는 H2(메모리)이며, 기동할 때 `src/main/resources/db/h2/schema.sql`과 `data.sql`로 테이블과 샘플 데이터를 만듭니다. 이 두 파일은 `db/mysql`(운영 RDS 스키마·데이터)을 H2 문법으로 옮긴 것이므로, MySQL 쪽이 바뀌면 함께 고칩니다.

React 개발 서버에서 호출할 때는 React 개발 서버의 프록시 기능으로 `/api`를 `http://localhost:8080`에 연결합니다. 운영에서는 같은 도메인(Apache 경유)이라 CORS 설정이 필요 없습니다.

## 빌드와 테스트

```bash
./mvnw clean verify
```

- 결과물: `target/spring-petclinic-4.0.0-SNAPSHOT.jar` (실행 가능한 JAR)
- MySQL 통합 테스트는 Docker가 있을 때만 실행되고, 없으면 자동으로 건너뜁니다.
- **프로젝트 경로에 한글 등 영문이 아닌 문자가 있으면** checkstyle(nohttp) 단계에서 설정 파일 경로를 읽지 못해 빌드가 실패합니다. 영문 경로(예: `C:\dev\spring-petclinic`)에서 빌드하세요.

## MySQL로 실행

`mysql` 프로파일을 켜고 접속 정보는 환경 변수로 넘깁니다.

```bash
SPRING_PROFILES_ACTIVE=mysql SPRING_DATASOURCE_URL=jdbc:mysql://<DB 엔드포인트>:3306/petclinic SPRING_DATASOURCE_USERNAME=petclinic SPRING_DATASOURCE_PASSWORD=<비밀번호> java -jar target/spring-petclinic-4.0.0-SNAPSHOT.jar
```

- 테이블과 데이터는 DB 담당이 `src/main/resources/db/mysql/schema.sql`과 `data.sql`을 **한 번** 실행해 만듭니다. 두 파일은 다시 실행할 수 없어서(`CREATE TABLE`, 일반 `INSERT`) `mysql` 프로파일은 기동할 때 SQL을 실행하지 않습니다(`spring.sql.init.mode=never`).
- 빈 DB에 처음 한 번만 앱으로 넣으려면 `SPRING_SQL_INIT_MODE=always`를 잠깐 주고 기동한 뒤 다시 뺍니다.
- 앱 전용 DB 계정에는 데이터 읽기·쓰기 권한(`SELECT, INSERT, UPDATE, DELETE`)만 있으면 됩니다.

## 서버(EC2)에 배포

Amazon Linux 2023 기준입니다. 원본 EC2 한 대에서 이 레포를 받아 빌드·설치하고, AMI로 만들어 같은 WAS를 여러 대 띄웁니다. 접속은 SSM Session Manager를 사용합니다.

| 파일 | 역할 |
| --- | --- |
| `deploy/build-and-deploy.sh` | git·JDK 설치 → `git pull` → `./mvnw` 빌드 → `setup-was.sh` 실행 |
| `deploy/setup-was.sh` | Corretto 21 설치, `petclinic` 계정 생성, JAR·환경 파일·서비스 설치 후 시작 |
| `deploy/petclinic.service` | systemd 서비스 → `/etc/systemd/system/` (부팅 시 자동 시작, 비정상 종료 시 10초 뒤 재시작, 시간대 `Asia/Seoul`) |
| `deploy/petclinic.env.example` | DB 접속 정보 양식 → `/etc/petclinic/petclinic.env` (root만 읽기, 600) |

라이브러리는 따로 설치하지 않습니다. `./mvnw`가 `pom.xml`에 적힌 라이브러리를 내려받아 실행 가능한 JAR 안에 모두 넣으므로, 서버에는 빌드용 git·JDK와 실행용 Java만 있으면 됩니다.

1. EC2에 SSM Session Manager로 접속해 레포를 받습니다. (EC2에서 인터넷으로 나갈 수 있어야 합니다)

   ```bash
   sudo dnf install -y git
   git clone https://github.com/kdt-6-aws-3/middle-project-was.git
   ```

2. 빌드·설치 스크립트를 실행합니다. 첫 실행은 설치까지만 하고 멈춥니다.

   ```bash
   sudo bash middle-project-was/deploy/build-and-deploy.sh
   ```

3. 환경 파일의 `<...>` 값을 DB 담당에게 받은 정보로 채웁니다. **실제 비밀번호는 레포에 커밋하지 않습니다.** (레포는 public)

   ```bash
   sudo vi /etc/petclinic/petclinic.env
   ```

4. 2번 명령을 다시 실행합니다. 서비스를 시작하고 헬스 체크가 `UP`이 될 때까지 기다립니다.
5. `sudo reboot` 뒤에도 자동으로 뜨는지 확인한 다음 AMI를 만듭니다.

- 로그: 두 곳에 남습니다.
  - `journalctl -u petclinic -f`: 콘솔 출력(journald). 앱이 기동하다 실패한 원인도 여기서 봅니다.
  - `tail -f /var/log/petclinic/app.log`: 같은 앱 로그를 파일로도 남깁니다(`mysql` 프로파일). CloudWatch Agent가 이 파일을 읽어 보냅니다. 10MB마다 압축해 새 파일로 넘기고, 7일 지난 파일과 전체 1GB를 넘는 오래된 파일은 지웁니다.
  - 둘 다 인스턴스 디스크에 있으므로 인스턴스가 종료되면 함께 사라집니다.
- 코드를 고친 뒤에는 푸시하고 2번 명령만 다시 실행하면 됩니다. 환경 파일은 덮어쓰지 않습니다. 바꾼 뒤에는 AMI를 새로 만듭니다.
- 시간대를 `Asia/Seoul`로 두는 이유: 생일·진료 날짜 검증의 "오늘"이 서버 시간대를 따르는데, EC2 기본값(UTC)이면 한국 시간 00~09시에 하루가 어긋납니다.
- EC2 IAM 역할에 `AmazonSSMManagedInstanceCore`가 필요합니다. SSH(22) 인바운드는 열지 않습니다.
- 환경 파일은 AMI에 함께 들어가므로 AMI를 다른 계정에 공유하지 않습니다.

## 원본 프로젝트

이 프로젝트는 [spring-projects/spring-petclinic](https://github.com/spring-projects/spring-petclinic)(Apache License 2.0)에서 출발했습니다. 원본의 설계 설명은 [발표 자료](https://speakerdeck.com/michaelisvy/spring-petclinic-sample-application)를 참고하세요.

## License

The Spring PetClinic sample application is released under version 2.0 of the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
