package com.ssg9th2team.geharbang.global.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
public class ObjectStorageService {

    // 이미지 최대 크기 설정 (이 크기를 초과하면 자동 리사이징)
    private static final int MAX_IMAGE_WIDTH = 1920;
    private static final int MAX_IMAGE_HEIGHT = 1080;

    // JPEG 압축 품질 (0.0 ~ 1.0)
    private static final float JPEG_QUALITY = 0.85f;

    @Value("${ncloud.storage.endpoint:}")
    private String endpoint;

    @Value("${ncloud.storage.region:}")
    private String region;

    @Value("${ncloud.storage.bucket:}")
    private String bucket;

    @Value("${ncloud.storage.access-key:}")
    private String accessKey;

    @Value("${ncloud.storage.secret-key:}")
    private String secretKey;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        if (endpoint == null || endpoint.isBlank()
                || region == null || region.isBlank()
                || bucket == null || bucket.isBlank()
                || accessKey == null || accessKey.isBlank()
                || secretKey == null || secretKey.isBlank()) {
            log.warn("Ncloud storage config missing; ObjectStorageService disabled.");
            return;
        }
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        log.info("Naver Cloud Object Storage initialized - bucket: {}", bucket);
    }

    /**
     * Base64 이미지를 업로드하고 공개 URL 반환
     */
    public String uploadBase64Image(String base64Image, String folder) {
        if (base64Image == null || base64Image.isEmpty()) {
            return null;
        }
        if (s3Client == null) {
            log.warn("Object storage not configured; returning original image payload.");
            return base64Image;
        }

        // 이미 URL인 경우 그대로 반환
        if (base64Image.startsWith("http")) {
            return base64Image;
        }

        try {
            // Base64 파싱
            String[] parts = base64Image.split(",");
            String header = parts[0];
            String data = parts.length > 1 ? parts[1] : parts[0];

            log.info("Base64 header: {}", header);

            byte[] imageBytes = Base64.getDecoder().decode(data);
            int originalSize = imageBytes.length;

            // 이미지 리사이징 적용
            imageBytes = resizeImageIfNeeded(imageBytes);
            log.info("Image size: {} bytes -> {} bytes ({}% 감소)",
                    originalSize, imageBytes.length,
                    Math.round((1 - (double) imageBytes.length / originalSize) * 100));

            // 리사이징 후에는 항상 jpg로 저장 (용량 최적화)
            String extension = "jpg";
            String contentType = "image/jpeg";

            // 파일명 생성
            String fileName = folder + "/" + UUID.randomUUID() + "." + extension;

            // S3에 업로드
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(contentType)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            log.info("Uploading image: bucket={}, key={}, size={} bytes", bucket, fileName, imageBytes.length);
            PutObjectResponse response = s3Client.putObject(putRequest, RequestBody.fromBytes(imageBytes));
            log.info("Upload response: ETag={}", response.eTag());

            // 공개 URL 반환
            String publicUrl = endpoint + "/" + bucket + "/" + fileName;
            log.info("Image uploaded successfully: {}", publicUrl);

            return publicUrl;

        } catch (Exception e) {
            log.error("Failed to upload image", e);
            throw new RuntimeException("이미지 업로드 실패: " + e.getMessage());
        }
    }

    /**
     * 이미지가 최대 크기를 초과하면 리사이징
     * - 비율 유지하면서 축소
     * - 고품질 보간법 사용
     * - JPEG로 압축하여 용량 최적화
     */
    private byte[] resizeImageIfNeeded(byte[] imageBytes) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            BufferedImage originalImage = ImageIO.read(inputStream);

            if (originalImage == null) {
                log.warn("이미지를 읽을 수 없습니다. 원본 그대로 사용합니다.");
                return imageBytes;
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // 리사이징이 필요한지 확인
            if (originalWidth <= MAX_IMAGE_WIDTH && originalHeight <= MAX_IMAGE_HEIGHT) {
                // 크기는 괜찮지만 JPEG 압축은 적용
                return compressToJpeg(originalImage);
            }

            // 비율 유지하면서 새 크기 계산
            double scaleX = (double) MAX_IMAGE_WIDTH / originalWidth;
            double scaleY = (double) MAX_IMAGE_HEIGHT / originalHeight;
            double scale = Math.min(scaleX, scaleY);

            int newWidth = (int) (originalWidth * scale);
            int newHeight = (int) (originalHeight * scale);

            // 고품질 리사이징
            BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resizedImage.createGraphics();

            // 고품질 렌더링 설정
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 흰색 배경 (투명 PNG 처리용)
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, newWidth, newHeight);

            // 이미지 그리기
            g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g2d.dispose();

            log.info("이미지 리사이징: {}x{} -> {}x{}", originalWidth, originalHeight, newWidth, newHeight);

            return compressToJpeg(resizedImage);

        } catch (Exception e) {
            log.warn("이미지 리사이징 실패, 원본 사용: {}", e.getMessage());
            return imageBytes;
        }
    }

    /**
     * BufferedImage를 JPEG로 압축
     */
    private byte[] compressToJpeg(BufferedImage image) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // JPEG 압축 품질 설정
        javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);

        writer.setOutput(ImageIO.createImageOutputStream(outputStream));
        writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
        writer.dispose();

        return outputStream.toByteArray();
    }

    /**
     * 이미지 삭제
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || !imageUrl.contains(bucket)) {
            return;
        }
        if (s3Client == null) {
            return;
        }

        try {
            // URL에서 key 추출
            String key = imageUrl.substring(imageUrl.indexOf(bucket + "/") + bucket.length() + 1);

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.debug("Image deleted: {}", imageUrl);

        } catch (Exception e) {
            log.error("Failed to delete image: {}", imageUrl, e);
        }
    }
}
