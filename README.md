# GuestHouse
SSG9th 최종 프로젝트

## 개발 환경
- Java 17 (Spring Boot 3.4.x)
- Node.js 18+ (Vite + Vue 3)
- MySQL (로컬/개발 DB 연결 시)

## 빠른 시작
### Frontend
```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
npm run build      # 빌드 검증
```

기본 스텁(`HomeView.vue`, `AboutView.vue`, `HelloWorld.vue`)이 포함되어 있어 바로 실행/빌드됩니다. 도메인 화면을 붙일 때 자유롭게 교체하세요.

### Backend
```bash
cd backend
./gradlew bootRun  # 또는 ./gradlew test / build
```

`src/main/resources/application.properties`에 MySQL 연결 정보가 비어 있습니다. 로컬 DB를 쓸 때 아이디/비밀번호를 채우고, DB 없이 부트하려면 JDBC 자동설정을 잠시 제외하는 설정을 추가하세요:
```
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

## 기타
- CORS: `WebConfig`에 로컬 포트(5173, 8080)만 허용되어 있습니다. 배포 도메인이 생기면 추가하세요.
- Git: 루트 `.gitignore`에 `node_modules`, `build`, `.gradle`, `dist` 등이 포함되어 있습니다.
