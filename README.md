# 프로젝트 실행 가이드

## 1. 데이터베이스 설정 (MySQL)
1. MySQL Workbench 또는 터미널을 엽니다.
2. `src/main/resources/schema.sql` 파일의 내용을 실행하여 테이블을 생성합니다 (데이터베이스 이름은 `lottery`로 가정).
   ```sql
   CREATE DATABASE lottery;
   USE lottery;
   -- schema.sql 내용 복사 붙여넣기 실행
   ```

## 2. 프로젝트 가져오기 (IntelliJ IDEA)
1. IntelliJ를 실행하고 **Open**을 클릭합니다.
2. `Desktop/기업 과제/LotteryEvent`를 선택합니다.
3. `pom.xml`을 자동으로 인식하여 라이브러리를 다운로드받을 때까지 기다립니다.

## 3. 설정 (application.properties)
`src/main/resources/application.properties` 파일을 열어 MySQL 접속 정보를 본인 환경에 맞게 수정하세요.
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/lottery?serverTimezone=UTC&characterEncoding=UTF-8
spring.datasource.username=root  <-- 아이디 확인
spring.datasource.password=1234  <-- 비밀번호 확인
```

## 4. 실행
`LotteryEventApplication.java` 파일을 열고 녹색 `Run` 버튼을 클릭하세요.
실행 후 브라우저에서 `http://localhost:8080/index.html`로 접속하면 이벤트 추첨 팝업 확인 가능.

## 5. 데이터베이스 테이블 정의 및 주요 컬럼 설계
이 프로젝트의 데이터베이스는 이벤트 관리, 참여자 추적, 당첨자 관리를 위한 3개의 주요 테이블로 구성되어 있습니다.

### 5.1. 이벤트 마스터 테이블 (`TB_EVENT_MASTER`)
이벤트의 기본 정보와 기간을 관리하는 테이블입니다.

| 컬럼명 | 타입 | 설명 | 비고 |
| :--- | :--- | :--- | :--- |
| `event_id` | Integer | 이벤트 고식별자 | PK, 자동 증가 |
| `event_name` | String | 이벤트 명칭 | 필수 |
| `start_dt` | DateTime | 이벤트 시작 일시 | 필수 |
| `end_dt` | DateTime | 이벤트 종료 일시 | 필수 |
| `announce_dt` | DateTime | 당첨자 발표 일시 | 필수 |
| `is_active` | Boolean | 이벤트 활성화 여부 | |
| `created_at` | DateTime | 데이터 생성 일시 | |

### 5.2. 이벤트 참여자 테이블 (`TB_EVENT_PARTICIPANT`)
이벤트에 응모한 사용자의 정보를 저장하는 테이블입니다.

| 컬럼명 | 타입 | 설명 | 비고 |
| :--- | :--- | :--- | :--- |
| `participant_id` | Integer | 참여자 고유 식별자 | PK, 자동 증가 |
| `event_id` | Integer | 관련 이벤트 ID | FK (`TB_EVENT_MASTER`) |
| `phone` | String | 참여자 휴대폰 번호 | 필수 |
| `name` | String | 참여자 이름 | |
| `entry_no` | Integer | 참여 순번/응모 번호 | 필수 |
| `ip_address` | String | 참여 당시 IP 주소 | |
| `reg_dt` | DateTime | 참여 등록 일시 | |
| `lottery_number` | String | 할당된 추첨 번호 | |

### 5.3. 이벤트 당첨자 테이블 (`TB_EVENT_WINNER`)
이벤트 추첨을 통해 선정된 당첨자 정보를 관리하는 테이블입니다.

| 컬럼명 | 타입 | 설명 | 비고 |
| :--- | :--- | :--- | :--- |
| `winner_id` | Integer | 당첨 고유 식별자 | PK, 자동 증가 |
| `event_id` | Integer | 관련 이벤트 ID | FK (`TB_EVENT_MASTER`) |
| `participant_id` | Integer | 참여자 식별 ID | FK (`TB_EVENT_PARTICIPANT`) |
| `winning_rank` | Integer | 당첨 등수 | 필수 |
| `prize_name` | String | 경품 명칭 | 필수 |
| `win_dt` | DateTime | 당첨 처리 일시 | |
| `check_count` | Integer | 당첨 결과 확인 횟수 | 기본값: 0 |
| `last_check_dt` | DateTime | 최종 결과 확인 일시 | |

#### 핵심 설계 관계도
- **1개**의 이벤트(`TB_EVENT_MASTER`)는 **여러 명**의 참여자(`TB_EVENT_PARTICIPANT`)를 가질 수 있습니다.
- **1개**의 이벤트(`TB_EVENT_MASTER`)는 **여러 명**의 당첨자(`TB_EVENT_WINNER`)를 배출합니다.
- **당첨자**(`TB_EVENT_WINNER`)는 반드시 **참여자**(`TB_EVENT_PARTICIPANT`) 테이블의 데이터를 참조해야 합니다. (당첨자는 참여자 중에서만 발생)

### 5.4. 인덱스(Index) 전략
데이터 조회 성능 향상을 위해 다음과 같은 인덱스를 설계하였습니다.

```sql
-- TB_EVENT_MASTER: 진행 중인 이벤트를 빠르게 찾기 위함
CREATE INDEX idx_master_active ON TB_EVENT_MASTER(is_active);

-- TB_EVENT_PARTICIPANT: 전체 참여 이력 조회 및 중복 체크 성능 향상
CREATE INDEX idx_part_phone ON TB_EVENT_PARTICIPANT(phone);

-- TB_EVENT_WINNER: 당첨자 조회 및 조인(Join) 성능 최적화
CREATE INDEX idx_winner_event ON TB_EVENT_WINNER(event_id);
CREATE INDEX idx_winner_part ON TB_EVENT_WINNER(participant_id);
```

#### 설무 상세
- **TB_EVENT_MASTER (is_active)**: `findByIsActiveTrue` 등의 쿼리에서 활성 이벤트를 즉시 조회하기 위해 사용합니다.
- **TB_EVENT_PARTICIPANT (phone)**: `(event_id, phone)` 복합 유니크 키가 있어 특정 이벤트 내 조회는 빠르지만, 전체 참여 이력 조회를 위해 단독 인덱스를 추가하였습니다.
- **TB_EVENT_WINNER (event_id, participant_id)**: 외래키(FK) 컬럼에 인덱스를 추가하여 조인 성능을 최적화하고 데이터 무결성 체크 속도를 높였습니다.

## 6. SMS 발송 기능 (구현 참고사항)
본 과제에서는 실제 SMS API(coolsms, twilio 등)를 연동하지 않고 **Mock(모의) SMS 서비스**를 구현하였습니다.
실제 문자 발송은 API 비용 발생 및 계정 설정 등의 제약이 있어, **콘솔 로그를 통해 발송 내역을 확인**하는 방식으로 대체하였습니다.

이벤트 참여 시, 서버 로그(Console)에 다음과 같은 형태로 출력됩니다:

```
================ SMS SEND ================
To: 010-1234-5678
Content: [이벤트] 인증번호: 123456 입니다. 4월 1일 추첨 결과를 기대해주세요!
==========================================
```

## 7. 당첨자 선정 로직 및 필터링 전략 (구현 설명)

### 1. 당첨 번호 조건 및 필터링 방식 비교
본 프로젝트에서는 당첨자 선정 시 **AND 연산자(`&&`)를 활용한 범위 필터링 방식**을 채택하였습니다.
기존의 단순 무작위 추출 방식과 비교하여 선택한 이유는 다음과 같습니다.

#### A. 기존 방식: 단순 무작위 추출 (Simple Random Picking)
- **설명**: 전체 참여자 중 N명을 단순히 랜덤하게 뽑는 방식
- **장점**: 구현이 매우 간단하고 빠름 (`Collections.shuffle` 후 상위 N명 선택)
- **단점**: "2등은 2000~7000번 사이 참여자 중에서만, 3등은 1000~8000번 사이 참여자 중에서만 선정"과 같은 **복잡한 비즈니스 요구사항을 반영하기 어려움**.
- **한계**: 특정 조건(지역, 가입일, 참여번호 범위 등)을 만족하는 당첨자를 선별하려면, 무작위 추출 후 다시 조건을 검사하는 비효율적인 과정을 거쳐야 함.

#### B. 선택한 방식: AND 필터링 
- **설명**: 여러 조건을 **AND(`&&`) 연산**으로 연결해 후보군을 정밀하게 필터링한 후 랜덤 선정.
- **구현 예시**:
  ```java
  // 2등 조건: (아직 당첨 안 됨) AND (참여번호 2000 이상) AND (참여번호 7000 이하)
  .filter(p -> !winningPhones.contains(p.getPhone())) 
  .filter(p -> p.getEntryNo() >= 2000 && p.getEntryNo() <= 7000)
  ```

- **선택 이유**:
  1.  **명확한 로직 표현**: "A 조건 `그리고` B 조건"이라는 요구사항이 코드에 직관적으로 드러남.
  2.  **유연한 확장성**: 추후 "서울 거주자(`AND Region == Seoul`)" 같은 조건이 추가되어도 `filter` 한 줄만 추가하면 됨.
  3.  **요구사항 정밀 반영**: 과제에서 요구하는 특정 범위(Range) 내 당첨자 선정을 정확하게 구현 가능.


### 2. 한계점 및 개선 방향 (대규모 트래픽 고려 시)
현재 로직은 **10,000명 내외의 과제 규모**에서는 **매우 빠르고 안전하게 동작**합니다 (메모리 사용량 약 2~3MB, 처리시간 0.01초 이내).
단, 수백만 명 단위의 대규모 트래픽 환경이나 동시 접속이 많은 실제 서비스 확장 시에는 다음과 같은 개선 방안을 고려해야 합니다.

#### A. 메모리 부족 (OOM: Out Of Memory) 위험
- **문제점**: `eventParticipantRepository.findByEventId`로 모든 참여자 정보를 `List` 객체로 한 번에 메모리에 로딩합니다. 수십만 명 이상의 데이터가 힙(Heap) 메모리에 올라갈 경우, 서버가 감당하지 못할 수 있습니다.
- **개선 방안**:
    - **Spring Batch**를 도입하여 대용량 데이터를 청크(Chunk) 단위로 나누어 처리
    - 애플리케이션 메모리 로딩 대신 DB 쿼리 레벨(ORDER BY RAND() LIMIT N)에서 랜덤 추출

#### B. 동시성 문제 (Concurrency & Race Condition)
- **문제점**: 관리자가 실수로 추첨 버튼을 동시에 누르거나, 분산 서버 환경에서 동시에 실행될 경우, 같은 참여자가 중복 당첨되거나 추첨 결과가 덮어씌워질 위험이 있습니다. (`@Transactional`만으로는 완벽한 동시성 제어가 어려울 수 있음)
- **개선 방안**:
    - **비관적 락(Pessimistic Lock)** 또는 **낙관적 락(Optimistic Lock)** 적용
    - Redis와 같은 분산 락(Distributed Lock) 활용
    - 추첨 상태 플래그(`isDrawing`)를 두어 중복 실행 방지

#### C. 성능 효율성 (Performance Overhead)
- **문제점**: 필요한 당첨자 수(약 1,000명)에 비해, 모든 데이터를 가져와서 필터링하는 방식은 불필요한 DB I/O와 네트워크 트래픽을 유발합니다.
- **개선 방안**:
    - "2000~7000번 사이" 조건 자체를 **QueryDSL이나 JPQL의 WHERE 절**에 포함시켜 대상자만 가져오도록 최적화

---

## 8. 결과 발표 기능 검증 가이드

구현된 기능들이 정상적으로 동작하는지 확인하기 위한 수동 검증 단계입니다.

### 1. 자동 오픈 및 결과 페이지 전환 (프론트엔드)
`index.html`은 날짜에 따라 '참여하기'와 '결과 확인하기' 모드를 자동으로 전환하며, 매일 최초 접속 시 팝업을 띄웁니다.

#### 테스트 준비
1. 브라우저에서 `index.html`을 엽니다.
2. 개발자 도구(F12) -> Application 탭 -> Local Storage에서 저장된 키들을 삭제합니다 (`lottery_popup_closed_date`, `lastResultCheckPopupDate` 등).

#### 시나리오 A: 이벤트 기간 (현재 ~ 2026.03.31)
- **날짜 설정**: 코드의 `ANNOUNCEMENT_DATE`는 `2026-04-01`입니다. 현재 날짜가 이보다 전이라면 '참여하기' 화면이 보여야 합니다.
- **테스트 팁**: `index.html`의 250번째 줄 근처 `ANNOUNCEMENT_DATE`를 미래 날짜로 설정되어 있는지 확인하세요.
- **동작 확인**:
    - 페이지 접속 시 참여하기 팝업(`event.html`)이 자동으로 떠야 합니다.
    - "오늘 하루 보지 않기"를 체크하고 닫은 뒤, 새로고침하면 팝업이 뜨지 않아야 합니다.

#### 시나리오 B: 결과 발표 기간 (2026.04.01 이후)
- **날짜 설정 (임시 수정)**: 테스트를 위해 `index.html`의 `ANNOUNCEMENT_DATE`를 오늘 날짜 또는 과거로 잠시 변경합니다 (예: `2024-01-01`).
- **동작 확인**:
    - 페이지 새로고침 시 "오늘의 당첨 결과를 확인하시겠습니까?" 라는 시스템 알림창(confirm)이 떠야 합니다.
    - [확인] 클릭 시: 결과 확인 팝업(`result.html`)이 떠야 합니다.
    - 다시 새로고침 시: 알림창이 뜨지 않아야 합니다 (하루 1회 제한).

### 2. 당첨 결과 상세/간략 표시 (백엔드)
`LotteryService`는 당첨 확인 횟수에 따라 일부 정보를 숨깁니다.

#### 테스트 데이터 준비
1. DB에 임의의 당첨자 데이터를 생성합니다 (또는 `EventWinner` 테이블 확인).
2. `check_count`를 0으로 설정합니다.

#### 시나리오
- **첫 번째 조회**:
    - `result.html` (또는 통합 팝업)에서 당첨된 전화번호로 조회합니다.
    - **결과**: "축하합니다! 1등에 당첨되셨습니다!" (등수와 상품이 구체적으로 표시됨)
- **두 번째 조회**:
    - 같은 번호로 다시 조회합니다.
    - **결과**: "당첨되셨습니다! (상세 내역은 최초 확인 시에만 제공됩니다)" (등수 숨김 처리됨)

### 3. 미확인 당첨자 알림 문자 (백엔드)
`NotificationService`는 발표 후 10일이 지난 미확인 당첨자에게 문자를 보냅니다.

#### 시나리오
- **DB 상태**: `TB_EVENT_MASTER`의 `end_dt`가 10일 이상 지난 이벤트가 있어야 합니다.
- **당첨자 상태**: `check_count`가 0인 당첨자가 있어야 합니다.
- **실행**:
    - 애플리케이션을 실행하면 스케줄러가 매일 오전 10시에 돕니다.
    - **즉시 테스트**: `NotificationService.sendLateWinnerNotification()` 메소드에 `@PostConstruct`를 임시로 붙이거나, 테스트 코드로 실행합니다.
- **확인**:
    - 콘솔 로그에 `Sent late notification to: 010-xxxx-xxxx`가 찍히는지 확인합니다.

