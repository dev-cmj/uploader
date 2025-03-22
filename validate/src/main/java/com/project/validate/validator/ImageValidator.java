package com.project.validate.validator;

import com.project.common.model.ContentMessage;
import com.project.common.model.ValidationResult;
import com.project.common.model.ValidationResult.ValidationSeverity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.project.common.model.ValidationResult.*;

@Slf4j
@Component
public class ImageValidator implements FileValidator {

    @Value("${validation.image.max-width:8000}")
    private int maxWidth;

    @Value("${validation.image.max-height:8000}")
    private int maxHeight;

    @Value("${validation.image.min-width:10}")
    private int minWidth;

    @Value("${validation.image.min-height:10}")
    private int minHeight;

    @Value("${validation.image.max-size-mb:50}")
    private int maxSizeMb;

    private static final Set<String> ALLOWED_FORMATS = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "tiff", "webp")
    );

    @Override
    public void validate(ContentMessage message, ValidationResult result) {
        log.info("Validating image: {}", message.getId());

        try {
            // 이미지 데이터 가져오기
            BufferedImage image;
            if (message.getSourcePath() != null) {
                image = ImageIO.read(new File(message.getSourcePath()));
            } else if (message.getTempFilePath() != null) {
                image = ImageIO.read(new File(message.getTempFilePath()));
            } else if (message.getContentData() != null) {
                image = ImageIO.read(new ByteArrayInputStream(message.getContentData()));
            } else {
                result.addIssue(new ValidationIssue(
                        ValidationSeverity.FATAL,
                        "IMAGE_DATA_MISSING",
                        "이미지 데이터가 없습니다",
                        "이미지 데이터를 찾을 수 없습니다"
                ));
                return;
            }

            // 이미지 읽기 실패 확인
            if (image == null) {
                result.addIssue(new ValidationIssue(
                        ValidationSeverity.FATAL,
                        "INVALID_IMAGE_FORMAT",
                        "이미지 형식이 유효하지 않습니다",
                        "지원되지 않는 이미지 형식이거나 손상된 이미지입니다"
                ));
                return;
            }

            // 이미지 크기 검증
            int width = image.getWidth();
            int height = image.getHeight();

            if (width > maxWidth || height > maxHeight) {
                result.addIssue(new ValidationIssue(
                        ValidationSeverity.ERROR,
                        "IMAGE_TOO_LARGE",
                        "이미지 해상도가 너무 큽니다",
                        String.format("최대 허용 해상도: %dx%d, 현재 해상도: %dx%d", maxWidth, maxHeight, width, height)
                ));
            }

            if (width < minWidth || height < minHeight) {
                result.addIssue(new ValidationIssue(
                        ValidationSeverity.ERROR,
                        "IMAGE_TOO_SMALL",
                        "이미지 해상도가 너무 작습니다",
                        String.format("최소 허용 해상도: %dx%d, 현재 해상도: %dx%d", minWidth, minHeight, width, height)
                ));
            }

            // 파일 크기 검증
            long fileSizeBytes;
            if (message.getSourcePath() != null) {
                fileSizeBytes = Files.size(Paths.get(message.getSourcePath()));
            } else if (message.getTempFilePath() != null) {
                fileSizeBytes = Files.size(Paths.get(message.getTempFilePath()));
            } else {
                fileSizeBytes = message.getContentData().length;
            }

            long maxSizeBytes = maxSizeMb * 1024L * 1024L;
            if (fileSizeBytes > maxSizeBytes) {
                result.addIssue(new ValidationIssue(
                        ValidationSeverity.ERROR,
                        "IMAGE_FILE_TOO_LARGE",
                        "이미지 파일 크기가 너무 큽니다",
                        String.format("최대 허용 크기: %dMB, 현재 크기: %.2fMB",
                                maxSizeMb, fileSizeBytes / (1024.0 * 1024.0))
                ));
            }

            // 이미지 포맷 검증
            String formatName = getImageFormat(message);
            if (formatName != null && !ALLOWED_FORMATS.contains(formatName.toLowerCase())) {
                result.addIssue(new ValidationIssue(
                        ValidationSeverity.WARNING,
                        "UNSUPPORTED_IMAGE_FORMAT",
                        "지원되지 않는 이미지 형식입니다",
                        "지원되는 형식: " + String.join(", ", ALLOWED_FORMATS)
                ));
            }

            log.info("Image validation completed for: {}, width: {}, height: {}",
                    message.getId(), width, height);

        } catch (Exception e) {
            log.error("Error validating image: {}", message.getId(), e);
            result.addIssue(new ValidationIssue(
                    ValidationSeverity.ERROR,
                    "IMAGE_VALIDATION_ERROR",
                    "이미지 검증 중 오류가 발생했습니다",
                    e.getMessage()
            ));
        }
    }

    /**
     * 이미지 포맷 추출
     */
    private String getImageFormat(ContentMessage message) {
        if (message.getFileName() != null && message.getFileName().lastIndexOf(".") > 0) {
            return message.getFileName()
                    .substring(message.getFileName().lastIndexOf(".") + 1)
                    .toLowerCase();
        }

        if (message.getContentType() != null) {
            String contentType = message.getContentType().toLowerCase();
            if (contentType.startsWith("image/")) {
                return contentType.substring(6);
            }
        }

        return null;
    }
}