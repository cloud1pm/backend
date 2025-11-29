# ☁️ Cloud1PM

### AI 기반 자기 정서 돌봄 서비스

> **숙명여자대학교 2025학년도 2학기 클라우드시스템(001) 기말 프로젝트 — 5조**

**Cloud1PM**은 우울·불안·스트레스 등 현대인의 정서적 어려움을 완화하기 위해 개발된 **AI 기반 정서 지지 서비스**입니다.

Google Gemini API를 활용하여 **실시간 감정 분석, 위험도 탐지, 맞춤형 솔루션 제시, 캐릭터 성장 시스템**을 제공하여 지속적인 마음 관리 경험을 돕습니다.

---

## 📑 목차

1. [프로젝트 개요](#-프로젝트-개요)
2. [팀원 및 역할](#-팀원-및-역할-contributors)
3. [기술 스택](#%EF%B8%8F-기술-스택-tech-stack)
4. [시스템 아키텍처](#-시스템-아키텍처-architecture)
5. [주요 기능](#-주요-기능-key-features)
6. [프로젝트 구조](#-프로젝트-구조-project-structure)
7. [시작하기](#-시작하기-getting-started)
8. [License](#-license)

---

## 📅 프로젝트 개요

- **주제:** AI 챗봇 기반 실시간 감정 분석 및 정서 치유 솔루션
- **목표:**
  - **사용자:** AI 대화를 통한 정서적 지지 및 위기 상황 사전 예방
  - **기술:** MSA·Kubernetes 기반 확장 가능한 클라우드 인프라 구축

---

## 👥 팀원 및 역할 (Contributors)

| 이름       |                                     사진                                      | GitHub                                     | 역할 및 주요 기여                                                                                                                                                                |
| ---------- | :---------------------------------------------------------------------------: | ------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **강민지** | <img src="https://avatars.githubusercontent.com/u/123651373?v=4" width="80"/> | [@mingd0d](https://github.com/mingd0d)     | **Backend & DevOps Lead**<br>- Kubernetes 클러스터 구성, CI/CD 구축<br>- HPA, Rolling Update 적용<br>- Spring Security & JWT 인증/인가 개발<br>- 전체 아키텍처 설계 및 DB 모델링 |
| **경세빈** | <img src="https://avatars.githubusercontent.com/u/164703063?v=4" width="80"/> | [@junii2002](https://github.com/junii2002) | **Frontend Core & Data Visualization**<br>- 감정 리포트(Recharts) 시각화<br>- 캐릭터 성장·애니메이션 시스템 개발<br>- 온보딩·위기 솔루션 로직 구현                               |
| **박소요** | <img src="https://avatars.githubusercontent.com/u/164558822?v=4" width="80"/> | [@oyossss](https://github.com/oyossss)     | **Frontend Lead & Backend Integration**<br>- 전체 UI/UX 구조 설계<br>- 게시판·댓글·좋아요·채팅 기능 개발<br>- 프론트–백엔드 API 연동 및 예외 처리                                |
|            |

---

## 🛠️ 기술 스택 (Tech Stack)

### Frontend

<img src="https://img.shields.io/badge/React-61DAFB?style=flat-square&logo=React&logoColor=black"/> <img src="https://img.shields.io/badge/Vite-646CFF?style=flat-square&logo=Vite&logoColor=white"/> <img src="https://img.shields.io/badge/Nginx-009639?style=flat-square&logo=Nginx&logoColor=white"/> <img src="https://img.shields.io/badge/Recharts-22b5bf?style=flat-square&logo=Recharts&logoColor=white"/>

### Backend

<img src="https://img.shields.io/badge/Spring Boot-6DB33F?style=flat-square&logo=SpringBoot&logoColor=white"/> <img src="https://img.shields.io/badge/Spring Security-6DB33F?style=flat-square&logo=SpringSecurity&logoColor=white"/> <img src="https://img.shields.io/badge/Java 17-007396?style=flat-square&logo=OpenJDK&logoColor=white"/> <img src="https://img.shields.io/badge/JPA-59666C?style=flat-square&logo=Hibernate&logoColor=white"/>

### AI & Data

<img src="https://img.shields.io/badge/Google Gemini-8E75B2?style=flat-square&logo=Google&logoColor=white"/> <img src="https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=MySQL&logoColor=white"/>

### DevOps & Infra

<img src="https://img.shields.io/badge/Kubernetes-326CE5?style=flat-square&logo=Kubernetes&logoColor=white"/> <img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=Docker&logoColor=white"/> <img src="https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=Gradle&logoColor=white"/>

---

## 🧩 시스템 아키텍처 (Architecture)

Cloud1PM은 **MSA 기반**으로 서비스들을 모듈화하고 **Kubernetes** 위에서 동작하도록 설계되었습니다.

- **Frontend:** Nginx 기반 React 앱
- **Backend:** 비즈니스 로직·DB 통신·인증/인가
- **AI-Analysis Service:** Gemini API 기반 감정 분석·위험도 계산
- **Batch Job:** 주간 감정 리포트(CronJob)
- **MySQL DB:** StatefulSet + PVC로 데이터 영속성 보장

---

## ✨ 주요 기능 (Key Features)

### 1. 🤖 AI 감정 분석 & 위기 대응 챗봇

- 실시간 감정 분석(긍정/부정), 위험도(1~10) 측정
- 고위험군 탐지 시 사용자가 등록한 해결 방안 자동 제시

### 2. 📊 감정 대시보드

- 최근 7일/1개월 감정 흐름 그래프 제공
- 감정 상태를 객관적으로 파악 가능

### 3. ☃️ 캐릭터 성장 시스템

- 서비스 참여로 ‘밥(Rice)’ 획득
- 캐릭터 단계 성장: 물방울 → 얼음 → 아기 눈송이 → 눈송이

### 4. 🏘️ 익명 커뮤니티 + 응원 기능

- 고민 공유 커뮤니티
- ‘오늘의 응원’ 기능으로 자기 위로 기록

### 5. 🚀 클라우드 네이티브 기능

- **HPA** 기반 Auto Scaling
- **Rolling Update**로 무중단 배포
- **CronJob**으로 매주 자동 리포트 발행

---

## 📂 프로젝트 구조 (Project Structure)

```bash
cloud1pm/
├── backend/
│   └── backend-dev/
│       ├── src/main/java/com/cloud1pm/backend/
│       │   ├── controller/
│       │   ├── service/
│       │   ├── entity/
│       │   ├── repository/
│       │   └── runner/
│       ├── Dockerfile
│       └── k8s-*.yaml
│
├── frontend/
│   └── frontend-dev/
│       ├── src/
│       │   ├── components/
│       │   ├── pages/
│       │   ├── api/
│       │   └── context/
│       ├── Dockerfile
│       └── nginx.conf
│
└── README.md
```

---

## 🚀 시작하기 (Getting Started)

### Prerequisites

- **Docker & Kubernetes** (Minikube 또는 Docker Desktop 권장)
- **Java 17+** (Backend)
- **Node.js 18+** (Frontend)
- **Google Gemini API Key** (필수)

### 1. Repository Clone

```bash
git clone https://github.com/cloud1pm/frontend.git
git clone https://github.com/cloud1pm/backend.git
```

### 2. Backend Setup

```bash
cd cloud1pm/backend/backend-dev

# ⚠️ src/main/resources/application.properties 파일에서
# GEMINI_API_KEY 및 DB 설정 확인이 필요합니다.

# Build
./gradlew build
```

### 3. Frontend Setup

```bash
cd cloud1pm/frontend/frontend-dev

# 의존성 설치
npm install

# 개발 서버 실행
npm run dev
```

### 4. Kubernetes Deploy

쿠버네티스 클러스터에 배포하려면 아래 순서대로 매니페스트를 적용하세요.

```bash
# 1. DB (MySQL) 배포
kubectl apply -f backend/backend-dev/k8s-mysql.yaml

# 2. Backend 서비스 배포
kubectl apply -f backend/backend-dev/k8s-backend.yaml

# 3. AI 분석 서비스 배포
kubectl apply -f backend/backend-dev/k8s-ai.yaml

# 4. Frontend 서비스 배포
kubectl apply -f frontend/frontend-dev/k8s-frontend.yaml

# (선택) 배치 작업(알림/리포트) 등록
kubectl apply -f backend/backend-dev/k8s-cronjob-alarm.yaml
kubectl apply -f backend/backend-dev/k8s-cronjob-report.yaml
```

---

## 📜 License

MIT License
