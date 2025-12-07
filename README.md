# 🎙️ 프로젝트 소개 (Project Overview)
개인적인 고민과 개발을 주제로 서로 소통하는 커뮤니티 서비스의 **실시간 채팅 전용 Back-end 서버**입니다.  
기존 Spring MVC + STOMP 서버에서 처리하던 채팅 기능을, **외부 I/O가 많은 특성(LLM API 호출, Redis, DB 조회 등)** 때문에  
**Spring WebFlux + 순수 WebSocket 기반의 별도 서버로 분리**하여 구현한 프로젝트입니다.

---

## ⚙️ Back-end 소개

- **아키텍처**
  - Spring Boot + Spring WebFlux 기반 비동기 / 논블로킹 채팅 서버
  - 순수 WebSocket 핸드셰이크 및 연결 관리 (STOMP 미사용)
  - 메인 커뮤니티 서버(MVC + STOMP)와 분리된 마이크로서비스 구조
  - Redis Pub/Sub을 이용한 멀티 인스턴스 간 메시지 브로드캐스트

- **주요 도메인 및 기능**
  - **채팅방**
    - 채팅방 생성 / 조회
    - 채팅방 입장 / 퇴장
  - **메시지**
    - WebSocket을 통한 텍스트 메시지 송수신
    - 특정 채팅방 단위로 메시지 브로드캐스팅
    - 최근 대화 내용 조회(LLM 프롬프트용)
  - **LLM 어시스턴트**
    - 채팅방 내 최근 대화 로그 기반 프롬프트 구성
    - LLM(Gemini) API 호출로 AI 답변 생성
    - 생성된 답변을 일반 채팅 메시지와 동일하게 브로드캐스트
  - **인증**
    - 메인 서버에서 발급한 JWT를 검증하여 사용자 식별
    - WebSocket 접속 시 토큰 검증(쿼리 파라미터 / 헤더 기반)

- **기술적 특징**
  - WebFlux + Reactor 기반 논블로킹 I/O 처리
  - WebClient를 활용한 LLM 외부 API 호출의 완전 비동기 처리
  - Redis Pub/Sub과 세션 레지스트리(SessionRegistry)를 활용한 다중 노드 확장 대응
  - LLM 호출·DB 조회 등 블로킹 가능성이 있는 작업을 별도 Scheduler로 분리하여 Netty 이벤트 루프 보호

---

## 👨‍👩‍👧‍👦 개발 인원 및 기간
- 개발 기간: 2025.09 ~ 2025.12 
- 개발 인원: Back-end 1인 개발 (본인)
- 역할:
  - 기존 MVC + STOMP 기반 채팅 기능 분리 및 WebFlux 전환 설계
  - 순수 WebSocket 기반 채팅 프로토콜 설계 및 구현
  - LLM 연동(대화 로그 수집, 프롬프트 구성, 응답 전송) 전 과정 구현
  - Redis Pub/Sub, JWT 인증 연동, 예외 처리 등 인프라 및 공통 모듈 구현

---

## 🛠 사용 기술 및 Tools

### Back-end
- Java
- Spring Boot (Spring WebFlux, Reactive WebSocket)
- Reactor Netty
- Spring Security (JWT 인증 연동)
- Spring Data JPA / MySQL 
- Redis (Pub/Sub 기반 메시지 브로드캐스트)
- WebClient (LLM API 호출)
- Google Gemini / LLM API
- Gradle

### Infra & 기타
- AWS EC2 / RDS / S3 / CloudFront / Route 53 
- Docker / Docker Compose
- Git, GitHub
- GitHub Actions(CI/CD)
<img width="708" height="872" alt="최종 아키텍처" src="https://github.com/user-attachments/assets/919f8784-db82-4048-bc01-76e7bbd17463" />

---

## 📂 GitHub Repository
- Chat 서버 GitHub (Spring WebFlux + WebSocket + LLM): https://github.com/sh123456-boop/chat_flux

---

## 🎥 시연 영상
- YouTube: https://youtu.be/3V6KJWw1kFE  
  > 커뮤니티 서비스 내 **실시간 채팅 및 LLM 자동 응답 기능**을 담당하는 서버입니다.
