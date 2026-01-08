# AI 추천 숙소명/소개 API 명세

본 문서는 숙소 등록/수정 화면에서 사용하는 **이미지 기반 AI 추천 API**를 정의합니다. Vision → GPT 조합으로 결과를 생성하며, JWT 인증이 필요합니다.

---

## 1. 엔드포인트 개요
| 항목 | 값 |
| --- | --- |
| HTTP Method | `POST` |
| Path | `/api/ai/accommodations/naming` |
| Auth | `Authorization: Bearer <access-token>` |
| Content-Type | `application/json` |
| Timeout | 15초 (Vision 5초 + GPT 10초) |

---

## 2. Request Body
```jsonc
{
  "image": "data:image/jpeg;base64,...",
  "language": "ko",
  "context": {
    "city": "제주",
    "district": "애월읍",
    "themes": ["오션뷰", "감성숙소"],
    "stayType": "GUESTHOUSE",
    "existingName": "",
    "existingDescription": ""
  }
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `image` | string | ✅ | `data:image/...;base64,...` 또는 S3 URL. Vision에 그대로 전달. |
| `language` | string | ✅ | `ko`, `en` 등 GPT 출력 언어. 기본 `ko`. |
| `context.city` | string | 선택 | Vision 라벨과 함께 Prompt에 포함될 위치 정보. |
| `context.district` | string | 선택 | 상세 행정구역. |
| `context.themes` | string[] | 선택 | 사용자가 선택한 테마 태그. |
| `context.stayType` | string | 선택 | `GUESTHOUSE`, `PENSION` 등 ENUM. |
| `context.existingName` | string | 선택 | 이미 입력된 제목(있으면 보정). |
| `context.existingDescription` | string | 선택 | 이미 입력된 설명. |

---

## 3. Response Body
```jsonc
{
  "name": "제주 선셋 오션하우스",
  "description": "애월 바다를 바라보는 감성 게스트하우스...",
  "confidence": 0.82,
  "visionLabels": [
    {"description": "Sky", "score": 0.93},
    {"description": "Coast", "score": 0.88}
  ],
  "visionText": "SLOW STAY...\n",
  "model": "gpt-4o-mini-vision",
  "tokens": {
    "prompt": 1024,
    "completion": 186,
    "total": 1210
  },
  "generatedAt": "2025-01-08T03:12:45+09:00"
}
```

| 필드 | 설명 |
| --- | --- |
| `name` | 추천 숙소명 (20자 내외) |
| `description` | 추천 소개문 (2문단 이내) |
| `confidence` | Vision 라벨 확신도 기반 임의 점수 (0~1) |
| `visionLabels` | Vision API가 반환한 주요 라벨 (로그/디버깅용) |
| `visionText` | Vision OCR 결과 전문 (선택) |
| `model` | 사용된 OpenAI 모델명 (`OPENAI_MODEL` 값) |
| `tokens` | GPT 호출 토큰 사용량 (비용 추적용) |
| `generatedAt` | ISO-8601 타임스탬프 |

---

## 4. 오류 코드
| HTTP Status | code | message | 설명 |
| --- | --- | --- | --- |
| 400 | `INVALID_IMAGE` | "이미지를 읽을 수 없습니다." | Base64 파싱 실패 |
| 401 | `UNAUTHORIZED` | "로그인이 필요합니다." | 토큰 없음/만료 |
| 422 | `VISION_ERROR` | "Google Vision 처리 중 오류" | Vision API 실패 |
| 422 | `GPT_ERROR` | "OpenAI 응답 파싱 실패" | GPT 호출 또는 JSON 파싱 오류 |
| 429 | `RATE_LIMITED` | "잠시 후 다시 시도해주세요." | 내부 호출 제한 |
| 500 | `INTERNAL_ERROR` | "AI 추천 생성에 실패했습니다." | 기타 예외 |

오류 응답 형식:
```json
{
  "code": "VISION_ERROR",
  "message": "Google Vision 처리 중 오류가 발생했습니다.",
  "traceId": "a1b2c3d4"
}
```

---

## 5. 시퀀스 요약
1. 프론트가 이미지 업로드 후 `AI 추천` 버튼 클릭.
2. `POST /api/ai/accommodations/naming` 호출 (JWT 포함).
3. 백엔드
   1. 이미지 → Vision (LABEL_DETECTION + TEXT_DETECTION).
   2. Vision 라벨/OCR + context → GPT‑4o Vision.
   3. 응답 파싱 후 `{name, description, ...}` 반환.
4. 프론트가 입력 필드를 추천 값으로 채우고 사용자는 수정 가능.

---

## 6. 구성 요소별 키 관리
- `backend/src/main/resources/application-secret.yml` (git ignore 대상) 내에 아래 항목이 있어야 합니다.
  ```yaml
  OPENAI_API_KEY: sk-***
  OPENAI_MODEL: gpt-4o-mini-vision
  OPENAI_BASE_URL: https://api.openai.com/v1
  google:
    cloud:
      credentials:
        location: file:/abs/path/google-vision.json
  ```
- 서버 환경에서는 `export OPENAI_API_KEY=...`, `export GOOGLE_APPLICATION_CREDENTIALS=...` 등으로 주입 후 yml에서 `${}` 로 참조 가능합니다.

이 문서는 AI 추천 기능을 구현하는 개발자/QA가 동일한 규격으로 통신할 수 있도록 유지보수합니다.
