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
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;

import java.net.URI;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
public class ObjectStorageService {

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

            byte[] imageBytes = Base64.getDecoder().decode(data);

            // 확장자 결정
            String extension = "jpg";
            if (header.contains("png")) {
                extension = "png";
            } else if (header.contains("gif")) {
                extension = "gif";
            } else if (header.contains("webp")) {
                extension = "webp";
            }

            // 파일명 생성
            String fileName = folder + "/" + UUID.randomUUID() + "." + extension;

            // Content-Type 결정
            String contentType = "image/" + extension;

            // S3에 업로드
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(contentType)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(imageBytes));

            // 공개 URL 반환
            String publicUrl = endpoint + "/" + bucket + "/" + fileName;
            log.debug("Image uploaded: {}", publicUrl);

            return publicUrl;

        } catch (Exception e) {
            log.error("Failed to upload image", e);
            throw new RuntimeException("이미지 업로드 실패: " + e.getMessage());
        }
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
