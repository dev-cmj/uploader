package com.project.process.consumer;

import com.project.common.constants.RabbitMQConstants;
import com.project.common.model.ContentMessage;
import com.project.process.service.ContentProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessingConsumer {

    private final ContentProcessingService processingService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConstants.PROCESSING_QUEUE)
    public void consumeProcessingMessage(ContentMessage message) {
        log.info("Received processing request for content: {}", message.getId());

        try {
            // 처리 상태로 업데이트
            message.nextStage(ContentMessage.ProcessingStatus.PROCESSING);
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.CONTENT_EXCHANGE,
                    RabbitMQConstants.STATUS_UPDATE_ROUTING_KEY,
                    message);

            // 파일 처리 수행
            String processedFilePath = processingService.processContent(message);

            if (processedFilePath != null) {
                // 처리 결과 파일 경로 설정
                message.setDestinationPath(processedFilePath);

                // 처리 완료 상태로 업데이트
                message.nextStage(ContentMessage.ProcessingStatus.PROCESSED);

                // 상태 업데이트 메시지 발행
                rabbitTemplate.convertAndSend(
                        RabbitMQConstants.CONTENT_EXCHANGE,
                        RabbitMQConstants.STATUS_UPDATE_ROUTING_KEY,
                        message);

                // 저장 서비스로 메시지 발행
                rabbitTemplate.convertAndSend(
                        RabbitMQConstants.CONTENT_EXCHANGE,
                        RabbitMQConstants.STORAGE_ROUTING_KEY,
                        message);

                log.info("Content processed successfully: {}", message.getId());
            } else {
                // 처리 실패
                message.withError("Failed to process content");
                message.nextStage(ContentMessage.ProcessingStatus.FAILED);

                // 상태 업데이트 메시지 발행
                rabbitTemplate.convertAndSend(
                        RabbitMQConstants.CONTENT_EXCHANGE,
                        RabbitMQConstants.STATUS_UPDATE_ROUTING_KEY,
                        message);

                // 알림 서비스로 메시지 발행
                rabbitTemplate.convertAndSend(
                        RabbitMQConstants.CONTENT_EXCHANGE,
                        RabbitMQConstants.NOTIFICATION_ROUTING_KEY,
                        message);

                log.warn("Content processing failed: {}", message.getId());
            }
        } catch (Exception e) {
            log.error("Error processing content: {}", message.getId(), e);

            // 오류 상태로 업데이트
            message.withError("Processing error: " + e.getMessage());
            message.nextStage(ContentMessage.ProcessingStatus.FAILED);

            // 상태 업데이트 메시지 발행
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.CONTENT_EXCHANGE,
                    RabbitMQConstants.STATUS_UPDATE_ROUTING_KEY,
                    message);

            // 알림 서비스로 메시지 발행
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.CONTENT_EXCHANGE,
                    RabbitMQConstants.NOTIFICATION_ROUTING_KEY,
                    message);
        }
    }
}