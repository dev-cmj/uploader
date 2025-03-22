package com.project.validate.service;

import com.project.common.model.ContentMessage;
import com.project.common.model.FileType;
import com.project.common.model.ValidationResult;
import com.project.common.model.ValidationResult.ValidationIssue;
import com.project.common.model.ValidationResult.ValidationSeverity;
import com.project.validate.validator.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {

    private final Map<FileType, FileValidator> validatorMap;
    private final AntiVirusService antiVirusService;

    /**
     * 콘텐츠 검증 수행
     *
     * @param message 검증할 콘텐츠 메시지
     * @return 검증 결과
     */
    public ValidationResult validateContent(ContentMessage message) {
        log.info("Validating content: {}, type: {}", message.getId(), message.getContentType());

        try {
            // 결과 객체 초기화
            ValidationResult result = new ValidationResult();
            result.setContentId(message.getId());
            result.setValid(true); // 기본값은 유효함
            result.setValidationTime(System.currentTimeMillis());
            result.setValidator("content-validation-service");

            // 기본 유효성 검사
            if (!validateBasicProperties(message, result)) {
                return result; // 기본 검증 실패
            }

            // 바이러스 검사
            if (!performAntiVirusScan(message, result)) {
                return result; // 바이러스 검사 실패
            }

            // 파일 타입별 검증
            if (message.getFileType() != null && validatorMap.containsKey(message.getFileType())) {
                FileValidator validator = validatorMap.get(message.getFileType());
                validator.validate(message, result);
            }

            // 최종 유효성 상태 확인 (이슈가 추가된 경우 재평가)
            boolean isValid = result.isValid() && result.getIssues().stream()
                    .noneMatch(issue -> issue.getSeverity() == ValidationSeverity.FATAL
                            || issue.getSeverity() == ValidationSeverity.ERROR);

            result.setValid(isValid);
            if (!isValid && result.getErrorMessage() == null) {
                result.setErrorMessage("파일 검증에 실패했습니다");
            }

            log.info("Validation completed for content: {}, valid: {}", message.getId(), isValid);
            return result;

        } catch (Exception e) {
            log.error("Error validating content: {}", message.getId(), e);
            return ValidationResult.invalid(message.getId(), "검증 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 기본 속성 검증
     */
    private boolean validateBasicProperties(ContentMessage message, ValidationResult result) {
        // 파일 존재 확인
        if (message.getSourcePath() != null) {
            Path filePath = Paths.get(message.getSourcePath());
            if (!Files.exists(filePath)) {
                result.addIssue(new ValidationIssue(
                        ValidationSeverity.FATAL,
                        "FILE_NOT_FOUND",
                        "파일을 찾을 수 없습니다",
                        "경로: " + message.getSourcePath()
                ));
                result.setValid(false);
                result.setErrorMessage("파일을 찾을 수 없습니다");
                return false;
            }
        } else if (message.getTempFilePath() != null) {
            Path filePath = Paths.get(message.getTempFilePath());
            if (!Files.exists(filePath)) {
                result.addIssue(new ValidationIssue(
                        ValidationSeverity.FATAL,
                        "FILE_NOT_FOUND",
                        "임시 파일을 찾을 수 없습니다",
                        "경로: " + message.getTempFilePath()
                ));
                result.setValid(false);
                result.setErrorMessage("임시 파일을 찾을 수 없습니다");
                return false;
            }
        } else if (message.getContentData() == null) {
            result.addIssue(new ValidationIssue(
                    ValidationSeverity.FATAL,
                    "NO_CONTENT_DATA",
                    "콘텐츠 데이터가 없습니다",
                    "콘텐츠 데이터가 존재하지 않습니다"
            ));
            result.setValid(false);
            result.setErrorMessage("콘텐츠 데이터가 없습니다");
            return false;
        }

        // 추가 기본 검증 (파일 크기, 타입 등)
        if (message.getFileSize() <= 0) {
            result.addIssue(new ValidationIssue(
                    ValidationSeverity.ERROR,
                    "INVALID_FILE_SIZE",
                    "파일 크기가 유효하지 않습니다",
                    "파일 크기: " + message.getFileSize()
            ));
            result.setValid(false);
            result.setErrorMessage("파일 크기가 유효하지 않습니다");
            return false;
        }

        // 파일 타입 확인 및 추론
        if (message.getFileType() == null && message.getContentType() != null) {
            message.inferFileTypeFromContentType();
        }

        return true;
    }

    /**
     * 바이러스 검사 수행
     */
    private boolean performAntiVirusScan(ContentMessage message, ValidationResult result) {
        try {
            boolean isSafe;

            if (message.getSourcePath() != null) {
                isSafe = antiVirusService.scanFile(message.getSourcePath());
            } else if (message.getTempFilePath() != null) {
                isSafe = antiVirusService.scanFile(message.getTempFilePath());
            } else if (message.getContentData() != null) {
                isSafe = antiVirusService.scanBytes(message.getContentData());
            } else {
                result.addIssue(new ValidationIssue(
                        ValidationSeverity.FATAL,
                        "SCAN_FAILED",
                        "바이러스 검사를 수행할 수 없습니다",
                        "검사할 콘텐츠 데이터가 없습니다"
                ));
                result.setValid(false);
                result.setErrorMessage("바이러스 검사를 수행할 수 없습니다");
                return false;
            }

            if (!isSafe) {
                result.addIssue(new ValidationIssue(
                        ValidationSeverity.FATAL,
                        "VIRUS_DETECTED",
                        "파일에서 악성 코드가 발견되었습니다",
                        "보안상의 이유로 파일이 거부되었습니다"
                ));
                result.setValid(false);
                result.setErrorMessage("파일에서 악성 코드가 발견되었습니다");
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Error during virus scan for content: {}", message.getId(), e);
            result.addIssue(new ValidationIssue(
                    ValidationSeverity.ERROR,
                    "SCAN_ERROR",
                    "바이러스 검사 중 오류가 발생했습니다",
                    e.getMessage()
            ));
            result.setValid(false);
            result.setErrorMessage("바이러스 검사 중 오류가 발생했습니다");
            return false;
        }
    }
}