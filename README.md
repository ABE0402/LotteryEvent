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


## 5. SMS 발송 기능 (구현 참고사항)
본 과제에서는 실제 SMS API(coolsms, twilio 등)를 연동하지 않고 **Mock(모의) SMS 서비스**를 구현하였습니다.
실제 문자 발송은 API 비용 발생 및 계정 설정 등의 제약이 있어, **콘솔 로그를 통해 발송 내역을 확인**하는 방식으로 대체하였습니다.

이벤트 참여 시, 서버 로그(Console)에 다음과 같은 형태로 출력됩니다:
```
================ SMS SEND ================
To: 010-1234-5678
Content: [이벤트] 인증번호: 123456 입니다. 4월 1일 추첨 결과를 기대해주세요!
==========================================
```

## 6. 당첨자 선정 로직 및 필터링 전략 (구현 설명)

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

- **선택 이유 (Why AND Filtering?)**:
  1.  **명확한 로직 표현**: "A 조건 `그리고` B 조건"이라는 요구사항이 코드에 직관적으로 드러남.
  2.  **유연한 확장성**: 추후 "서울 거주자(`AND Region == Seoul`)" 같은 조건이 추가되어도 `filter` 한 줄만 추가하면 됨.
  3.  **요구사항 정밀 반영**: 과제에서 요구하는 특정 범위(Range) 내 당첨자 선정을 정확하게 구현 가능.

### 2. 한계점 및 개선 방향 (대규모 트래픽 고려 시)
현재 로직은 **1,000~10,000명 규모의 소규모 이벤트**에 최적화되어 있습니다.
만약 수백만 명 단위의 대규모 트래픽 환경이나 동시 접속이 많은 실제 서비스에 적용할 경우, 다음과 같은 문제점과 개선 방안을 고려해야 합니다.

#### A. 메모리 부족 (OOM: Out Of Memory) 위험
- **문제점**: `eventParticipantRepository.findByEventId`로 모든 참여자 정보를 `List` 객체로 한 번에 메모리에 로딩합니다. 수십만 명 이상의 데이터가 힙(Heap) 메모리에 올라갈 경우, 서버가 감당하지 못할 수 있습니다.
- **개선 방안**:
    - **Spring Batch**를 도입하여 대용량 데이터를 청크(Chunk) 단위로 나누어 처리
    - 애플리케이션 메모리 로딩 대신 **DB 쿼리 레벨(ORDER BY RAND() LIMIT N)**에서 랜덤 추출

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

