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
실행 후 브라우저에서 `http://localhost:8080/event.html`로 접속하여 이벤트를 테스트할 수 있습니다.


## 5. SMS 발송 기능 (구현 참고사항)
본 과제에서는 실제 SMS API(coolsms, twilio 등)를 연동하지 않고 **Mock(모의) SMS 서비스**를 구현하였습니다.
실제 문자 발송은 API 비용 발생 및 계정 설정 등의 제약이 있어, **콘솔 로그를 통해 발송 내역을 확인**하는 방식으로 대체하였습니다.

이벤트 참여 시, 서버 로그(Console)에 다음과 같은 형태로 출력됩니다:
<img width="481" height="180" alt="image" src="https://github.com/user-attachments/assets/0a614ffb-f20c-4598-8553-2ed7bf7689f3" />

```
================ SMS SEND ================
To: 010-1234-5678
Content: [이벤트] 인증번호: 123456 입니다. 4월 1일 추첨 결과를 기대해주세요!
==========================================
```
실제 운영 환경 반영 시에는 `SmsService` 인터페이스의 구현체만 실제 API 연동 코드로 교체하면 됩니다.

