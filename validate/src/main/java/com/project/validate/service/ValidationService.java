package com.project.validate.service;

import com.project.common.model.ContentMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class ValidationService {

    // 허용된 이미지 MIME 타입 목록
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    );

    // 허용된 비디오 MIME 타입 목록
    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList(
            "video/mp4", "video/webm", "video/quicktime", "video/x-msvideo"
    );

    // 허용된 오디오 MIME 타입 목록
    private static final List<String> ALLOWED_AUDIO_TYPES = Arrays.asList(
            "audio/mpeg", "audio/wav", "audio/ogg", "audio/midi"
    );

    // 허용된 문서 MIME 타입 목록
    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList(
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );

    // 최대 파일 크기 (200MB)
    private static final long MAX_FILE_SIZE = 200 * 1024 * 1024;

    private final Tika tika = new Tika();

    /**
     * 콘텐츠 검증 수행
     * @param message 검증할 콘텐츠 메시지
     * @return 검증 결과 (true: 유효함, false: 유효하지 않음)
     */
    public boolean validateContent(ContentMessage message) {
        log.info("Validating content: {}", message.getId());

        // 파일 경로 검증
        Path filePath = Paths.get(message.getSourcePath());
        File file = filePath.toFile();

        if (!file.exists()) {
            log.error("File does not exist: {}", message.getSourcePath());
            message.withError("File does not exist");
            return false;
        }

        // 파일 크기 검증
        if (file.length() > MAX_FILE_SIZE) {
            log.error("File size exceeds the limit: {}", file.length());
            message.withError("File size exceeds the limit");
            return false;
        }

        try {
            // MIME 타입 검증
            String detectedMimeType = tika.detect(file);
            log.info("Detected MIME type: {}", detectedMimeType);

            // 파일 유형에 따른 MIME 타입 검증
            boolean isValidMimeType = switch (message.getFileType()) {
                case "IMAGE" -> ALLOWED_IMAGE_TYPES.contains(detectedMimeType);
                case "VIDEO" -> ALLOWED_VIDEO_TYPES.contains(detectedMimeType);
                case "AUDIO" -> ALLOWED_AUDIO_TYPES.contains(detectedMimeType);
                case "PDF", "TEXT" -> ALLOWED_DOCUMENT_TYPES.contains(detectedMimeType);
                default ->
                    // 기타 유형은 기본적으로 허용하지 않음
                        false;
            };

            if (!isValidMimeType) {
                log.error("Invalid MIME type: {}", detectedMimeType);
                message.withError("Invalid file type");
                return false;
            }

            // 파일 내용 검증 (간단한 예시)
            // 실제 구현에서는 바이러스 스캔, 암호화 검사 등을 수행할 수 있음
            boolean isContentValid = validateFileContent(file, detectedMimeType);

            if (!isContentValid) {
                log.error("Invalid file content");
                message.withError("Invalid file content");
                return false;
            }

            // 모든 검증 통과
            return true;

        } catch (IOException e) {
            log.error("Error validating file", e);
            message.withError("Error validating file: " + e.getMessage());
            return false;
        }
    }

    /**
     * 파일 내용 검증 (실제 구현은 필요에 따라 확장)
     * @param file 검증할 파일
     * @param mimeType 파일의 MIME 타입
     * @return 검증 결과
     */
    private boolean validateFileContent(File file, String mimeType) {
        // 실제 구현에서는 바이러스 스캔, 이미지 내용 검사 등을 수행
        // 예시로는 파일이 비어 있지 않은지만 확인
        return file.length() > 0;
    }
}