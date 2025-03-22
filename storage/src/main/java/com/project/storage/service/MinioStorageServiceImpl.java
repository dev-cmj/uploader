package com.project.storage.service;

import com.project.common.model.ContentMessage;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageServiceImpl implements StorageService {

    private final MinioClient minioClient;
    private final String bucketName;

    @Value("${minio.presigned-url-expiry:7}") // 기본 7일
    private int presignedUrlExpiryDays;

    @Override
    public String storeFile(ContentMessage message ) {
        try {
            // 콘텐츠 ID와 확장자로 고유한 객체 이름 생성
            String objectName = generateObjectName(message);

            // 콘텐츠 데이터 얻기
            byte[] contentBytes;
            if (message.getContentData() != null) {
                // 메시지에 직접 포함된 데이터 사용
                contentBytes = message.getContentData();
            } else if (message.getSourcePath() != null || message.getTempFilePath() != null) {
                // 파일 경로에서 데이터 로드
                String filePath = message.getSourcePath() != null ?
                        message.getSourcePath() : message.getTempFilePath();
                contentBytes = Files.readAllBytes(Paths.get(filePath));
            } else {
                throw new IllegalArgumentException("No content data or file path found in message");
            }

            // 콘텐츠 타입 결정
            String contentType = determineContentType(message);

            // 객체 메타데이터 설정
            Map<String, String> userMetadata = new HashMap<>();
            userMetadata.put("Content-Type", contentType);
            userMetadata.put("X-Content-Id", message.getId());
            userMetadata.put("X-User-Id", message.getUserId());
            userMetadata.put("X-Original-Filename", message.getFileName());

            // MinIO에 업로드
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(contentBytes), contentBytes.length, -1)
                            .contentType(contentType)
                            .userMetadata(userMetadata)
                            .build()
            );

            // 접근 URL 생성 (presigned URL)
            String accessUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .method(Method.GET)
                            .expiry(presignedUrlExpiryDays, TimeUnit.DAYS)
                            .build()
            );

            log.info("File stored in MinIO: bucket={}, object={}", bucketName, objectName);

            // 임시 파일 정리 (필요한 경우)
            cleanupTempFile(message);

            return accessUrl;

        } catch (Exception e) {
            log.error("Failed to store file in MinIO for content: {}", message.getId(), e);
            return null;
        }
    }

    @Override
    public String storeProcessedFile(ContentMessage message) {
        try {
            if (message.getProcessedPath() == null) {
                throw new IllegalArgumentException("Processed file path is missing");
            }

            // 파일 확장자를 포함한 객체 이름 생성
            Path processedFilePath = Paths.get(message.getProcessedPath());
            String fileName = processedFilePath.getFileName().toString();
            String extension = fileName.lastIndexOf(".") > 0 ?
                    fileName.substring(fileName.lastIndexOf(".")) : "";

            // 고유한 객체 이름 생성
            String objectName = String.format("processed/%s/%s%s",
                    message.getUserId(), UUID.randomUUID().toString(), extension);

            // 파일 데이터 로드
            byte[] fileContent = Files.readAllBytes(processedFilePath);

            // 콘텐츠 타입 결정
            String contentType = determineContentType(message);

            // 객체 메타데이터 설정
            Map<String, String> userMetadata = new HashMap<>();
            userMetadata.put("Content-Type", contentType);
            userMetadata.put("X-Content-Id", message.getId());
            userMetadata.put("X-User-Id", message.getUserId());
            userMetadata.put("X-Original-Filename", message.getFileName());
            userMetadata.put("X-Is-Processed", "true");

            // MinIO에 업로드
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(fileContent), fileContent.length, -1)
                            .contentType(contentType)
                            .userMetadata(userMetadata)
                            .build()
            );

            // 접근 URL 생성
            String accessUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .method(Method.GET)
                            .expiry(presignedUrlExpiryDays, TimeUnit.DAYS)
                            .build()
            );

            log.info("Processed file stored in MinIO: bucket={}, object={}", bucketName, objectName);

            // 임시 파일 정리
            cleanupTempFile(message);

            return accessUrl;

        } catch (Exception e) {
            log.error("Failed to store processed file in MinIO for content: {}", message.getId(), e);
            return null;
        }
    }

    /**
     * 객체의 고유한 이름을 생성합니다.
     */
    private String generateObjectName(ContentMessage message) {
        String extension = getFileExtension(message);
        return String.format("original/%s/%s%s",
                message.getUserId(), UUID.randomUUID().toString(), extension);
    }

    /**
     * 메시지로부터 파일 확장자를 결정합니다.
     */
    private String getFileExtension(ContentMessage message) {
        // 파일명에서 확장자 추출 시도
        if (message.getFileName() != null && message.getFileName().lastIndexOf(".") > 0) {
            return message.getFileName().substring(message.getFileName().lastIndexOf("."));
        }

        // 콘텐츠 타입으로 확장자 결정
        if (message.getContentType() == null) {
            return "";
        }

        return switch (message.getContentType().toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "video/mp4" -> ".mp4";
            case "video/mpeg" -> ".mpeg";
            case "video/webm" -> ".webm";
            case "audio/mpeg" -> ".mp3";
            case "audio/wav" -> ".wav";
            case "audio/ogg" -> ".ogg";
            case "application/pdf" -> ".pdf";
            case "text/plain" -> ".txt";
            case "text/html" -> ".html";
            case "application/json" -> ".json";
            case "application/xml" -> ".xml";
            default -> "";
        };
    }

    /**
     * 메시지로부터 콘텐츠 타입을 결정합니다.
     */
    private String determineContentType(ContentMessage message) {
        return message.getContentType() != null ?
                message.getContentType() : "application/octet-stream";
    }

    /**
     * 임시 파일을 정리합니다.
     */
    private void cleanupTempFile(ContentMessage message) {
        try {
            // 임시 파일 정리
            if (message.getTempFilePath() != null) {
                File tempFile = new File(message.getTempFilePath());
                if (tempFile.exists() && tempFile.delete()) {
                    log.debug("Temporary file deleted: {}", message.getTempFilePath());
                }
            }

            // 처리된 파일 정리 (선택적)
            if (message.getProcessedPath() != null) {
                File processedFile = new File(message.getProcessedPath());
                if (processedFile.exists() && processedFile.delete()) {
                    log.debug("Processed file deleted: {}", message.getProcessedPath());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to clean up temporary files for content: {}", message.getId(), e);
        }
    }
}