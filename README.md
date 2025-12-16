# GuestHouse

SSG 9th 최종 프로젝트 — 게스트하우스 플랫폼 (Spring Boot + Vue)

> `develop` 브랜치는 팀 통합용 브랜치입니다. 기능 개발은 dev/개인 브랜치에서 작업 후 PR로 `develop`에 병합합니다.

---

## ✨ 주요 기능
- 게스트: 회원가입/로그인, 숙소 검색/조회, 예약/결제, 마이페이지
- 호스트: 숙소 등록/관리, 예약 관리, 정산 관리, 호스트 전용 챗봇, 대시보드
- 관리자: 숙소 승인/관리, 예역 조회 및 관리, 회원 조회 및 관리, 대시보드
- 공통: 리뷰

---

## 🧩 기술 스택
### Backend
- Java 17 / Spring Boot 3.4.x
- JPA + MyBatis (혼용)
- Spring Security
- MySQL

### Frontend
- Node.js 18+
- Vue 3 + Vite

---

## 🗂 레포 구조
```text
root
├── frontend/        # Vue 3 + Vite
└── backend/         # Spring Boot
````

## 🤖 CI (GitHub Actions)
- 워크플로 위치: `.github/workflows/ci.yml`
- 트리거: `push`/`pull_request` → 백엔드 `./gradlew build -x test`, 프런트 `npm ci && npm run build`
- 테스트/DB 마이그레이션 추가: 테스트 DB 프로파일 준비 후 `-x test` 제거하고 `./gradlew flywayMigrate` 등을 단계에 넣으면 됩니다.
- 로컬 확인: 루트에서 `cd backend && ./gradlew build`, `cd frontend && npm run build`로 워크플로와 동일하게 실행해볼 수 있습니다.

---

## ⚡ 빠른 시작

### Frontend

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
npm run build      # 빌드 검증
```

> 기본 스텁(`HomeView.vue`, `AboutView.vue`, `HelloWorld.vue`)이 포함되어 있어 바로 실행/빌드됩니다.
> 도메인 화면을 붙일 때 자유롭게 교체하세요.

### Backend

```bash
cd backend
./gradlew bootRun  # 또는 ./gradlew test / build
```

#### DB 설정

`src/main/resources/application.properties`(또는 `application-local.properties`)에 MySQL 연결 정보를 채우세요.

DB 없이 부트가 필요하면 아래 설정을 임시로 추가합니다:

```properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

---

## 🔀 브랜치 전략

* `main` : 배포/최종본(안정 브랜치)
* `develop` : 통합 개발 브랜치(기능 PR이 모이는 곳)
* `dev/개인` : 개인 작업 브랜치

---

## ✅ 협업 규칙 (중요)

### Issue → Branch → PR

1. Issue 생성(작업 범위/DoD 작성)
2. 브랜치 생성 후 작업
3. PR 생성 → `develop`로 병합

### PR 작성 규칙

* 제목 예시: `[FEAT] 로그인 API 구현`, `[FIX] 예약 취소 버그 수정`
* 본문에 이슈 자동 연결(머지 시 이슈 자동 종료):

  * `Closes #12`
* 최소 1명 승인 필요(룰셋 기준)

### 커밋 메시지(권장)

* `feat: ...`, `fix: ...`, `docs: ...`, `refactor: ...`, `chore: ...`

---

## 🧷 기타

* **CORS**: `WebConfig`에 로컬 포트(5173, 8080)만 허용되어 있습니다. 배포 도메인이 생기면 추가하세요.
* **Gitignore**: 루트 `.gitignore`에 `node_modules`, `.gradle`, `dist`, `build` 등이 포함되어 있습니다.

---

## 🔗 링크

* API 문서(Swagger): (추후 추가)
* ERD / 기획 / 와이어프레임: (추후 추가)
* 팀 규칙/노션: (추후 추가)
