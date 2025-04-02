package com.project.upload.listener;

import com.project.common.constants.RabbitMQConstants;
import com.project.common.model.ContentMessage;
import com.project.common.model.ContentStatus;
import com.project.upload.service.StatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatusUpdateConsumer {

    private final StatusService statusService;

    @RabbitListener(queues = RabbitMQConstants.STATUS_UPDATE_QUEUE)
    public void consumeStatusUpdate(ContentMessage message) {
        log.info("Received status update for content: {}, status: {}",
                message.getId(), message.getStatus());

        try {
            // userId 필드가 없는 경우 처리
            if (message.getUserId() == null || message.getUserId().isEmpty()) {
                log.warn("Status update missing userId for content: {}", message.getId());
            }

            // 파일명 필드가 없는 경우 처리
            if (message.getFileName() == null || message.getFileName().isEmpty()) {
                log.warn("Status update missing fileName for content: {}", message.getId());
            }

            //status에서 PROCCESSED 받을 경우 COMPLETED 호출
            if (message.getStatus() == ContentStatus.PROCESSED) {
                message.setStatus(ContentStatus.COMPLETED);
            }

            // 상태 업데이트 서비스 호출 (웹소켓 알림 포함)
            boolean updated = statusService.updateStatus(
                    message.getId(),
                    message.getUserId(),      // 사용자 ID 전달
                    message.getFileName(),    // 파일명 전달
                    message.getStatus(),
                    message.getErrorMessage(),
                    message.getAccessUrl()
            );

            if (updated) {
                log.debug("Status updated successfully for content: {}", message.getId());
            } else {
                log.warn("Failed to update status for content: {}", message.getId());
            }
        } catch (Exception e) {
            log.error("Error processing status update for content: {}", message.getId(), e);
        }
    }
}