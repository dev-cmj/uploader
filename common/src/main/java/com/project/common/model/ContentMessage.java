package com.project.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentMessage implements Serializable {

    private UUID id;
    private UUID correlationId;
    private String userId;
    private String fileName;
    private String fileType;
    private String contentType;
    private Long fileSize;
    private String sourcePath;
    private String destinationPath;
    private ProcessingStatus status;
    private String errorMessage;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 처리 상태 열거형
     */
    public enum ProcessingStatus {
        UPLOADED,      // 업로드 완료
        VALIDATING,    // 검증 중
        VALIDATED,     // 검증 완료
        INVALID,       // 유효하지 않음
        PROCESSING,    // 처리 중
        PROCESSED,     // 처리 완료
        STORING,       // 저장 중
        STORED,        // 저장 완료
        FAILED,        // 처리 실패
        COMPLETED      // 모든 처리 완료
    }

    /**
     * 새 메시지 생성 (신규 업로드)
     */
    public static ContentMessage createNew(String userId, String fileName, String contentType,
                                           Long fileSize, String sourcePath) {
        return ContentMessage.builder()
                .id(UUID.randomUUID())
                .correlationId(UUID.randomUUID())
                .userId(userId)
                .fileName(fileName)
                .contentType(contentType)
                .fileType(determineFileType(contentType))
                .fileSize(fileSize)
                .sourcePath(sourcePath)
                .status(ProcessingStatus.UPLOADED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 다음 단계 메시지 생성 (처리 파이프라인 진행)
     */
    public ContentMessage nextStage(ProcessingStatus newStatus) {
        this.setStatus(newStatus);
        this.setUpdatedAt(LocalDateTime.now());
        return this;
    }

    /**
     * 오류 메시지 설정
     */
    public ContentMessage withError(String errorMessage) {
        this.setStatus(ProcessingStatus.FAILED);
        this.setErrorMessage(errorMessage);
        this.setUpdatedAt(LocalDateTime.now());
        return this;
    }

    /**
     * 콘텐츠 타입으로부터 파일 타입 결정
     */
    public static String determineFileType(String contentType) {
        if (contentType == null) {
            return "UNKNOWN";
        }

        if (contentType.startsWith("image/")) {
            return "IMAGE";
        } else if (contentType.startsWith("video/")) {
            return "VIDEO";
        } else if (contentType.startsWith("audio/")) {
            return "AUDIO";
        } else if (contentType.startsWith("application/pdf")) {
            return "PDF";
        } else if (contentType.startsWith("text/")) {
            return "TEXT";
        } else {
            return "OTHER";
        }
    }
}