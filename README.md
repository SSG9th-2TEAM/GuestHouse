# 🏠 지금이곳 - AI 기반 게스트하우스 플랫폼

> **"AI로 발견하는 나만의 여행, 데이터로 완성하는 호스트의 성공"**
>
> **게스트에게는 취향 저격 감성 숙소를, 호스트에게는 데이터 기반의 운영 솔루션을 제공하는 올인원 플랫폼**

<br/>

## 📖 프로젝트 소개

**지금이곳**은 단순한 숙소 중개를 넘어, **Google Gemini AI** 기술을 서비스 전반에 녹여낸 **지능형 게스트하우스 플랫폼**입니다.

**게스트**는 복잡한 검색 없이 **AI 트래블 가이드**와 대화하며 나만의 여행 코스와 숙소를 추천받고, 수많은 리뷰를 일일이 읽는 대신 **AI 요약**을 통해 숙소의 핵심 정보를 한눈에 파악할 수 있습니다.

**호스트**는 감에 의존하는 운영에서 벗어나, AI가 분석한 **수요 예측**, **트렌드 리포트**, **리뷰 감성 분석**을 통해 데이터 기반의 매출 증대 전략을 수립할 수 있습니다.

기술을 통해 여행의 경험을 혁신하고, 호스트와 게스트를 가장 스마트하게 연결합니다.

<br/>

## 🛠️ Tech Stack

### **Frontend**

<img src="https://img.shields.io/badge/Vue.js 3-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white"> <img src="https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white"> <img src="https://img.shields.io/badge/Pinia-FFE4C4?style=for-the-badge&logo=pinia&logoColor=black"> <img src="https://img.shields.io/badge/Axios-5A29E4?style=for-the-badge&logo=axios&logoColor=white"> <img src="https://img.shields.io/badge/Tailwind CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white">

### **Backend**

<img src="https://img.shields.io/badge/Java 17-007396?style=for-the-badge&logo=java&logoColor=white"> <img src="https://img.shields.io/badge/Spring Boot 3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"> <img src="https://img.shields.io/badge/Spring Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white"> <img src="https://img.shields.io/badge/MyBatis-000000?style=for-the-badge&logo=mybatis&logoColor=white"> <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white">

### **Database & Infrastructure**

<img src="https://img.shields.io/badge/MySQL 8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white"> <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white"> <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"> <img src="https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white"> <img src="https://img.shields.io/badge/NCP (Naver Cloud)-03C75A?style=for-the-badge&logo=naver&logoColor=white">

### **External APIs**

<img src="https://img.shields.io/badge/Google Gemini AI-8E75B2?style=for-the-badge&logo=googlebard&logoColor=white"> <img src="https://img.shields.io/badge/Toss Payments-0050FF?style=for-the-badge&logo=toss&logoColor=white"> <img src="https://img.shields.io/badge/Kakao Map-FFCD00?style=for-the-badge&logo=kakao&logoColor=black"> <img src="https://img.shields.io/badge/NCP Object Storage-03C75A?style=for-the-badge&logo=naver&logoColor=white">

<br/>

## 🏗️ System Architecture

서비스의 안정성과 확장성을 고려하여 **3-Tier 분산 서버 구조**로 설계되었습니다.

![System Architecture](images/system.jpeg)

* **Server 1 (Database):** Docker 컨테이너 기반의 MySQL (Port 3306) 운영.
* **Server 2 (Main Application):** 사용자 및 호스트용 메인 비즈니스 로직 처리 (Port 8080) 및 Redis (Port 6379)를 통한 캐싱/세션 관리.
* **Server 3 (Admin Application):** 관리자 전용 백오피스 서버 분리 (Port 8081).
* **Storage:** NCP Object Storage(S3 호환)를 이용한 이미지 파일 관리.

<br/>

## 📊 ERD (Entity Relationship Diagram)

![ERD](images/Final_ERD.png)

<br/>

## ✨ Key Features

### 1. 🤖 AI-Powered Features (Gemini Flash)

프로젝트 전반에 걸쳐 **Google Gemini AI**를 적극적으로 활용하여 사용자 경험과 운영 효율성을 극대화했습니다.

* **AI 여행 가이드 (Chatbot):** "제주도 2박 3일 여행 코스 추천해줘"와 같은 자연어 질문에 대해 맞춤형 여행 일정과 숙소를 추천합니다.
* **리뷰 요약 및 분석:** 수백 개의 리뷰를 AI가 분석하여 '청결도', '친절함' 등의 키워드로 요약하고, 호스트에게는 개선점을 제안하는 리포트를 생성합니다.
* **숙소 상세 요약:** 긴 숙소 설명을 AI가 핵심만 요약하여 게스트가 빠르게 정보를 파악할 수 있도록 돕습니다.
* **OCR 사업자 등록:** 호스트 입점 시 사업자 등록증 이미지를 AI Vision으로 인식하여 정보를 자동 입력합니다.

### 2. 🏨 게스트하우스 예약 및 결제

* **카카오맵 기반 검색:** 지도 UI를 통해 위치 기반으로 직관적인 숙소 탐색.
* **실시간 예약/결제:** Toss Payments 연동을 통한 안전하고 빠른 결제 시스템.
* **테마별 검색:** 파티, 촌캉스, 오션뷰 등 MZ세대 맞춤형 테마 필터링.

### 3. 📊 호스트 전용 솔루션

* **수요 예측 (Demand Forecast):** 과거 예약 데이터를 기반으로 향후 30일간의 수요를 예측하고 가격 전략 제시.
* **테마 트렌드 리포트:** 지역별/시즌별 인기 테마를 분석하여 매출 증대 전략 제안.
* **Cold Start 대응:** 데이터가 부족한 신규 숙소에게도 지역 트렌드 기반의 맞춤형 가이드 제공.

### 4. 👑 관리자(Admin) 기능

* 호스트 입점 승인 및 반려 프로세스.
* 전체 회원 및 예약 현황 대시보드 관리.
* 쿠폰 및 프로모션 발행 관리.

<br/>

## 📚 Technical Documentation

프로젝트 개발 과정에서 고민한 기술적 이슈와 해결 과정을 문서화했습니다.

### **Architecture & DevOps**
* [**CI/CD Pipeline**](docs/CICD_PIPELINE.md): GitHub Actions와 Docker를 활용한 무중단 배포 파이프라인 구축.
* [**Concurrency Control Strategy**](docs/LOCKING_STRATEGY.md): 선착순 예약 시 발생하는 동시성 문제를 Redis 분산 락(Redisson)으로 해결.
* [**Why Vue 3?**](docs/WHY_VUE3.md): 프론트엔드 프레임워크 선정 이유와 Composition API 활용 전략.

### **Performance Optimization**
* [**Search Logic Optimization**](docs/search_logic.md): 클라이언트 필터링의 한계를 극복하기 위한 서버 사이드 검색 구현 및 인덱싱 적용.
* [**Redis Coupon System**](docs/redis-coupon-notion.md): Redis의 원자적 연산(DECR)을 활용하여 초당 4만 건의 쿠폰 발급 요청 처리.
* [**N+1 Query Optimization**](backend/src/main/resources/db/N+1_OPTIMIZATION_REPORT.md): MyBatis ResultMap 최적화를 통해 숙소 상세 조회 성능 61% 개선.
* [**Main Page Bulk Loading**](backend/src/main/resources/db/MAIN_PAGE_OPTIMIZATION_REPORT.md): 테마별 숙소 조회 시 발생하는 쿼리를 Bulk 조회로 변경하여 응답 속도 87% 단축.

### **AI & Recommendation**
* [**Recommendation System**](docs/RECOMMENDATION_SYSTEM.md): 사용자 취향과 리뷰 태그를 분석한 하이브리드 추천 알고리즘.
* [**AI Travel Guide**](docs/AI_TRAVEL_GUIDE.md): Google Gemini API를 활용한 대화형 여행 가이드 챗봇 구현.
* [**Host AI Report**](docs/host-report-ai.md): 리뷰 감성 분석 및 트렌드 리포트 생성 로직.

<br/>

## 📂 Project Structure

```text
GuestHouse
├── backend                 # Spring Boot Backend
│   ├── src/main/java       # Source code
│   │   └── domain          # Domain-driven design (accommodation, booking, report, ai...)
│   └── src/main/resources  # Config & Mapper xml
├── frontend                # Vue.js Frontend
│   ├── src
│   │   ├── api             # Axios API modules
│   │   ├── components      # Reusable Vue components
│   │   ├── views           # Page views (Host, Guest, Admin)
│   │   └── stores          # Pinia state management
├── docs                    # Technical Documentation & Trouble Shooting
└── README.md
```

<br/>

## 🚀 Getting Started

### Backend

```bash
cd backend
./gradlew clean build
java -jar build/libs/geharbang-0.0.1-SNAPSHOT.jar
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

<br/>

## 👨‍💻 Team Members

|    Role    |   Name    |             Position             |                  GitHub                   |
|:----------:|:---------:|:--------------------------------:|:-----------------------------------------:|
| **Leader** | **[엄현석]** | **메인, 검색, 지도, 리스트 (Main & Map)** | [@EHS](https://github.com/heathcliff4736) |
|   Member   |   [김형근]   | 호스트 관리, 대시보드, 통계 (Host & Admin)  |    [@KHG](https://github.com/geeunii)     |
|   Member   |   [이경민]   |  서버 아키텍처, 결제, AI 추천 (Arch & AI)  |     [@LKM](https://github.com/LKMGIT)     |
|   Member   |   [이재훈]   |   회원, 인증, 인앱 메신저 (User & Chat)   |  [@LJH](https://github.com/jaehoon0321)   |
|   Member   |   [장현우]   | 숙소, 쿠폰, 리뷰, 위시리스트 (Accommoation) |    [@JHW](https://github.com/fsdawer)     |

<br/>

---
*This project was developed as a final team project.*
