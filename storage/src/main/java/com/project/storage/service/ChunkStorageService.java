package com.project.storage.service;

import com.project.common.constants.RabbitMQConstants;
import com.project.common.model.ContentMessage;
import com.project.common.model.ContentStatus;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChunkStorageService {

    private final MinioClient minioClient;
    private final RabbitTemplate rabbitTemplate;
    private final String bucketName;
    private final String tempBucketName;

    // 청크 추적 맵 (contentId -> 수신된 청크 수)
    private final Map<String, Integer> chunkTracker = new ConcurrentHashMap<>();

    /**
     * 청크 단위 업로드 처리
     */
    @RabbitListener(queues = RabbitMQConstants.CHUNK_STORAGE_QUEUE)
    public void handleChunkUpload(ContentMessage message) {
        try {
            log.info("Received chunk [{}/{}] for content: {}",
                    message.getChunkIndex() + 1, message.getTotalChunks(), message.getId());

            // 청크 정보 추출
            byte[] chunkData = message.getContentData();
            int chunkIndex = message.getChunkIndex();
            int totalChunks = message.getTotalChunks();

            // 객체 이름 생성
            String tempObjectName = String.format("%s/chunk_%d", message.getId(), chunkIndex);

            // MinIO에 청크 저장
            uploadChunk(tempObjectName, chunkData, message);

            // 청크 카운터 업데이트
            Integer receivedChunks = chunkTracker.compute(message.getId(),
                    (id, count) -> count == null ? 1 : count + 1);

            // 모든 청크 수신 완료 확인
            if (receivedChunks == totalChunks) {
                log.info("All chunks received for content: {}, merging...", message.getId());

                // 단일 파일 병합 (또는 단일 청크인 경우 바로 처리)
                String finalObjectName = processCompleteContent(message);

                // 액세스 URL 생성
                String accessUrl = generateAccessUrl(finalObjectName);

                // 메시지 업데이트
                ContentMessage validationMessage = message.createLightCopy();
                validationMessage.setAccessUrl(accessUrl);
                validationMessage.setStatus(ContentStatus.UPLOADED);

                // 상태 업데이트 발행
                rabbitTemplate.convertAndSend(
                        RabbitMQConstants.CONTENT_EXCHANGE,
                        RabbitMQConstants.STATUS_UPDATE_ROUTING_KEY,
                        validationMessage);

                validationMessage.setContentData(chunkData);

                // 검증 요청 발행
                rabbitTemplate.convertAndSend(
                        RabbitMQConstants.CONTENT_EXCHANGE,
                        RabbitMQConstants.VALIDATION_ROUTING_KEY,
                        validationMessage);

                // 임시 청크 정리
                cleanupTempChunks(message.getId(), totalChunks);

                // 청크 트래커에서 제거
                chunkTracker.remove(message.getId());

                log.info("Content ready for validation: {}, url: {}", message.getId(), accessUrl);
            }

        } catch (Exception e) {
            log.error("Error handling chunk upload for content: {}", message.getId(), e);

            // 오류 상태 업데이트
            ContentMessage errorMessage = message.createLightCopy();
            errorMessage.withError("Chunk storage error: " + e.getMessage());
            errorMessage.setStatus(ContentStatus.FAILED);

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.CONTENT_EXCHANGE,
                    RabbitMQConstants.STATUS_UPDATE_ROUTING_KEY,
                    errorMessage);

            // 알림 서비스로 전송
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.CONTENT_EXCHANGE,
                    RabbitMQConstants.NOTIFICATION_ROUTING_KEY,
                    errorMessage);
        }
    }

    /**
     * 단일 청크 업로드
     */
    private void uploadChunk(String objectName, byte[] data, ContentMessage message) throws Exception {
        // 객체 메타데이터 설정
        Map<String, String> userMetadata = new HashMap<>();
        userMetadata.put("Content-Type", message.getContentType());
        userMetadata.put("X-Content-Id", message.getId());
        userMetadata.put("X-Chunk-Index", String.valueOf(message.getChunkIndex()));
        userMetadata.put("X-Total-Chunks", String.valueOf(message.getTotalChunks()));
        userMetadata.put("X-User-Id", message.getUserId());
        userMetadata.put("X-File-Name", message.getFileName());

        // MinIO에 업로드
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(tempBucketName)
                        .object(objectName)
                        .stream(new ByteArrayInputStream(data), data.length, -1)
                        .contentType(message.getContentType())
                        .userMetadata(userMetadata)
                        .build()
        );

        log.debug("Chunk uploaded: bucket={}, object={}", tempBucketName, objectName);
    }

    /**
     * 모든 청크가 수신된 후 처리
     */
    private String processCompleteContent(ContentMessage message) throws Exception {
        int totalChunks = message.getTotalChunks();

        // 단일 청크인 경우 바로 최종 버킷으로 복사
        if (totalChunks == 1) {
            String sourceObjectName = String.format("%s/chunk_0", message.getId());
            String targetObjectName = generateFinalObjectName(message);

            // 소스 객체 복사
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .source(CopySource.builder()
                                    .bucket(tempBucketName)
                                    .object(sourceObjectName)
                                    .build())
                            .bucket(bucketName)
                            .object(targetObjectName)
                            .build()
            );

            return targetObjectName;
        } else {
            // 여러 청크의 경우 병합 작업 필요
            // 실제 구현에서는 MinIO의 Compose Object 또는 서버 측 병합 사용
            // 여기서는 단순화하고 임시로 단일 객체로 처리

            // 이 코드는 실제로 청크 병합을 구현해야 함
            String finalObjectName = generateFinalObjectName(message);

            // 구현해야 할 병합 로직...

            return finalObjectName;
        }
    }

    /**
     * 임시 청크 파일 정리
     */
    private void cleanupTempChunks(String contentId, int totalChunks) {
        try {
            for (int i = 0; i < totalChunks; i++) {
                String chunkObjectName = String.format("%s/chunk_%d", contentId, i);

                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(tempBucketName)
                                .object(chunkObjectName)
                                .build()
                );
            }
            log.debug("Temporary chunks cleaned up for content: {}", contentId);
        } catch (Exception e) {
            log.warn("Error cleaning up temporary chunks for content: {}", contentId, e);
        }
    }

    /**
     * 파일 접근 URL 생성
     */
    private String generateAccessUrl(String objectName) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .method(Method.GET)
                        .expiry(7, TimeUnit.DAYS)
                        .build()
        );
    }

    /**
     * 최종 객체 이름 생성
     */
    private String generateFinalObjectName(ContentMessage message) {
        String extension = getFileExtension(message.getContentType(), message.getFileName());
        return String.format("content/%s/%s%s",
                message.getUserId(), UUID.randomUUID().toString(), extension);
    }

    /**
     * 파일 확장자 결정
     */
    private String getFileExtension(String contentType, String fileName) {
        // 파일명에서 확장자 추출 시도
        if (fileName != null && fileName.lastIndexOf(".") > 0) {
            return fileName.substring(fileName.lastIndexOf("."));
        }

        // 콘텐츠 타입으로 확장자 결정
        if (contentType == null) {
            return "";
        }

        switch (contentType.toLowerCase()) {
            case "image/jpeg": return ".jpg";
            case "image/png": return ".png";
            case "image/gif": return ".gif";
            case "video/mp4": return ".mp4";
            case "video/mpeg": return ".mpeg";
            case "audio/mpeg": return ".mp3";
            case "application/pdf": return ".pdf";
            default: return "";
        }
    }
}