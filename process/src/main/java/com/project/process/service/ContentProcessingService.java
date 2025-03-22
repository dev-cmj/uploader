package com.project.process.service;

import com.project.common.model.ContentMessage;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class ContentProcessingService {

    @Value("${file.processed-dir:./uploads/processed}")
    private String processedDir;

    /**
     * 파일 유형에 따라 적절한 처리 수행
     * @param message 처리할 콘텐츠 메시지
     * @return 처리된 파일 경로 (실패 시 null)
     */
    public String processContent(ContentMessage message) {
        log.info("Processing content: {}, type: {}", message.getId(), message.getFileType());

        try {
            // 처리 결과를 저장할 디렉토리 생성
            Path processedDirPath = Paths.get(processedDir);
            Files.createDirectories(processedDirPath);

            // 원본 파일 경로
            Path sourcePath = Paths.get(message.getSourcePath());

            // 파일 타입에 따른 처리
            return switch (message.getFileType()) {
                case IMAGE -> processImage(sourcePath, processedDirPath, message);
                case VIDEO -> processVideo(sourcePath, processedDirPath, message);
                case AUDIO -> processAudio(sourcePath, processedDirPath, message);
                case PDF -> processPdf(sourcePath, processedDirPath, message);
                case TEXT -> processText(sourcePath, processedDirPath, message);
                default ->
                    // 기본 처리 (단순 복사)
                        processDefault(sourcePath, processedDirPath, message);
            };
        } catch (Exception e) {
            log.error("Error processing content", e);
            return null;
        }
    }

    /**
     * 이미지 처리 (리사이징, 압축 등)
     */
    private String processImage(Path sourcePath, Path processedDirPath, ContentMessage message) {
        try {
            // 처리된 파일명 생성
            String originalFileName = sourcePath.getFileName().toString();
            String extension = getFileExtension(originalFileName);
            String processedFileName = UUID.randomUUID() + "-processed" + extension;
            Path destinationPath = processedDirPath.resolve(processedFileName);

            // 이미지 리사이징 및 압축
            Thumbnails.of(sourcePath.toFile())
                    .size(800, 800) // 최대 크기 설정
                    .keepAspectRatio(true) // 비율 유지
                    .outputQuality(0.8) // 품질 설정
                    .toFile(destinationPath.toFile());

            log.info("Image processed successfully: {}", destinationPath);
            return destinationPath.toString();
        } catch (Exception e) {
            log.error("Error processing image", e);
            return null;
        }
    }

    /**
     * 비디오 처리 (실제 구현은 필요에 따라 확장)
     */
    private String processVideo(Path sourcePath, Path processedDirPath, ContentMessage message) {
        // 실제 비디오 트랜스코딩 구현 시 ffmpeg 등의 라이브러리 사용
        // 이 예제에서는 단순 복사
        return processDefault(sourcePath, processedDirPath, message);
    }

    /**
     * 오디오 처리 (실제 구현은 필요에 따라 확장)
     */
    private String processAudio(Path sourcePath, Path processedDirPath, ContentMessage message) {
        // 실제 오디오 처리 구현 시 jAudio 등의 라이브러리 사용
        // 이 예제에서는 단순 복사
        return processDefault(sourcePath, processedDirPath, message);
    }

    /**
     * PDF 처리 (실제 구현은 필요에 따라 확장)
     */
    private String processPdf(Path sourcePath, Path processedDirPath, ContentMessage message) {
        // 실제 PDF 처리 구현 시 Apache PDFBox 등의 라이브러리 사용
        // 이 예제에서는 단순 복사
        return processDefault(sourcePath, processedDirPath, message);
    }

    /**
     * 텍스트 파일 처리 (실제 구현은 필요에 따라 확장)
     */
    private String processText(Path sourcePath, Path processedDirPath, ContentMessage message) {
        // 실제 텍스트 처리 구현
        // 이 예제에서는 단순 복사
        return processDefault(sourcePath, processedDirPath, message);
    }

    /**
     * 기본 처리 (파일 복사)
     */
    private String processDefault(Path sourcePath, Path processedDirPath, ContentMessage message) {
        try {
            String originalFileName = sourcePath.getFileName().toString();
            String extension = getFileExtension(originalFileName);
            String processedFileName = UUID.randomUUID() + "-processed" + extension;
            Path destinationPath = processedDirPath.resolve(processedFileName);

            Files.copy(sourcePath, destinationPath);

            log.info("File copied: {}", destinationPath);
            return destinationPath.toString();
        } catch (Exception e) {
            log.error("Error copying file", e);
            return null;
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex);
        }
        return "";
    }
}