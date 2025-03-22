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

import static com.project.common.constants.RabbitMQConstants.STATUS_UPDATE_QUEUE;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatusUpdateService {

    private final SimpMessagingTemplate messagingTemplate;

    // 웹소켓으로 상태 업데이트 전송
    public void sendStatusUpdate(UUID contentId, String status, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("contentId", contentId);
        payload.put("status", status);
        payload.put("message", message);
        payload.put("timestamp", System.currentTimeMillis());

        log.info("Sending status update for content {}: {}", contentId, status);
        messagingTemplate.convertAndSend("/queue/status/" + contentId, payload);
    }

    // RabbitMQ 상태 업데이트 메시지 수신 및 웹소켓 전달
    @RabbitListener(queues = STATUS_UPDATE_QUEUE)
    public void handleStatusUpdate(ContentMessage message) {
        log.info("Received status update for content {}: {}", message.getId(), message.getStatus());

        String statusMessage = generateStatusMessage(message);
        sendStatusUpdate(message.getId(), message.getStatus().toString(), statusMessage);
    }

    private String generateStatusMessage(ContentMessage message) {
        return switch (message.getStatus()) {
            case UPLOADED -> "파일이 업로드되었습니다.";
            case VALIDATING -> "파일 검증 중입니다...";
            case VALIDATED -> "파일 검증이 완료되었습니다.";
            case INVALID -> "파일이 유효하지 않습니다: " + message.getErrorMessage();
            case PROCESSING -> "파일 처리 중입니다...";
            case PROCESSED -> "파일 처리가 완료되었습니다.";
            case STORING -> "파일 저장 중입니다...";
            case STORED -> "파일이 저장소에 저장되었습니다.";
            case FAILED -> "처리 중 오류가 발생했습니다: " + message.getErrorMessage();
            case COMPLETED -> "모든 처리가 완료되었습니다.";
            default -> "상태: " + message.getStatus();
        };
    }
}