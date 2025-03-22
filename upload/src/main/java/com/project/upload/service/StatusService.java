package com.project.upload.service;

import com.project.common.model.ContentStatus;
import com.project.upload.dto.ContentStatusDTO;
import com.project.upload.entity.ContentStatusEntity;
import com.project.upload.entity.ContentStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatusService {

    private final ContentStatusRepository contentStatusRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 콘텐츠 상태 업데이트 및 웹소켓으로 알림
     */
    @Transactional
    public boolean updateStatus(String contentId, String userId, String fileName, ContentStatus status,
                                String errorMessage, String accessUrl) {
        try {
            // 기존 상태 조회
            ContentStatusEntity entity = contentStatusRepository.findByContentId(contentId)
                    .orElse(new ContentStatusEntity());

            // 상태 업데이트
            entity.setContentId(contentId);

            // 사용자 ID와 파일명 설정 (처음 저장할 때만)
            if (entity.getId() == null) {
                entity.setCreatedAt(System.currentTimeMillis());

                if (userId != null && !userId.isEmpty()) {
                    entity.setUserId(userId);
                }
                if (fileName != null && !fileName.isEmpty()) {
                    entity.setFileName(fileName);
                }
            }

            entity.setStatus(status);
            entity.setUpdatedAt(System.currentTimeMillis());

            // 오류 메시지 업데이트 (있는 경우)
            if (errorMessage != null) {
                entity.setErrorMessage(errorMessage);
            }

            // 액세스 URL 업데이트 (있는 경우)
            if (accessUrl != null) {
                entity.setAccessUrl(accessUrl);
            }

            // 엔티티 저장
            contentStatusRepository.save(entity);

            // 웹소켓으로 상태 업데이트 전송
            sendStatusUpdateWebSocket(entity);

            log.debug("Status updated: contentId={}, status={}", contentId, status);
            return true;
        } catch (Exception e) {
            log.error("Failed to update status: contentId={}, status={}", contentId, status, e);
            return false;
        }
    }
    /**
     * 웹소켓으로 상태 업데이트 전송
     */
    private void sendStatusUpdateWebSocket(ContentStatusEntity entity) {
        try {
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("contentId", entity.getContentId());
            statusUpdate.put("status", entity.getStatus().name());
            statusUpdate.put("message", getStatusMessage(entity));
            statusUpdate.put("errorMessage", entity.getErrorMessage());
            statusUpdate.put("accessUrl", entity.getAccessUrl());
            statusUpdate.put("timestamp", entity.getUpdatedAt());

            // 콘텐츠별 토픽으로 전송
            messagingTemplate.convertAndSend("/topic/content/" + entity.getContentId(), statusUpdate);

            // 사용자별 큐로도 전송 (사용자 ID가 있는 경우)
            if (entity.getUserId() != null) {
                messagingTemplate.convertAndSend("/queue/status/" + entity.getUserId(), statusUpdate);
            }

            log.debug("Status update sent via WebSocket for content: {}", entity.getContentId());
        } catch (Exception e) {
            log.error("Error sending status update via WebSocket for content: {}", entity.getContentId(), e);
        }
    }

    /**
     * 상태에 따른 메시지 생성
     */
    private String getStatusMessage(ContentStatusEntity entity) {
        if (entity.getErrorMessage() != null) {
            return entity.getErrorMessage();
        }

        return switch (entity.getStatus()) {
            case UPLOADING -> "파일 업로드 중...";
            case UPLOADED -> "파일 업로드가 완료되었습니다. 검증 대기 중...";
            case VALIDATING -> "파일 검증 중...";
            case VALIDATED -> "파일 검증이 완료되었습니다. 처리 대기 중...";
            case VALIDATION_FAILED -> "파일 검증에 실패했습니다.";
            case PROCESSING -> "파일 처리 중...";
            case PROCESSED -> "파일 처리가 완료되었습니다. 저장 중...";
            case STORING -> "파일 저장 중...";
            case STORED -> "파일 저장이 완료되었습니다.";
            case COMPLETED -> "모든 처리가 완료되었습니다.";
            case FAILED -> "처리 중 오류가 발생했습니다.";
            default -> "상태 업데이트: " + entity.getStatus().name();
        };
    }

    /**
     * 콘텐츠 상태 조회
     */
    public ContentStatusDTO getStatus(String contentId) {
        return contentStatusRepository.findByContentId(contentId)
                .map(this::mapToDTO)
                .orElse(null);
    }

    /**
     * 사용자의 콘텐츠 상태 목록 조회
     */
    public List<ContentStatusDTO> getUserContentStatus(String userId) {
        return contentStatusRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 엔티티를 DTO로 변환
     */
    private ContentStatusDTO mapToDTO(ContentStatusEntity entity) {
        ContentStatusDTO dto = new ContentStatusDTO();
        dto.setContentId(entity.getContentId());
        dto.setUserId(entity.getUserId());
        dto.setFileName(entity.getFileName());
        dto.setStatus(entity.getStatus());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setAccessUrl(entity.getAccessUrl());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}