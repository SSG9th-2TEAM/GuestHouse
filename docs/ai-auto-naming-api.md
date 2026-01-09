# AI 추천 숙소명/소개 API 명세 (Gemini 버전)

숙소 등록/수정 화면에서 사용하는 **이미지 기반 AI 추천** API를 정의합니다. Google Cloud Vision으로 이미지 특징을 추출하고, Google Gemini로 숙소명/소개를 생성합니다. JWT 인증이 필요합니다.

---

## 1. 엔드포인트
| 항목 | 값 |
| --- | --- |
| HTTP Method | `POST` |
| Path | `/api/ai/accommodations/naming` |
| Auth | `Authorization: Bearer <access-token>` |
| Content-Type | `application/json` |
| Timeout | 약 15초 (Vision + Gemini) |

---

## 2. Request Body
```jsonc
{
  "images": [
    "data:image/jpeg;base64,...",
    "data:image/png;base64,..."
  ],
  "language": "ko",
  "context": {
    "city": "제주",
    "district": "애월읍",
    "township": "어음리",
    "themes": ["오션뷰", "감성숙소"],
    "stayType": "GUESTHOUSE",
    "existingName": "기존 이름",
    "existingDescription": "이미 입력된 소개문"
  }
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `images` | string[] | ✅ | `data:image/...;base64,...` 형식의 이미지 배열. 최대 5장 권장. |
| `language` | string | 선택 | 응답 언어. 기본 `ko`. |
| `context.city` | string | 선택 | 시/도 정보. |
| `context.district` | string | 선택 | 구/군 정보. |
| `context.township` | string | 선택 | 읍/면/동 정보. |
| `context.themes` | string[] | 선택 | 사용자가 선택한 테마. |
| `context.stayType` | string | 선택 | `GUESTHOUSE`, `PENSION` 등 ENUM. |
| `context.existingName` | string | 선택 | 이미 입력된 숙소명 (AI 보정용). |
| `context.existingDescription` | string | 선택 | 이미 입력된 소개문. |

> 프론트에서는 업로드한 이미지를 최대 1024px로 리사이징한 뒤 Base64로 변환하여 전송합니다.

---

## 3. Response Body
```jsonc
{
  "name": "제주 선셋 오션하우스",
  "description": "애월 바다를 바라보는 감성 게스트하우스입니다...",
  "confidence": 0.82,
  "visionLabels": [
    {"description": "Costal view", "score": 0.91},
    {"description": "Sunset", "score": 0.88}
  ],
  "visionText": "SLOW STAY ...",
  "model": "gemini-2.0-flash",
  "tokenUsage": {
    "prompt": 820,
    "completion": 210,
    "total": 1030
  },
  "generatedAt": "2026-01-08T11:12:45+09:00"
}
```

| 필드 | 설명 |
| --- | --- |
| `name` | 추천 숙소명. |
| `description` | 추천 소개문. |
| `confidence` | AI가 응답에 부여한 상대적 확신도 (0~1). |
| `visionLabels` | Vision 라벨 결과 (중복 제거 후 score 기준 정렬). |
| `visionText` | Vision OCR 전체 텍스트. |
| `model` | 사용된 Gemini 모델명. |
| `tokenUsage` | Gemini API 토큰 사용량 (`prompt`, `completion`, `total`). |
| `generatedAt` | ISO-8601 타임스탬프. |

---

## 4. 오류 코드
| HTTP Status | code | message | 설명 |
| --- | --- | --- | --- |
| 400 | `INVALID_IMAGE` | "이미지를 읽을 수 없습니다." | Base64 파싱 실패 |
| 401 | `UNAUTHORIZED` | "로그인이 필요합니다." | JWT 없음/만료 |
| 422 | `VISION_ERROR` | "Google Vision 처리 중 오류" | Vision API 실패 |
| 422 | `GEMINI_ERROR` | "Gemini 응답 파싱 실패" | Gemini 호출/JSON 파싱 오류 |
| 429 | `RATE_LIMITED` | "잠시 후 다시 시도해주세요." | 내부 호출 제한 |
| 500 | `INTERNAL_ERROR` | "AI 추천 생성에 실패했습니다." | 기타 예외 |

에러 예시:
```json
{
  "code": "GEMINI_ERROR",
  "message": "Gemini 처리 중 오류가 발생했습니다.",
  "traceId": "a1b2c3d4"
}
```

---

## 5. 시퀀스 요약
1. 프론트가 이미지 업로드 후 `AI 추천` 버튼 클릭 → 이미지 리사이징(Base64) → API 호출.
2. 백엔드는 Google Vision으로 라벨/OCR을 추출하고, Gemini에 JSON Schema와 함께 프롬프트를 전달한다.
3. Gemini 응답을 파싱해 `{name, description, confidence, ...}`를 조합하여 반환한다.
4. 프론트가 응답을 입력 필드에 반영하고, 사용자가 수정/저장한다.

---

## 6. 비밀 키 관리
`backend/src/main/resources/application-secret.yml` (Git ignore) 예시:
```yaml
GEMINI_API_KEY: AIza...
GEMINI_MODEL: gemini-2.0-flash
GEMINI_BASE_URL: https://generativelanguage.googleapis.com/v1
google:
  cloud:
    credentials:
      location: classpath:googlevision/ocr-test-480914-b8a930bfbdc0.json
```
운영 환경에서는 `export GEMINI_API_KEY=...` 식으로 주입 후 `${GEMINI_API_KEY}`로 참조할 수 있습니다.

---

## 7. 프론트 가이드 (요약)
- 이미지 업로드 후 `canvas` 또는 `browser-image-compression`으로 1024px 이하로 축소, JPEG 0.85 품질로 Base64를 생성합니다.
- 배너 + 상세 이미지를 모두 `images` 배열로 전송합니다(최대 5장 권장).
- 로딩/오류 상태를 사용자에게 명확히 보여주고, 응답이 오면 `form.name`, `form.description`에 적용합니다.

이 문서는 실제 구현(2026-01-08 기준)에 맞춰 유지보수됩니다.
