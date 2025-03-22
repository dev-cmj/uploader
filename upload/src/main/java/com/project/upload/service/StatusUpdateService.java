package com.project.upload.service;

import com.project.common.model.ContentMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatusUpdateService {

    private final SimpMessagingTemplate messagingTemplate;

    // 상태 업데이트 메시지 전송
    public void sendStatusUpdate(UUID contentId, String status, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("contentId", contentId);
        payload.put("status", status);
        payload.put("message", message);
        payload.put("timestamp", System.currentTimeMillis());

        log.debug("Sending status update for content {}: {}", contentId, status);
        messagingTemplate.convertAndSend("/queue/status/" + contentId, payload);
    }

    // RabbitMQ 메시지에서 웹소켓으로 전달
    @RabbitListener(queues = "content.status.update.queue")
    public void handleStatusUpdate(ContentMessage message) {
        log.info("Received status update for content {}: {}", message.getId(), message.getStatus());

        String statusMessage = generateStatusMessage(message);
        sendStatusUpdate(message.getId(), message.getStatus().toString(), statusMessage);
    }

    // 상태별 메시지 생성
    private String generateStatusMessage(ContentMessage message) {
        return switch (message.getStatus()) {
            case UPLOADED -> "파일이 업로드되었습니다.";
            case VALIDATING -> "파일 검증 중입니다...";
            case VALIDATED -> "파일 검증이 완료되었습니다.";
            // 기타 케이스들...
            default -> "상태: " + message.getStatus();
        };
    }
}