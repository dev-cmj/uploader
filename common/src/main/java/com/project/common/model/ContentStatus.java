
package com.project.common.model;

/**
 * 콘텐츠 처리 상태를 나타내는 열거형
 */
public enum ContentStatus {
    // 업로드 단계
    UPLOADING,      // 업로드 진행 중
    UPLOADED,       // 업로드 완료

    // 검증 단계
    VALIDATING,     // 검증 진행 중
    VALIDATED,      // 검증 완료
    VALIDATION_FAILED, // 검증 실패

    // 처리 단계
    PROCESSING,     // 처리 진행 중
    PROCESSED,      // 처리 완료

    // 저장 단계
    STORING,        // 저장 진행 중
    STORED,         // 저장 완료

    // 최종 상태
    COMPLETED,      // 모든 처리 완료
    FAILED,         // 처리 실패

    // 기타 상태
    CANCELLED,      // 사용자에 의한 취소
    EXPIRED         // 만료됨
}