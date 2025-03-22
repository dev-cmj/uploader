package com.project.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@NoArgsConstructor
public class ContentMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    // 필수 식별 정보
    private String id;                   // 콘텐츠 고유 ID
    private String userId;               // 사용자 ID
    private String fileName;             // 원본 파일명
    private String contentType;          // 컨텐츠 타입 (MIME)
    private FileType fileType;           // 파일 타입 (IMAGE, VIDEO 등)
    private long fileSize;               // 파일 크기 (바이트)

    // 스트리밍/청크 관련 정보
    private int chunkIndex;              // 현재 청크 인덱스 (0부터 시작)
    private int totalChunks;             // 총 청크 수
    private byte[] contentData;          // 현재 청크 데이터

    // 처리 상태 정보
    private ContentStatus status;        // 현재 처리 상태
    private ContentPriority priority;    // 처리 우선순위
    private long timestamp;              // 메시지 생성 시간

    // 경로 정보
    private String sourcePath;           // 원본 파일 경로
    private String tempFilePath;         // 임시 파일 경로 (임시 저장 사용 시)
    private String processedPath;        // 처리된 파일 경로
    private String accessUrl;            // 최종 액세스 URL

    // 처리 옵션
    private ProcessingOptions processingOptions; // 처리 옵션

    // 오류 정보
    private String errorMessage;         // 오류 메시지

    // 유틸리티 메서드

    /**
     * 다음 처리 단계로 상태 변경
     */
    public void nextStage(ContentStatus newStatus) {
        this.status = newStatus;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 오류 상태로 변경
     */
    public ContentMessage withError(String errorMessage) {
        this.errorMessage = errorMessage;
        this.status = ContentStatus.FAILED;
        this.timestamp = System.currentTimeMillis();
        return this;
    }

    /**
     * 메시지 복제 (청크 데이터 제외)
     * 상태 업데이트 등에 사용
     */
    public ContentMessage createLightCopy() {
        ContentMessage copy = new ContentMessage();
        copy.id = this.id;
        copy.userId = this.userId;
        copy.fileName = this.fileName;
        copy.contentType = this.contentType;
        copy.fileType = this.fileType;
        copy.fileSize = this.fileSize;
        copy.chunkIndex = this.chunkIndex;
        copy.totalChunks = this.totalChunks;
        copy.status = this.status;
        copy.priority = this.priority;
        copy.timestamp = System.currentTimeMillis();
        copy.sourcePath = this.sourcePath;
        copy.tempFilePath = this.tempFilePath;
        copy.processedPath = this.processedPath;
        copy.accessUrl = this.accessUrl;
        copy.processingOptions = this.processingOptions;
        copy.errorMessage = this.errorMessage;
        // contentData는 복사하지 않음
        return copy;
    }

    /**
     * MIME 타입에서 파일 타입 추론
     */
    public void inferFileTypeFromContentType() {
        if (contentType == null) {
            fileType = FileType.OTHER;
            return;
        }

        String lowerCaseType = contentType.toLowerCase();
        if (lowerCaseType.startsWith("image/")) {
            fileType = FileType.IMAGE;
        } else if (lowerCaseType.startsWith("video/")) {
            fileType = FileType.VIDEO;
        } else if (lowerCaseType.startsWith("audio/")) {
            fileType = FileType.AUDIO;
        } else if (lowerCaseType.equals("application/pdf")) {
            fileType = FileType.PDF;
        } else if (lowerCaseType.startsWith("text/") ||
                lowerCaseType.equals("application/json") ||
                lowerCaseType.equals("application/xml")) {
            fileType = FileType.TEXT;
        } else {
            fileType = FileType.OTHER;
        }
    }

    /**
     * 처리 옵션
     */
    @Data
    public static class ProcessingOptions implements Serializable {
        private static final long serialVersionUID = 1L;

        private boolean resizeImage;         // 이미지 리사이징 여부
        private int targetWidth;             // 대상 너비
        private int targetHeight;            // 대상 높이
        private boolean compressVideo;       // 비디오 압축 여부
        private String videoFormat;          // 변환할 비디오 포맷
        private boolean extractText;         // 텍스트 추출 여부 (PDF 등)
        private boolean generateThumbnail;   // 썸네일 생성 여부
        private boolean watermark;           // 워터마크 적용 여부
        private String watermarkText;        // 워터마크 텍스트
    }
}