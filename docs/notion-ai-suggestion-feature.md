# AI 숙소 소개 자동 추천 기능

## 📋 기능 개요

호스트가 숙소를 등록할 때 이미지만 업로드하면, AI가 자동으로 **숙소명**과 **소개글**을 추천해주는 기능입니다.

### 왜 이 기능을 만들었나?
- 호스트들이 "뭐라고 써야 할지 모르겠다"는 피드백이 많았음
- 매력적인 소개글 작성은 예약률에 직접적인 영향을 미침
- 이미지에서 정보를 추출하면 더 정확하고 일관된 소개글 생성 가능

---

## 🔄 요청 플로우

### 사용자 관점
1. 숙소 등록/수정 페이지에서 이미지 업로드
2. **"AI로 숙소 소개 받기"** 버튼 클릭
3. 약 3~5초 대기
4. 숙소명과 소개글이 자동으로 입력됨

### 기술 관점
```
[프론트엔드]
     │
     │  POST /api/ai/accommodations/naming
     │  {
     │    images: ["base64...", "base64..."],  ← 배너 + 상세이미지 모두
     │    language: "ko",
     │    context: { city, district, themes, ... }
     │  }
     │  + JWT 토큰 (Authorization 헤더)
     │
     ▼
[백엔드 API]
```

---

## 🏗️ 백엔드 아키텍처

### 전체 구조도

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         AccommodationAiController                        │
│                    POST /api/ai/accommodations/naming                    │
│                                                                          │
│  • 인증 확인 (JWT → HostIdentityResolver)                                │
│  • 요청 유효성 검증 (@Valid)                                              │
│  • 서비스 호출 및 응답 반환                                                │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         AccommodationAiService                           │
│                                                                          │
│  1. 이미지 리스트를 Vision API로 분석 요청                                 │
│  2. Vision 결과 + 숙소 컨텍스트로 프롬프트 생성                            │
│  3. Gemini API로 텍스트 생성 요청                                         │
│  4. 응답 조합하여 반환                                                    │
└─────────────────────────────────────────────────────────────────────────┘
                    │                               │
                    ▼                               ▼
┌──────────────────────────────┐    ┌──────────────────────────────────────┐
│     VisionImageAnalyzer      │    │          GeminiTextClient            │
│                              │    │                                      │
│  • Google Cloud Vision API   │    │  • Google Gemini API                 │
│  • 라벨 감지 (Label Detection)│    │  • 모델: gemini-2.0-flash            │
│  • 텍스트 감지 (OCR)          │    │  • JSON 스키마 강제 출력              │
│  • 다중 이미지 일괄 분석      │    │  • 온도: 0.5 (안정적 출력)            │
└──────────────────────────────┘    └──────────────────────────────────────┘
```

---

## 📁 파일별 상세 설명

### 1. AccommodationAiController
**경로:** `domain/accommodation/controller/AccommodationAiController.java`

```java
@RestController
@RequestMapping("/api/ai/accommodations")
@RequiredArgsConstructor
public class AccommodationAiController {

    private final AccommodationAiService accommodationAiService;
    private final HostIdentityResolver hostIdentityResolver;

    @PostMapping("/naming")
    public ResponseEntity<AccommodationAiSuggestionResponse> suggestNaming(
            @Valid @RequestBody AccommodationAiSuggestionRequest request,
            Authentication authentication
    ) {
        // 1. JWT에서 호스트 ID 추출
        Long hostId = hostIdentityResolver.resolveHostUserId(authentication);

        // 2. AI 서비스 호출
        AccommodationAiSuggestionResponse response = accommodationAiService.suggest(hostId, request);

        // 3. 응답 반환
        return ResponseEntity.ok(response);
    }
}
```

**역할:**
- API 엔드포인트 정의
- 인증된 호스트만 접근 가능하도록 `HostIdentityResolver`로 검증
- 요청 DTO 유효성 검사 (`@Valid`)

---

### 2. AccommodationAiService
**경로:** `domain/accommodation/service/AccommodationAiService.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AccommodationAiService {

    private final VisionImageAnalyzer visionImageAnalyzer;
    private final GeminiTextClient geminiTextClient;

    public AccommodationAiSuggestionResponse suggest(Long hostId, AccommodationAiSuggestionRequest request) {
        // 1. 이미지 유효성 검사
        if (request == null || request.getImages() == null || request.getImages().isEmpty()) {
            throw new IllegalArgumentException("이미지 데이터가 필요합니다.");
        }

        // 2. Vision API로 모든 이미지 분석 (배너 + 상세이미지)
        VisionImageAnalyzer.ImageAnalysisResult analysisResult =
            visionImageAnalyzer.analyzeMultiple(request.getImages());

        // 3. 프롬프트 생성 (Vision 결과 + 숙소 컨텍스트)
        String prompt = buildPrompt(request.getContext(), analysisResult);

        // 4. Gemini API 호출
        GeminiTextClient.TextCompletionResult aiResult =
            geminiTextClient.generateSuggestion(prompt, request.resolveLanguage());

        // 5. 응답 조합
        return AccommodationAiSuggestionResponse.builder()
                .name(aiResult.getName())
                .description(aiResult.getDescription())
                .confidence(aiResult.getConfidence())
                .visionLabels(analysisResult.getLabels())
                .visionText(analysisResult.getFullText())
                .model(aiResult.getModel())
                .tokenUsage(...)
                .generatedAt(aiResult.getGeneratedAt())
                .build();
    }
}
```

**프롬프트 생성 로직:**
```java
private String buildPrompt(AccommodationAiSuggestionContext context,
                           VisionImageAnalyzer.ImageAnalysisResult analysisResult) {
    StringBuilder builder = new StringBuilder();
    builder.append("아래 정보와 이미지를 참고하여 숙소 이름과 소개문을 제안해라.\n");

    // 숙소 컨텍스트 추가
    if (context != null) {
        if (hasText(context.getExistingName())) {
            builder.append("- 기존 이름: ").append(context.getExistingName()).append("\n");
        }
        if (hasText(context.getStayType())) {
            builder.append("- 숙소 유형: ").append(context.getStayType()).append("\n");
        }
        // 위치: 서울특별시 강남구 역삼동
        String location = joinLocation(context.getCity(), context.getDistrict(), context.getTownship());
        if (hasText(location)) {
            builder.append("- 위치: ").append(location).append("\n");
        }
        // 테마: 감성, 모던, 커플
        if (context.getThemes() != null && !context.getThemes().isEmpty()) {
            builder.append("- 테마: ").append(String.join(", ", context.getThemes())).append("\n");
        }
    }

    // Vision 분석 결과 추가
    if (analysisResult != null) {
        // 라벨: Room(95%), Interior design(89%), Furniture(85%)
        if (analysisResult.getLabels() != null && !analysisResult.getLabels().isEmpty()) {
            String labelSummary = analysisResult.getLabels().stream()
                    .limit(5)
                    .map(label -> label.getDescription() + "(" + Math.round(label.getScore() * 100) + "%)")
                    .collect(Collectors.joining(", "));
            builder.append("- Vision 라벨: ").append(labelSummary).append("\n");
        }
        // OCR 텍스트: WiFi 비밀번호, 환영 메시지 등
        if (hasText(analysisResult.getFullText())) {
            builder.append("- 이미지 OCR 텍스트: ").append(truncate(analysisResult.getFullText(), 400)).append("\n");
        }
    }

    builder.append("결과는 간결하고 감성적인 톤으로 작성한다.");
    return builder.toString();
}
```

---

### 3. VisionImageAnalyzer
**경로:** `domain/ai/vision/VisionImageAnalyzer.java`

**역할:** Google Cloud Vision API를 사용하여 이미지에서 정보 추출

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class VisionImageAnalyzer {

    @Value("${google.cloud.credentials.location:}")
    private Resource credentialsResource;

    private ImageAnnotatorSettings visionSettings;
    private boolean enabled = false;

    // 초기화: 인증 정보 로드
    @PostConstruct
    public void init() {
        // Google Cloud 인증 파일 로드
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(credentialsResource.getInputStream())
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-vision"));

        visionSettings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build();
        enabled = true;
    }

    // 다중 이미지 분석 (배너 + 상세이미지 모두)
    public ImageAnalysisResult analyzeMultiple(List<String> base64Images) {
        // 1. 각 이미지를 AnnotateImageRequest로 변환
        List<AnnotateImageRequest> requests = new ArrayList<>();
        for (String base64Image : base64Images) {
            byte[] bytes = decode(base64Image);  // Base64 → byte[] + 리사이징

            Image image = Image.newBuilder()
                    .setContent(ByteString.copyFrom(bytes))
                    .build();

            // 두 가지 분석 기능 요청
            Feature textFeature = Feature.newBuilder()
                    .setType(Feature.Type.TEXT_DETECTION)  // OCR
                    .build();
            Feature labelFeature = Feature.newBuilder()
                    .setType(Feature.Type.LABEL_DETECTION)  // 라벨 감지
                    .build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(textFeature)
                    .addFeatures(labelFeature)
                    .setImage(image)
                    .build();
            requests.add(request);
        }

        // 2. 일괄 분석 요청 (BatchAnnotateImages)
        BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);

        // 3. 결과 통합 (모든 이미지의 라벨/텍스트 합치기)
        StringBuilder textBuilder = new StringBuilder();
        List<VisionLabel> allLabels = new ArrayList<>();

        for (AnnotateImageResponse result : response.getResponsesList()) {
            // OCR 텍스트 추가
            if (!result.getTextAnnotationsList().isEmpty()) {
                String text = result.getTextAnnotations(0).getDescription();
                textBuilder.append(text).append("\n---\n");
            }
            // 라벨 추가 (중복 제거)
            result.getLabelAnnotationsList().forEach(entity -> {
                VisionLabel label = VisionLabel.builder()
                        .description(entity.getDescription())
                        .score(entity.getScore())
                        .build();
                // 같은 라벨이 없으면 추가
                if (allLabels.stream().noneMatch(l -> l.getDescription().equals(label.getDescription()))) {
                    allLabels.add(label);
                }
            });
        }

        // 4. 라벨을 점수 순으로 정렬
        allLabels.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));

        return ImageAnalysisResult.success(textBuilder.toString(), allLabels);
    }
}
```

**왜 다중 이미지를 분석하나?**
- 배너 이미지만으로는 정보가 부족할 수 있음
- 상세 이미지에는 객실 내부, 시설, 주변 환경 등 다양한 정보가 있음
- 여러 이미지를 분석하면 더 풍부한 컨텍스트로 정확한 소개글 생성 가능

**Vision API 분석 결과 예시:**
```json
{
  "labels": [
    {"description": "Room", "score": 0.95},
    {"description": "Interior design", "score": 0.89},
    {"description": "Furniture", "score": 0.85},
    {"description": "Wood", "score": 0.82},
    {"description": "Window", "score": 0.78}
  ],
  "fullText": "WELCOME\nWiFi: guest_house_5G\nPassword: welcome123"
}
```

---

### 4. GeminiTextClient
**경로:** `domain/ai/gemini/GeminiTextClient.java`

**역할:** Google Gemini API를 호출하여 텍스트 생성

```java
@Component
@Slf4j
public class GeminiTextClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String apiKey;      // GEMINI_API_KEY
    private final String model;       // gemini-2.0-flash
    private final String baseUrl;     // https://generativelanguage.googleapis.com/v1beta

    public TextCompletionResult generateSuggestion(String prompt, String languageTag) {
        // 1. Gemini API 요청 본문 구성
        Map<String, Object> generationConfig = Map.of(
                "temperature", 0.5,  // 안정적인 출력 (0.0~2.0)
                "responseMimeType", "application/json",  // JSON 응답 강제
                "responseSchema", Map.of(  // 출력 스키마 정의
                        "type", "OBJECT",
                        "properties", Map.of(
                                "name", Map.of("type", "STRING"),
                                "description", Map.of("type", "STRING"),
                                "confidence", Map.of("type", "NUMBER")
                        ),
                        "required", List.of("name", "description")
                )
        );

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", buildPrompt(prompt, languageTag)))
                )),
                "generationConfig", generationConfig
        );

        // 2. API 호출
        String url = String.format("%s/models/%s:generateContent?key=%s", baseUrl, model, apiKey);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        // 3. 응답 파싱
        return parseResponse(response.getBody());
    }

    // 시스템 프롬프트 추가
    private String buildPrompt(String contextPrompt, String languageTag) {
        return "당신은 게스트하우스 기획 담당자입니다. 아래 힌트를 참고하여 숙소 이름과 소개문을 JSON으로 작성하세요.\n"
                + "출력 형식은 {\"name\":string,\"description\":string,\"confidence\":number} 입니다.\n"
                + "confidence는 0과 1 사이 숫자입니다.\n"
                + "소개문(description)은 최소 8문장 이상으로 상세하게 작성하세요. "
                + "숙소의 분위기, 위치적 장점, 주변 관광지, 편의시설, 특별한 서비스, "
                + "추천 대상, 계절별 매력 등을 풍부하게 포함해주세요.\n"
                + "응답 언어: " + languageTag + "\n\n"
                + contextPrompt;
    }
}
```

**왜 Gemini를 선택했나?**

| 항목 | GPT-4o-mini | Gemini 2.0 Flash |
|------|-------------|------------------|
| 가격 | $0.15 / 1M 토큰 | 무료 티어 있음 |
| 속도 | 빠름 | 매우 빠름 |
| 한국어 | 좋음 | 좋음 |
| JSON 출력 | function calling 필요 | 네이티브 지원 |

**Gemini의 장점:**
1. **responseSchema**로 JSON 형식 강제 가능 → 파싱 오류 방지
2. 무료 티어로 개발/테스트 가능
3. Google Cloud 생태계와 통합 용이 (Vision API와 같은 인증)

---

### 5. DTOs (Data Transfer Objects)

#### AccommodationAiSuggestionRequest
```java
@Getter
@Setter
public class AccommodationAiSuggestionRequest {

    @NotEmpty(message = "이미지 데이터는 필수입니다.")
    private List<String> images;  // Base64 이미지 리스트

    private String language = "ko";  // 응답 언어

    @Valid
    private AccommodationAiSuggestionContext context;  // 숙소 컨텍스트
}
```

#### AccommodationAiSuggestionContext
```java
@Getter
@Setter
public class AccommodationAiSuggestionContext {
    private String existingName;        // 기존 숙소명
    private String existingDescription; // 기존 소개글
    private String stayType;            // 숙소 유형 (게스트하우스, 펜션 등)
    private String city;                // 시/도
    private String district;            // 구/군
    private String township;            // 동/읍/면
    private List<String> themes;        // 테마 (감성, 모던, 커플 등)
}
```

#### AccommodationAiSuggestionResponse
```java
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccommodationAiSuggestionResponse {
    private final String name;              // 추천 숙소명
    private final String description;       // 추천 소개글
    private final Double confidence;        // AI 확신도 (0.0 ~ 1.0)
    private final List<VisionLabel> visionLabels;  // Vision 감지 라벨
    private final String visionText;        // OCR 추출 텍스트
    private final String model;             // 사용된 AI 모델
    private final TokenUsage tokenUsage;    // 토큰 사용량
    private final String generatedAt;       // 생성 시각
}
```

---

## 🔀 동작 순서 (상세 다이어그램)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              [Host UI - 프론트엔드]                           │
│                                                                              │
│  1. 호스트가 이미지 업로드 (배너 1장 + 상세 N장)                              │
│  2. "AI로 숙소 소개 받기" 버튼 클릭                                          │
│  3. 모든 이미지를 Base64로 변환                                              │
│  4. 숙소 정보(위치, 테마 등) 컨텍스트 구성                                    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ POST /api/ai/accommodations/naming
                                      │ {
                                      │   images: ["data:image/jpeg;base64,...", ...],
                                      │   language: "ko",
                                      │   context: { city: "서울", themes: ["감성"] }
                                      │ }
                                      │ + Authorization: Bearer {JWT}
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                         [AccommodationAiController]                          │
│                                                                              │
│  1. JWT 토큰 검증 → 호스트 ID 추출                                           │
│  2. 요청 데이터 유효성 검사 (@Valid)                                         │
│  3. AccommodationAiService.suggest() 호출                                    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                          [AccommodationAiService]                            │
│                                                                              │
│  1. 이미지 리스트를 VisionImageAnalyzer로 전달                               │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                          [VisionImageAnalyzer]                               │
│                                                                              │
│  1. 각 이미지 Base64 → byte[] 디코딩                                         │
│  2. 이미지 리사이징 (1024x1024 이하로)                                       │
│  3. Google Cloud Vision API 일괄 호출                                        │
│     - LABEL_DETECTION: 이미지에서 객체/장면 감지                             │
│     - TEXT_DETECTION: OCR로 텍스트 추출                                      │
│  4. 결과 통합 (라벨 중복 제거, 점수순 정렬)                                   │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ ImageAnalysisResult {
                                      │   labels: [Room(95%), Interior(89%), ...],
                                      │   fullText: "WiFi: guest123\n환영합니다"
                                      │ }
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                          [AccommodationAiService]                            │
│                                                                              │
│  2. 프롬프트 생성                                                            │
│     - Vision 라벨: Room(95%), Interior design(89%), ...                     │
│     - OCR 텍스트: WiFi 비밀번호 등                                           │
│     - 숙소 컨텍스트: 서울 강남구, 게스트하우스, 테마: 감성                    │
│                                                                              │
│  3. GeminiTextClient로 프롬프트 전달                                         │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                            [GeminiTextClient]                                │
│                                                                              │
│  1. 시스템 프롬프트 + 사용자 프롬프트 조합                                    │
│  2. Gemini API 호출 (gemini-2.0-flash)                                       │
│     - temperature: 0.5 (안정적 출력)                                         │
│     - responseSchema: {name, description, confidence}                        │
│  3. JSON 응답 파싱                                                           │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ TextCompletionResult {
                                      │   name: "강남 모던 스테이",
                                      │   description: "강남의 중심부에 위치한...",
                                      │   confidence: 0.92,
                                      │   tokenUsage: { prompt: 150, completion: 280 }
                                      │ }
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                          [AccommodationAiService]                            │
│                                                                              │
│  4. 응답 DTO 조합                                                            │
│     - Gemini 결과 (name, description, confidence)                            │
│     - Vision 결과 (labels, visionText)                                       │
│     - 메타데이터 (model, tokenUsage, generatedAt)                            │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                              [Host UI - 프론트엔드]                           │
│                                                                              │
│  5. 응답 수신                                                                │
│  6. form.name = response.name                                                │
│  7. form.description = response.description                                  │
│  8. "AI 추천 결과를 적용했습니다." 모달 표시                                  │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 💰 비용 포인트

### Google Cloud Vision API
| 기능 | 월 무료 | 초과 시 (1,000건당) |
|------|---------|---------------------|
| LABEL_DETECTION | 1,000건 | $1.50 |
| TEXT_DETECTION | 1,000건 | $1.50 |

### Google Gemini API
| 모델 | 무료 티어 | 유료 |
|------|-----------|------|
| gemini-2.0-flash | 15 RPM, 100만 토큰/일 | (2026.01) $0.09 / 1M 입력 토큰 |

**비용 최적화 전략:**
1. Vision API 호출 전 이미지 리사이징 → 처리 속도 향상
2. Gemini의 빠른 모델(flash) 사용 → 응답 시간 단축
3. 프롬프트 최적화 → 토큰 사용량 감소

---

## ⚠️ 주의사항 및 설정

### 필수 환경 설정 (application-secret.yml)

```yaml
# Gemini API 설정
GEMINI_API_KEY: AIzaSy로_시작하는_API_키
GEMINI_MODEL: gemini-2.0-flash
GEMINI_BASE_URL: https://generativelanguage.googleapis.com/v1beta

# Google Cloud Vision 설정
google:
  cloud:
    credentials:
      location: classpath:googlevision/your-credentials.json
```

### Google Cloud 인증 파일 위치
```
backend/src/main/resources/googlevision/your-credentials.json
```

### API 키 발급 방법
1. **Gemini API 키**: [Google AI Studio](https://aistudio.google.com/app/apikey)에서 발급
2. **Vision API**: [Google Cloud Console](https://console.cloud.google.com/)에서 서비스 계정 생성 후 JSON 키 다운로드

### 프론트엔드 요구사항
- 최소 1장 이미지(배너 또는 상세) 업로드 후 AI 버튼 호출
- 이미지가 없으면 "이미지를 먼저 업로드해주세요" 안내

---

## 📊 응답 예시

### Request
```json
{
  "images": [
    "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
    "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
  ],
  "language": "ko",
  "context": {
    "city": "서울특별시",
    "district": "강남구",
    "township": "역삼동",
    "stayType": "게스트하우스",
    "themes": ["감성", "모던"],
    "existingName": "",
    "existingDescription": ""
  }
}
```

### Response
```json
{
  "name": "역삼 모던 스테이",
  "description": "강남의 중심부에 위치한 역삼 모던 스테이는 현대적인 감각과 편안한 휴식을 동시에 제공합니다. 넓은 창문을 통해 들어오는 자연광이 공간 전체를 밝게 비추며, 세련된 인테리어가 특별한 분위기를 연출합니다. 역삼역에서 도보 5분 거리에 위치하여 강남 일대 관광과 비즈니스에 최적의 접근성을 자랑합니다. 무료 와이파이, 에어컨, 개인 욕실 등 필수 편의시설이 완비되어 있으며, 청결한 침구류로 쾌적한 숙면을 보장합니다. 커플 여행객과 비즈니스 출장객 모두에게 추천드리며, 봄에는 근처 선릉의 벚꽃을, 가을에는 단풍을 감상하기 좋습니다. 친절한 호스트가 24시간 문의에 응대해 드립니다. 감성적인 공간에서 특별한 하루를 보내세요.",
  "confidence": 0.92,
  "visionLabels": [
    {"description": "Room", "score": 0.95},
    {"description": "Interior design", "score": 0.89},
    {"description": "Furniture", "score": 0.85},
    {"description": "Wood", "score": 0.82},
    {"description": "Window", "score": 0.78}
  ],
  "visionText": "WELCOME\nWiFi: modern_stay_5G",
  "model": "gemini-2.0-flash",
  "tokenUsage": {
    "prompt": 156,
    "completion": 312,
    "total": 468
  },
  "generatedAt": "2026-01-08T21:30:00+09:00[Asia/Seoul]"
}
```
