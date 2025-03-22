package com.project.storage.service;

import com.project.common.model.ContentMessage;

public interface StorageService {
    /**
     * 파일을 스토리지에 저장하고 접근 URL을 반환합니다.
     *
     * @param message 저장할 콘텐츠 정보를 포함한 메시지
     * @return 저장된 파일에 접근 가능한 URL, 실패시 null
     */
    String storeFile(ContentMessage message);

    /**
     * 처리된 파일을 스토리지에 저장하고 접근 URL을 반환합니다.
     *
     * @param message 저장할 콘텐츠 정보를 포함한 메시지 (processedPath 포함)
     * @return 저장된 파일에 접근 가능한 URL, 실패시 null
     */
    String storeProcessedFile(ContentMessage message);
}