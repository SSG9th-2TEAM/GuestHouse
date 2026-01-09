# 전체 이미지 업로드 자동 리사이징 기능

## 📋 기능 개요

프로젝트에서 업로드되는 **모든 이미지**를 자동으로 리사이징하고 압축하여 스토리지 용량을 절약하고 로딩 속도를 개선하는 기능입니다.  
2026년 1월부터는 숙소 등록/수정 화면에서 **AI 추천 요청 전에 프론트엔드에서도 1024px 이하로 축소**하여 전송합니다.

### 왜 이 기능을 만들었나?

**문제 상황:**
- 사용자들이 휴대폰으로 찍은 원본 이미지(5~10MB)를 그대로 업로드
- NCloud Object Storage 용량 빠르게 증가 → 비용 증가
- 웹페이지에서 이미지 로딩 시간 지연 → 사용자 경험 저하
- 대용량 이미지가 필요 없는 웹 환경에서 불필요한 리소스 낭비

**해결 방안:**
- 업로드 시점에서 자동으로 리사이징
- 웹에 적합한 크기(1920x1080)로 축소
- JPEG 압축으로 용량 최적화
- 원본 비율 유지로 이미지 왜곡 방지

---

## 🎯 적용 범위

`ObjectStorageService`는 프로젝트의 **모든 이미지 업로드**를 담당하는 중앙 서비스입니다.

### 이 기능이 적용되는 곳
| 기능 | 백엔드 리사이징 | 프론트 리사이징 |
|------|----------------|------------------|
| 숙소 등록/수정 이미지 업로드 | ✅ (ObjectStorageService) | ✅ (AI 추천용 Base64 전송) |
| 객실 등록/수정 | ✅ | ❌ |
| 리뷰 첨부 이미지 | ✅ | ❌ |
| 프로필 이미지 | ✅ | ❌ |
| AI 추천 API (`/ai/accommodations/naming`) | ✅ (VisionImageAnalyzer) | ✅ (캔버스로 1024px) |
| 기타 업로드 | ✅ | 필요 시 확장 |

**장점:** 한 곳만 수정하면 전체 시스템에 적용됨

> **추가 이점**: 프론트에서 미리 축소하면 AI 추천 요청 페이로드가 80% 이상 줄어들어 사용자 경험이 개선됩니다.

---

## 🔄 처리 흐름

### 프론트엔드 (AI 추천용)

```
[사용자 업로드]
    │ (File 객체)
    ▼
[canvas 리사이징: 최대 1024px, JPEG 0.85]
    │
    ├─ HostAccommodationRegister.vue → images[]
    └─ HostAccommodationEdit.vue → images[]
```

- 캔버스에서 축소한 Base64는 **AI 추천 API**로만 전송되고, 실제 업로드는 원본 파일을 유지합니다.
- 이미지가 5장 이상이면 상위 5장을 사용하도록 제한(네트워크 안정성).

### 백엔드 업로드 플로우

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        [사용자 - 프론트엔드]                             │
│                                                                          │
│  1. 파일 선택 (input type="file")                                        │
│  2. JavaScript에서 Base64로 인코딩                                       │
│  3. API 요청에 Base64 문자열 포함                                        │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
                                   │  "data:image/jpeg;base64,/9j/4AAQ..."
                                   │  (원본: 4000x3000, 5MB)
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        [백엔드 - Service Layer]                          │
│                                                                          │
│  예: AccommodationServiceImpl, ReviewServiceImpl 등                      │
│                                                                          │
│  objectStorageService.uploadBase64Image(base64Image, "accommodations")  │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      [ObjectStorageService]                              │
│                                                                          │
│  uploadBase64Image(String base64Image, String folder) 메서드             │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         [1단계: Base64 디코딩]                           │
│                                                                          │
│  // "data:image/jpeg;base64,/9j/4AAQ..." 파싱                            │
│  String[] parts = base64Image.split(",");                                │
│  String header = parts[0];    // "data:image/jpeg;base64"                │
│  String data = parts[1];      // "/9j/4AAQ..."                           │
│                                                                          │
│  byte[] imageBytes = Base64.getDecoder().decode(data);                   │
│  // 결과: 5,242,880 bytes (5MB)                                          │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      [2단계: 리사이징 필요 여부 확인]                     │
│                                                                          │
│  resizeImageIfNeeded(imageBytes) 호출                                    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  BufferedImage originalImage = ImageIO.read(inputStream);       │    │
│  │                                                                  │    │
│  │  int originalWidth = originalImage.getWidth();   // 4000        │    │
│  │  int originalHeight = originalImage.getHeight(); // 3000        │    │
│  │                                                                  │    │
│  │  // 최대 크기: 1920 x 1080                                       │    │
│  │  if (originalWidth <= 1920 && originalHeight <= 1080) {         │    │
│  │      return compressToJpeg(originalImage);  // 압축만            │    │
│  │  }                                                               │    │
│  │  // → 4000 > 1920 이므로 리사이징 필요!                          │    │
│  └─────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       [3단계: 새 크기 계산]                              │
│                                                                          │
│  // 비율 유지하면서 최대 크기에 맞추기                                    │
│                                                                          │
│  double scaleX = 1920.0 / 4000 = 0.48                                    │
│  double scaleY = 1080.0 / 3000 = 0.36                                    │
│  double scale = Math.min(0.48, 0.36) = 0.36  // 더 작은 비율 선택        │
│                                                                          │
│  int newWidth = 4000 * 0.36 = 1440                                       │
│  int newHeight = 3000 * 0.36 = 1080                                      │
│                                                                          │
│  // 결과: 4000x3000 → 1440x1080 (비율 4:3 유지)                          │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        [4단계: 고품질 리사이징]                          │
│                                                                          │
│  BufferedImage resizedImage = new BufferedImage(                         │
│      newWidth, newHeight, BufferedImage.TYPE_INT_RGB                     │
│  );                                                                      │
│                                                                          │
│  Graphics2D g2d = resizedImage.createGraphics();                         │
│                                                                          │
│  // ⭐ 핵심: 고품질 렌더링 힌트 설정                                      │
│  g2d.setRenderingHint(                                                   │
│      RenderingHints.KEY_INTERPOLATION,                                   │
│      RenderingHints.VALUE_INTERPOLATION_BICUBIC  // 바이큐빅 보간법      │
│  );                                                                      │
│  g2d.setRenderingHint(                                                   │
│      RenderingHints.KEY_RENDERING,                                       │
│      RenderingHints.VALUE_RENDER_QUALITY  // 품질 우선                   │
│  );                                                                      │
│  g2d.setRenderingHint(                                                   │
│      RenderingHints.KEY_ANTIALIASING,                                    │
│      RenderingHints.VALUE_ANTIALIAS_ON  // 안티앨리어싱                   │
│  );                                                                      │
│                                                                          │
│  // 흰색 배경 (PNG 투명 배경 처리)                                        │
│  g2d.setColor(Color.WHITE);                                              │
│  g2d.fillRect(0, 0, newWidth, newHeight);                                │
│                                                                          │
│  // 이미지 그리기                                                         │
│  g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);          │
│  g2d.dispose();                                                          │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        [5단계: JPEG 압축]                                │
│                                                                          │
│  compressToJpeg(resizedImage) 호출                                       │
│                                                                          │
│  // JPEG Writer 설정                                                     │
│  ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next(); │
│  ImageWriteParam param = writer.getDefaultWriteParam();                  │
│                                                                          │
│  // 압축 품질: 85% (0.0 ~ 1.0)                                           │
│  param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);                │
│  param.setCompressionQuality(0.85f);                                     │
│                                                                          │
│  // 바이트 배열로 출력                                                    │
│  ByteArrayOutputStream outputStream = new ByteArrayOutputStream();       │
│  writer.setOutput(ImageIO.createImageOutputStream(outputStream));        │
│  writer.write(null, new IIOImage(resizedImage, null, null), param);      │
│                                                                          │
│  // 결과: 5MB → 약 200KB (96% 감소!)                                     │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      [6단계: NCloud 업로드]                              │
│                                                                          │
│  String fileName = "accommodations/" + UUID.randomUUID() + ".jpg";       │
│                                                                          │
│  PutObjectRequest putRequest = PutObjectRequest.builder()                │
│      .bucket(bucket)                                                     │
│      .key(fileName)                                                      │
│      .contentType("image/jpeg")                                          │
│      .acl(ObjectCannedACL.PUBLIC_READ)  // 공개 읽기                     │
│      .build();                                                           │
│                                                                          │
│  s3Client.putObject(putRequest, RequestBody.fromBytes(imageBytes));      │
│                                                                          │
│  return endpoint + "/" + bucket + "/" + fileName;                        │
│  // "https://kr.object.ncloudstorage.com/guesthouse/accommodations/      │
│  //  550e8400-e29b-41d4-a716-446655440000.jpg"                           │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        [결과 반환]                                       │
│                                                                          │
│  로그 출력:                                                              │
│  "Image size: 5242880 bytes -> 204800 bytes (96% 감소)"                  │
│  "이미지 리사이징: 4000x3000 -> 1440x1080"                               │
│  "Image uploaded successfully: https://..."                              │
│                                                                          │
│  서비스로 URL 반환 → DB에 저장                                           │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 💻 코드 상세 설명

### ObjectStorageService.java 전체 구조

```java
@Slf4j
@Service
public class ObjectStorageService {

    // ========================================
    // 설정 상수
    // ========================================

    // 이미지 최대 크기 (이 크기 초과 시 리사이징)
    private static final int MAX_IMAGE_WIDTH = 1920;   // Full HD 가로
    private static final int MAX_IMAGE_HEIGHT = 1080;  // Full HD 세로

    // JPEG 압축 품질 (0.0 ~ 1.0)
    // 0.85 = 85% 품질 (육안으로 구분 어려움, 용량 크게 감소)
    private static final float JPEG_QUALITY = 0.85f;

    // ========================================
    // NCloud 설정 (application.properties에서 주입)
    // ========================================

    @Value("${ncloud.storage.endpoint:}")
    private String endpoint;  // https://kr.object.ncloudstorage.com

    @Value("${ncloud.storage.region:}")
    private String region;    // kr-standard

    @Value("${ncloud.storage.bucket:}")
    private String bucket;    // guesthouse

    @Value("${ncloud.storage.access-key:}")
    private String accessKey;

    @Value("${ncloud.storage.secret-key:}")
    private String secretKey;

    private S3Client s3Client;  // AWS S3 호환 클라이언트

    // ... 메서드들
}
```

### 왜 이런 설정값을 선택했나?

#### MAX_IMAGE_WIDTH / HEIGHT = 1920 x 1080

```
┌─────────────────────────────────────────────────────────────┐
│                    해상도 비교                               │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  원본 이미지 (4K)     : 3840 x 2160  →  과도함              │
│  Full HD (선택)       : 1920 x 1080  →  웹에 적합           │
│  HD                   : 1280 x 720   →  너무 작음           │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 웹사이트 일반적인 이미지 표시 영역                    │    │
│  │                                                       │    │
│  │  • 숙소 상세 페이지 메인 이미지: 약 800~1200px 폭     │    │
│  │  • 리스트 썸네일: 약 300~400px 폭                     │    │
│  │  • 모바일: 약 375~428px 폭                            │    │
│  │                                                       │    │
│  │  → 1920px면 어떤 화면에서도 충분!                     │    │
│  │  → Retina 디스플레이(2x)까지 대응 가능                │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

#### JPEG_QUALITY = 0.85 (85%)

```
┌─────────────────────────────────────────────────────────────┐
│                    JPEG 품질별 비교                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  품질 100% : 거의 무손실, 용량 크게 감소 안 됨              │
│  품질 95%  : 미세한 차이, 용량 약간 감소                    │
│  품질 85%  : 육안 구분 어려움, 용량 크게 감소 ← 선택!       │
│  품질 70%  : 약간의 품질 저하 보임                          │
│  품질 50%  : 뚜렷한 품질 저하                               │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  85%를 선택한 이유:                                   │    │
│  │                                                       │    │
│  │  1. 일반 사용자가 원본과 구분 불가능                  │    │
│  │  2. 용량 대폭 감소 (원본 대비 70~90% 감소)            │    │
│  │  3. 업계 표준 (대부분의 웹사이트가 80~90% 사용)       │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

### resizeImageIfNeeded() 메서드 상세

```java
/**
 * 이미지가 최대 크기를 초과하면 리사이징
 *
 * @param imageBytes 원본 이미지 바이트 배열
 * @return 리사이징된 이미지 (또는 압축만 된 원본)
 */
private byte[] resizeImageIfNeeded(byte[] imageBytes) {
    try {
        // ========================================
        // 1단계: 바이트 배열을 BufferedImage로 변환
        // ========================================
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
        BufferedImage originalImage = ImageIO.read(inputStream);

        // ImageIO가 읽을 수 없는 형식인 경우 (예: 손상된 파일)
        if (originalImage == null) {
            log.warn("이미지를 읽을 수 없습니다. 원본 그대로 사용합니다.");
            return imageBytes;  // 안전하게 원본 반환
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // ========================================
        // 2단계: 리사이징 필요 여부 판단
        // ========================================
        if (originalWidth <= MAX_IMAGE_WIDTH && originalHeight <= MAX_IMAGE_HEIGHT) {
            // 이미 충분히 작음 → 리사이징 불필요
            // 하지만 JPEG 압축은 적용 (PNG → JPEG 변환 등)
            return compressToJpeg(originalImage);
        }

        // ========================================
        // 3단계: 새 크기 계산 (비율 유지)
        // ========================================

        // 가로/세로 각각의 축소 비율 계산
        double scaleX = (double) MAX_IMAGE_WIDTH / originalWidth;
        double scaleY = (double) MAX_IMAGE_HEIGHT / originalHeight;

        // 더 작은 비율을 선택 → 두 축 모두 최대 크기 이내로
        double scale = Math.min(scaleX, scaleY);

        /*
         * 예시: 4000 x 3000 이미지
         *
         * scaleX = 1920 / 4000 = 0.48
         * scaleY = 1080 / 3000 = 0.36
         * scale = min(0.48, 0.36) = 0.36
         *
         * newWidth = 4000 * 0.36 = 1440
         * newHeight = 3000 * 0.36 = 1080
         *
         * 결과: 1440 x 1080 (높이가 딱 맞고, 너비는 여유 있음)
         */

        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        // ========================================
        // 4단계: 새 BufferedImage 생성 및 리사이징
        // ========================================

        // TYPE_INT_RGB: 투명도 없는 RGB (JPEG는 투명도 미지원)
        BufferedImage resizedImage = new BufferedImage(
            newWidth, newHeight, BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g2d = resizedImage.createGraphics();

        /*
         * ⭐ 고품질 렌더링 힌트 (핵심!)
         *
         * 이 설정들이 없으면 이미지가 깨져 보일 수 있음
         */

        // BICUBIC: 가장 부드러운 보간법
        // - NEAREST_NEIGHBOR: 빠르지만 계단현상 발생
        // - BILINEAR: 중간 품질
        // - BICUBIC: 가장 부드럽고 자연스러움 ← 선택
        g2d.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );

        // 렌더링 품질 우선
        g2d.setRenderingHint(
            RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY
        );

        // 안티앨리어싱: 가장자리 부드럽게
        g2d.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        // ========================================
        // 5단계: 흰색 배경 채우기
        // ========================================

        /*
         * PNG는 투명 배경을 가질 수 있지만,
         * JPEG는 투명도를 지원하지 않음
         *
         * 투명 영역을 흰색으로 채워서 검은색으로 변하는 것 방지
         */
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, newWidth, newHeight);

        // ========================================
        // 6단계: 이미지 그리기
        // ========================================
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();  // 리소스 해제

        log.info("이미지 리사이징: {}x{} -> {}x{}",
            originalWidth, originalHeight, newWidth, newHeight);

        // ========================================
        // 7단계: JPEG 압축
        // ========================================
        return compressToJpeg(resizedImage);

    } catch (Exception e) {
        // 어떤 오류가 발생해도 서비스 중단 방지
        log.warn("이미지 리사이징 실패, 원본 사용: {}", e.getMessage());
        return imageBytes;  // 안전하게 원본 반환
    }
}
```

---

### compressToJpeg() 메서드 상세

```java
/**
 * BufferedImage를 JPEG 형식으로 압축
 *
 * 왜 모든 이미지를 JPEG로 변환하나?
 * - JPEG는 사진에 최적화된 손실 압축 포맷
 * - PNG보다 10~50배 작은 용량
 * - 웹 브라우저 호환성 100%
 *
 * @param image 압축할 이미지
 * @return JPEG 바이트 배열
 */
private byte[] compressToJpeg(BufferedImage image) throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // JPEG Writer 가져오기
    ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();

    // 압축 파라미터 설정
    ImageWriteParam param = writer.getDefaultWriteParam();
    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);  // 명시적 압축 모드
    param.setCompressionQuality(JPEG_QUALITY);  // 0.85 = 85%

    // 출력 스트림 설정
    writer.setOutput(ImageIO.createImageOutputStream(outputStream));

    // 이미지 쓰기
    writer.write(
        null,
        new IIOImage(image, null, null),
        param
    );

    writer.dispose();  // 리소스 해제

    return outputStream.toByteArray();
}
```

---

## ⚙️ 설정값 요약

```
┌──────────────────────┬────────────┬─────────────────────────────────────┐
│        설정          │     값     │               설명                  │
├──────────────────────┼────────────┼─────────────────────────────────────┤
│ MAX_IMAGE_WIDTH      │ 1920px     │ 최대 가로 크기 (Full HD)            │
├──────────────────────┼────────────┼─────────────────────────────────────┤
│ MAX_IMAGE_HEIGHT     │ 1080px     │ 최대 세로 크기 (Full HD)            │
├──────────────────────┼────────────┼─────────────────────────────────────┤
│ JPEG_QUALITY         │ 0.85 (85%) │ 압축 품질 (육안 구분 어려움)        │
├──────────────────────┼────────────┼─────────────────────────────────────┤
│ 보간법               │ BICUBIC    │ 가장 부드러운 리사이징 알고리즘     │
├──────────────────────┼────────────┼─────────────────────────────────────┤
│ 출력 포맷            │ JPEG       │ 모든 이미지를 JPEG로 통일           │
└──────────────────────┴────────────┴─────────────────────────────────────┘
```

---

## 🎯 핵심 기능 상세

### 1. 비율 유지 리사이징

```
원본: 4000 x 3000 (4:3 비율)
         │
         ▼
┌─────────────────────────────────────────────────────────┐
│  비율 계산:                                              │
│                                                          │
│  scaleX = 1920 / 4000 = 0.48                            │
│  scaleY = 1080 / 3000 = 0.36                            │
│                                                          │
│  scale = min(0.48, 0.36) = 0.36  ← 더 작은 비율 선택    │
│                                                          │
│  newWidth = 4000 × 0.36 = 1440                          │
│  newHeight = 3000 × 0.36 = 1080                         │
│                                                          │
│  결과: 1440 x 1080 (여전히 4:3 비율!)                   │
└─────────────────────────────────────────────────────────┘

왜 min()을 사용하나?
- 두 축 모두 최대 크기를 초과하지 않도록 보장
- 더 작은 비율을 선택하면 가로/세로 모두 범위 내로 들어옴
```

### 2. 고품질 렌더링 (BICUBIC 보간법)

```
┌─────────────────────────────────────────────────────────┐
│                    보간법 비교                           │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  NEAREST_NEIGHBOR (최근접 이웃)                         │
│  ├─ 가장 빠름                                           │
│  ├─ 품질 최하                                           │
│  └─ 계단 현상 (픽셀이 깨져 보임)                        │
│                                                          │
│  BILINEAR (이중 선형)                                   │
│  ├─ 중간 속도                                           │
│  ├─ 중간 품질                                           │
│  └─ 약간의 흐림 현상                                    │
│                                                          │
│  BICUBIC (이중 삼차) ← 선택!                            │
│  ├─ 가장 느림 (하지만 여전히 빠름)                      │
│  ├─ 최고 품질                                           │
│  └─ 가장 자연스러운 결과                                │
│                                                          │
│  ┌───────────────────────────────────────────────┐      │
│  │  BICUBIC은 주변 16개 픽셀을 분석하여           │      │
│  │  새 픽셀 값을 계산함                           │      │
│  │  → 부드럽고 자연스러운 축소 결과               │      │
│  └───────────────────────────────────────────────┘      │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### 3. JPEG 압축

```
┌─────────────────────────────────────────────────────────┐
│                    포맷별 특성                           │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  PNG:                                                    │
│  ├─ 무손실 압축                                         │
│  ├─ 투명도 지원                                         │
│  ├─ 사진에는 용량이 큼 (수 MB)                          │
│  └─ 로고/아이콘에 적합                                  │
│                                                          │
│  JPEG: ← 선택!                                          │
│  ├─ 손실 압축 (품질 조절 가능)                          │
│  ├─ 투명도 미지원                                       │
│  ├─ 사진에 최적화 (수백 KB)                             │
│  └─ 숙소 이미지에 적합                                  │
│                                                          │
│  WebP:                                                   │
│  ├─ 차세대 포맷                                         │
│  ├─ JPEG보다 30% 작음                                   │
│  └─ 일부 구형 브라우저 미지원                           │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### 4. 투명 배경 처리

```
문제 상황:
┌─────────────────────────────────────────────────────────┐
│                                                          │
│  PNG 이미지에 투명 배경이 있을 때:                       │
│                                                          │
│  ┌─────────────┐      변환      ┌─────────────┐         │
│  │  🏠         │  ──────────▶  │  🏠         │         │
│  │  (투명)     │    JPEG       │  (검은색)    │  ← 문제!│
│  └─────────────┘               └─────────────┘         │
│                                                          │
│  JPEG는 투명도를 지원하지 않아서                        │
│  투명 영역이 검은색으로 변함                             │
│                                                          │
└─────────────────────────────────────────────────────────┘

해결:
┌─────────────────────────────────────────────────────────┐
│                                                          │
│  변환 전에 흰색 배경을 먼저 채움:                        │
│                                                          │
│  g2d.setColor(Color.WHITE);                              │
│  g2d.fillRect(0, 0, width, height);                      │
│  g2d.drawImage(original, ...);                           │
│                                                          │
│  ┌─────────────┐      변환      ┌─────────────┐         │
│  │  🏠         │  ──────────▶  │  🏠         │         │
│  │  (투명)     │    JPEG       │  (흰색)     │  ← 해결!│
│  └─────────────┘               └─────────────┘         │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## 📊 효과 측정

### 실제 테스트 결과

```
┌───────────────────────┬───────────┬───────────┬───────────┐
│         상황          │   원본    │  변환 후  │   감소율  │
├───────────────────────┼───────────┼───────────┼───────────┤
│ 5MB PNG (4000x3000)   │ 5,000 KB  │  ~200 KB  │   96%     │
├───────────────────────┼───────────┼───────────┼───────────┤
│ 3MB JPEG (3000x2000)  │ 3,000 KB  │  ~250 KB  │   92%     │
├───────────────────────┼───────────┼───────────┼───────────┤
│ 2MB JPEG (2500x1500)  │ 2,000 KB  │  ~180 KB  │   91%     │
├───────────────────────┼───────────┼───────────┼───────────┤
│ 800KB (1200x800)      │   800 KB  │  ~150 KB  │   81%     │
├───────────────────────┼───────────┼───────────┼───────────┤
│ 작은 이미지 (800x600) │   400 KB  │  ~100 KB  │   75%     │
│ (리사이징 없이 압축만)│           │           │           │
└───────────────────────┴───────────┴───────────┴───────────┘
```

### 서버 로그 예시

```
INFO  - Base64 header: data:image/png;base64
INFO  - Image size: 5242880 bytes -> 204800 bytes (96% 감소)
INFO  - 이미지 리사이징: 4000x3000 -> 1440x1080
INFO  - Uploading image: bucket=guesthouse, key=accommodations/550e8400-e29b-41d4-a716-446655440000.jpg, size=204800 bytes
INFO  - Upload response: ETag="abc123..."
INFO  - Image uploaded successfully: https://kr.object.ncloudstorage.com/guesthouse/accommodations/550e8400-e29b-41d4-a716-446655440000.jpg
```

---

## 💰 비용 절감 효과

### NCloud Object Storage 요금

| 항목 | 요금 |
|------|------|
| 저장 용량 | 약 33원/GB/월 |
| 다운로드 트래픽 | 약 110원/GB |

### 월간 예상 절감액 (숙소 100개, 이미지 평균 5장 기준)

```
리사이징 적용 전:
- 숙소당 평균 이미지 용량: 5MB × 5장 = 25MB
- 총 저장 용량: 100 × 25MB = 2.5GB
- 월 저장 비용: 2.5GB × 33원 = 약 83원

리사이징 적용 후:
- 숙소당 평균 이미지 용량: 200KB × 5장 = 1MB
- 총 저장 용량: 100 × 1MB = 100MB (0.1GB)
- 월 저장 비용: 0.1GB × 33원 = 약 3원

절감액: 약 80원/월 (96% 절감)

※ 트래픽 비용까지 포함하면 절감 효과 더 큼
※ 숙소 수 증가 시 절감액 비례 증가
```

---

## ⚠️ 주의사항

### 1. 서버 메모리 사용
```
대용량 이미지 처리 시 메모리 사용량 증가 가능
- BufferedImage는 비압축 상태로 메모리에 로드됨
- 4000x3000 RGB 이미지 = 약 36MB 메모리 사용
- 동시에 많은 이미지 업로드 시 OOM 가능성

대응:
- JVM 힙 메모리 충분히 설정 (-Xmx2g 이상 권장)
- 필요시 이미지 크기 제한 추가
```

### 2. 무손실 원본이 필요한 경우
```
현재 구현은 모든 이미지를 JPEG로 변환
- 원본 보존이 필요한 경우 별도 저장 로직 필요
- 현재 프로젝트에서는 웹 표시용으로 충분
```

### 3. 지원 포맷
```
ImageIO가 지원하는 포맷:
- JPEG, PNG, GIF, BMP, WBMP

지원하지 않는 포맷:
- WebP (별도 라이브러리 필요)
- HEIC (iPhone 기본 포맷, 별도 라이브러리 필요)
- RAW 포맷들

대응:
- 프론트엔드에서 파일 확장자 검증
- 지원하지 않는 포맷은 에러 메시지 표시
```
