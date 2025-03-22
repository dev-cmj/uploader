package com.project.validate.validator;

import com.project.common.model.ContentMessage;
import com.project.common.model.ValidationResult;

/**
 * 파일 타입별 검증을 수행하는 인터페이스
 */
public interface FileValidator {

    /**
     * 파일 검증 수행
     *
     * @param message 검증할 콘텐츠 메시지
     * @param result 검증 결과를 누적할 결과 객체
     */
    void validate(ContentMessage message, ValidationResult result);
}