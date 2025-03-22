package com.project.common.model;

/**
 * 파일 유형을 나타내는 열거형
 */
public enum FileType {
    IMAGE,  // 이미지 파일 (jpg, png, gif 등)
    VIDEO,  // 비디오 파일 (mp4, avi, mov 등)
    AUDIO,  // 오디오 파일 (mp3, wav, ogg 등)
    PDF,    // PDF 문서
    TEXT,   // 텍스트 파일 (txt, html, xml, json 등)
    OTHER   // 기타 파일 형식
}