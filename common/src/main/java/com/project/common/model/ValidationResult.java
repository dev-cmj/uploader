package com.project.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String contentId;
    private boolean valid;
    private String errorMessage;
    private List<ValidationIssue> issues = new ArrayList<>();

    // 추가 메타데이터
    private long validationTime;
    private String validator;

    /**
     * 간단한 유효/무효 결과 생성
     */
    public static ValidationResult valid(String contentId) {
        ValidationResult result = new ValidationResult();
        result.setContentId(contentId);
        result.setValid(true);
        result.setValidationTime(System.currentTimeMillis());
        return result;
    }

    /**
     * 오류 메시지와 함께 무효 결과 생성
     */
    public static ValidationResult invalid(String contentId, String errorMessage) {
        ValidationResult result = new ValidationResult();
        result.setContentId(contentId);
        result.setValid(false);
        result.setErrorMessage(errorMessage);
        result.setValidationTime(System.currentTimeMillis());
        return result;
    }

    /**
     * 이슈를 추가하고 유효성 상태 자동 설정
     */
    public ValidationResult addIssue(ValidationIssue issue) {
        this.issues.add(issue);

        // 하나라도 FATAL이면 무효로 설정
        if (issue.getSeverity() == ValidationSeverity.FATAL) {
            this.valid = false;
            if (this.errorMessage == null) {
                this.errorMessage = issue.getMessage();
            }
        }

        return this;
    }

    /**
     * 검증 이슈 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationIssue implements Serializable {
        private static final long serialVersionUID = 1L;

        private ValidationSeverity severity;
        private String code;
        private String message;
        private String details;
    }

    /**
     * 검증 이슈 심각도
     */
    public enum ValidationSeverity {
        INFO,       // 정보성 메시지
        WARNING,    // 경고 (유효성 판단에 영향 없음)
        ERROR,      // 오류 (일반적으로 유효성 실패)
        FATAL       // 치명적 오류 (항상 유효성 실패)
    }
}