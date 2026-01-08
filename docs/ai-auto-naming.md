# AI 기반 숙소명/소개 추천 가이드

숙소 등록/수정 화면에서 **이미지를 먼저 업로드**하면, AI가 해당 이미지를 분석해 숙소명과 소개 문구를 제안하는 기능을 구축하기 위한 절차입니다.  
Google Cloud Vision으로 이미지 캡션/태그를 뽑고, OpenAI GPT‑4o Vision(또는 GPT‑4.1 Vision)으로 자연스러운 텍스트를 생성하는 2단계 구조를 사용합니다.

---

## 전체 흐름
1. 사용자가 배너/상세 이미지를 업로드하면 프론트에서 미리보기와 함께 `AI 추천` 버튼을 표시합니다.
2. 프론트는 업로드한 이미지를 Base64 혹은 S3 URL로 백엔드 AI 추천 API에 보냅니다.
3. 백엔드는 Google Cloud Vision으로 이미지에서 텍스트/라벨/설명을 추출합니다.
4. Vision 결과(라벨, OCR 텍스트, 지리 정보 등)를 Prompt에 포함해 OpenAI GPT‑4o Vision에게 숙소명·소개문을 생성하도록 요청합니다.
5. 백엔드는 `{name, description, confidence}` 형태로 응답을 반환하고, 프론트는 입력 필드를 해당 값으로 채워주되 사용자가 수정할 수 있도록 합니다.

---

## Google Cloud Vision 세팅
1. **프로젝트 생성** → Vision API 활성화.
2. **서비스 계정** 생성 후 JSON 키 파일을 다운받습니다.
3. 백엔드에서 사용할 서비스 계정 JSON은 Git에 올리지 말고 `GOOGLE_APPLICATION_CREDENTIALS` 경로 or Secret Manager로 관리합니다.
4. Spring Boot에서는 `spring.cloud.gcp.credentials.location=file:/path/to/key.json` 혹은 `application-secret.yml`에 다음처럼 설정합니다.
   ```yaml
   spring:
     cloud:
       gcp:
         credentials:
           location: file:${GCP_CREDENTIAL_FILE}
         project-id: ${GCP_PROJECT_ID}
   ```
5. Vision API 호출은 REST/SDK 둘 다 가능하며, Base64 인코딩한 이미지를 `image.content`에 넣어 `TEXT_DETECTION`, `LABEL_DETECTION`을 동시에 요청합니다.

---

## OpenAI GPT‑4o Vision 연동
1. 현재 Codex CLI에서 사용 중인 OpenAI API 키가 Vision 모델 권한을 갖고 있는지 확인합니다. (계정 콘솔 → Limits → GPT‑4o Vision)
2. 백엔드 `application-secret.yml`에 모델명과 키를 별도로 관리합니다.
   ```yaml
   openai:
     api-key: ${OPENAI_API_KEY}
     model: gpt-4o-mini-vision
   ```
3. Spring에서 `@ConfigurationProperties(prefix = "openai")` 혹은 `@Value`로 키/모델을 주입해 OpenAI SDK 또는 REST 호출에 사용합니다.
4. Vision 호출 시 Google Vision에서 추출한 라벨 정보를 Prompt에 포함합니다.
   ```jsonc
   {
     "model": "gpt-4o-mini-vision",
     "messages": [
       {
         "role": "system",
         "content": "당신은 게스트하우스 작명 도우미입니다. 이미지 라벨과 주변 정보를 참고해 이름/소개를 2문장 이내로 제안하세요."
       },
       {
         "role": "user",
         "content": [
           {"type": "input_text", "text": "라벨: beach, sunrise, cozy interior ..."},
           {"type": "input_image", "image_url": "data:image/jpeg;base64,...."}
         ]
       }
     ]
   }
   ```

---

## Spring Boot 비밀 설정
1. `backend/src/main/resources/application-secret.example.yml`을 복사해 `application-secret.yml`로 저장합니다. (Git에서 무시되므로 안전)
2. 아래 속성에 실제 값을 입력합니다.
   ```yaml
   OPENAI_API_KEY: sk-...
   OPENAI_MODEL: gpt-4o-mini-vision
   OPENAI_BASE_URL: https://api.openai.com/v1
   google:
     cloud:
       credentials:
         location: file:/absolute/path/to/google-vision-key.json
   ```
3. 운영 환경에서는 위 값을 환경 변수로 주입하고, yml에서는 `${VAR_NAME}` 형태로 참조할 수 있습니다.
4. `application.properties`는 `spring.config.import=...application-secret.yml`을 포함하므로 서버 재시작 시점에 자동으로 로드됩니다.

---

## 백엔드 API 설계 예시
```
POST /api/ai/accommodations/naming
Request:
{
  "image": "data:image/jpeg;base64,...",
  "language": "ko"
}

Response:
{
  "name": "제주 선셋 오션하우스",
  "description": "노을이 드는 객실과 야외 정원이 있는 감성 숙소입니다.",
  "confidence": 0.82,
  "visionLabels": ["coast", "sunset", "guesthouse"]
}
```

- 실패 시에는 Vision/GPT 각각 어디서 오류가 났는지 메시지/로그 감싸서 전달합니다.
- 프론트에서는 `추천값 적용`/`무시` 두 가지 UX를 노출합니다.

---

## 프론트 적용 아이디어
- 배너/상세 이미지 업로드 이후 곧바로 `AI 추천` 버튼 활성화.
- 버튼 클릭 → 로딩 스피너 → 추천 값이 오면 `적용`/`다시 요청` 선택 가능.
- 입력 필드에 적용한 후에도 사용자가 자유롭게 수정 가능.

---

## 운영 시 고려사항
- **비용**: Vision API는 월 1000유닛 무료, 이후 유료. GPT‑4o Vision은 토큰+이미지 단위로 과금됩니다.
- **캐시**: 동일 이미지 재요청 시 결과를 Redis 등에 캐싱해 비용 절감.
- **타임아웃/리트라이**: Vision/GPT 각각의 SLA가 다르므로 타임아웃을 분리하고, 실패 시 Graceful fallback 메시지 제공.
- **보안**: 이미지 데이터가 민감할 수 있으므로 HTTPS 전송, 임시 URL/서명 URL 사용. API 키는 Secret Manager 및 환경변수로만 주입합니다.

상세 엔드포인트 규격은 `docs/ai-auto-naming-api.md`를 참고하세요.
