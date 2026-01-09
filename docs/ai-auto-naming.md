# AI 기반 숙소명/소개 추천 가이드 (Vision + Gemini)

숙소 등록/수정 화면에서 이미지를 업로드한 뒤 **AI로 숙소 소개 받기** 버튼을 누르면, Google Cloud Vision과 Google Gemini를 조합해 숙소명/소개를 제안합니다. 이 문서는 실제 구현(2026년 1월 기준)과 동일한 구성 요소, 설정 방법, 운영 팁을 정리한 것입니다.

---

## 전체 흐름
1. 사용자가 배너/상세 이미지를 업로드하면 프론트에서 저용량 Base64로 변환합니다. (모바일 네트워크에서도 빨리 전송되도록 캔버스로 1024px 이하로 리사이징)
2. 프론트는 최대 5장의 이미지를 `images: []` 배열로 `/api/ai/accommodations/naming`에 전달합니다.
3. 백엔드는 Google Cloud Vision으로 **라벨/텍스트**를 추출합니다. 여러 이미지의 라벨은 HashSet으로 중복 제거합니다.
4. Vision 결과 + 위치/테마 등의 컨텍스트로 Prompt를 만들고, **Gemini 2.0 Flash** 모델에 JSON Schema를 강제하여 `{name, description, confidence}`를 생성합니다.
5. 응답에는 Vision 라벨/텍스트, Gemini 모델명, 토큰 사용량이 포함됩니다. 프론트는 입력 필드에 값을 적용하고 사용자가 수정할 수 있습니다.

---

## Google Cloud Vision 설정
1. GCP 프로젝트에서 **Vision API**를 Enable하고 서비스 계정 키(JSON)를 발급합니다.
2. `backend/src/main/resources/application-secret.yml` (git ignore)에서 아래와 같이 경로를 지정합니다.
   ```yaml
   google:
     cloud:
       credentials:
         location: classpath:googlevision/ocr-test-480914-b8a930bfbdc0.json
   ```
   또는 `file:/abs/path/key.json` 형식으로 지정할 수 있습니다.
3. Vision에서는 `TEXT_DETECTION` + `LABEL_DETECTION`을 동시에 호출하며, 다중 이미지는 `batchAnnotateImages`로 처리합니다.
4. 서버 단에서도 최대 1024px로 리사이징하여 Vision 호출 비용을 줄입니다(`VisionImageAnalyzer#resizeIfNeeded`).

---

## Gemini 텍스트 모델 설정
1. GCP Console에서 **Generative Language API**를 Enable합니다.
2. API 키를 발급한 뒤 `application-secret.yml`에 아래와 같이 입력합니다.
   ```yaml
   GEMINI_API_KEY: AIza...
   GEMINI_MODEL: gemini-2.0-flash
   GEMINI_BASE_URL: https://generativelanguage.googleapis.com/v1
   ```
3. `GeminiTextClient`는 `generationConfig.responseMimeType=application/json`과 Schema를 지정해 JSON 응답만 받습니다.
4. 토큰 사용량(`usageMetadata.promptTokenCount`, `candidatesTokenCount`, `totalTokenCount`)을 `AccommodationAiSuggestionResponse.tokenUsage`로 전달합니다.

---

## Spring Boot 연동
1. `application.properties`에서 `spring.config.import=optional:classpath:application-secret.yml`를 선언해 secret을 자동 로드합니다.
2. `AccommodationAiController` (`/api/ai/accommodations/naming`)는 JWT 인증 후 `AccommodationAiService`를 호출합니다.
3. `AccommodationAiService`는 `visionImageAnalyzer.analyzeMultiple(images)` → `geminiTextClient.generateSuggestion()` 순으로 호출하고, Vision 텍스트/라벨을 프롬프트에 함께 전달합니다.
4. 실패 시 `IllegalStateException`이 전파되고 `GlobalExceptionHandler`가 `500 INTERNAL_ERROR` 형태로 응답합니다.

---

## 프론트엔드 처리
- `frontend/src/api/ai.js` : `/ai/accommodations/naming`에 POST 요청을 보내는 래퍼.
- `HostAccommodationRegister.vue` / `HostAccommodationEdit.vue`
  - 업로드한 모든 이미지(배너 + 상세)를 수집하고, 캔버스로 1024px 이하/품질 0.85로 리사이징 후 Base64로 변환합니다.
  - `images: string[]` 형태로 API에 전달합니다.
  - 응답을 받으면 `form.name`, `form.description`을 덮어쓰고, 모달로 안내합니다.
  - 로딩 상태(`isAiSuggesting`)와 오류 메시지를 UI에 표시합니다.

---

## 운영 시 고려사항
- **비용**: Vision API는 이미지를 많이 보내면 비용이 늘어나므로, 프론트와 백엔드 모두 리사이징을 적용했습니다.
- **쿼터**: Gemini API는 프로젝트/키마다 사용 한도가 있으므로, Billing/Usage 대시보드에서 모니터링합니다.
- **오류 대응**: `API_KEY_INVALID`, `NOT_FOUND`, `RATE_LIMIT` 등의 에러 메시지를 그대로 사용자에게 노출하지 않도록 `GlobalExceptionHandler`에서 공통 메시지를 내려줍니다.
- **문서 동기화**: 구현은 Gemini 기준이므로, 문서/README 등을 수정할 때도 `GEMINI_*` 키와 모델명을 사용합니다.

자세한 필드 정의와 오류 코드는 `docs/ai-auto-naming-api.md` 문서를 참고하세요.
