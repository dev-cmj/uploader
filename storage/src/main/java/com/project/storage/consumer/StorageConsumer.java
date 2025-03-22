package com.project.storage.consumer;

import com.project.common.constants.RabbitMQConstants;
import com.project.common.model.ContentMessage;
import com.project.common.model.ContentStatus;
import com.project.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
//@Component
@RequiredArgsConstructor
public class StorageConsumer {

    private final StorageService storageService;
    private final RabbitTemplate rabbitTemplate;

//    @RabbitListener(queues = RabbitMQConstants.STORAGE_QUEUE)
    public void consumeStorageMessage(ContentMessage message) {
        log.info("Received storage request for content: {}", message.getId());

        try {
            // 저장 상태로 업데이트
            message.nextStage(ContentStatus.STORING);
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.CONTENT_EXCHANGE,
                    RabbitMQConstants.STATUS_UPDATE_ROUTING_KEY,
                    message);

            // 파일 저장 수행 (S3/MinIO에 저장)
            String accessUrl = storageService.storeFile(message);

            if (accessUrl != null) {
                // 저장 결과 URL 설정
                message.setAccessUrl(accessUrl);

                // 저장 완료 상태로 업데이트
                message.nextStage(ContentStatus.STORED);
                rabbitTemplate.convertAndSend(
                        RabbitMQConstants.CONTENT_EXCHANGE,
                        RabbitMQConstants.STATUS_UPDATE_ROUTING_KEY,
                        message);

                // 최종 완료 상태로 업데이트
                message.nextStage(ContentStatus.COMPLETED);
                rabbitTemplate.convertAndSend(
                        RabbitMQConstants.CONTENT_EXCHANGE,
                        RabbitMQConstants.STATUS_UPDATE_ROUTING_KEY,
                        message);

                // 알림 서비스로 메시지 발행
                rabbitTemplate.convertAndSend(
                        RabbitMQConstants.CONTENT_EXCHANGE,
                        RabbitMQConstants.NOTIFICATION_ROUTING_KEY,
                        message);

                log.info("Content stored successfully: {}, url: {}", message.getId(), accessUrl);
            } else {
                // 저장 실패
                message.withError("Failed to store content");

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

                log.warn("Content storage failed: {}", message.getId());
            }
        } catch (Exception e) {
            log.error("Error storing content: {}", message.getId(), e);

            // 오류 상태로 업데이트
            message.withError("Storage error: " + e.getMessage());

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

    /**
     * 처리된 파일을 최종 저장소에 저장하는 메서드
     */
//    @RabbitListener(queues = RabbitMQConstants.PROCESSING_QUEUE)
    public void consumeProcessedContent(ContentMessage message) {
        log.info("Received processed content for storage: {}", message.getId());

        try {
            // 저장 상태로 업데이트
            message.nextStage(ContentStatus.STORING);
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.CONTENT_EXCHANGE,
                    RabbitMQConstants.STATUS_UPDATE_ROUTING_KEY,
                    message);

            // 처리된 파일 저장 (processedPath가 설정된 경우)
            String accessUrl;
            if (message.getProcessedPath() != null) {
                accessUrl = storageService.storeProcessedFile(message);
            } else {
                // 처리된 경로가 없는 경우 원본 저장
                accessUrl = storageService.storeFile(message);
            }

            if (accessUrl != null) {
                // 저장 결과 URL 설정
                message.setAccessUrl(accessUrl);

                // 저장 완료 상태로 업데이트
                message.nextStage(ContentStatus.STORED);
                rabbitTemplate.convertAndSend(
                        RabbitMQConstants.CONTENT_EXCHANGE,
                        RabbitMQConstants.STATUS_UPDATE_ROUTING_KEY,
                        message);

                // 최종 완료 상태로 업데이트
                message.nextStage(ContentStatus.COMPLETED);
                rabbitTemplate.convertAndSend(
                        RabbitMQConstants.CONTENT_EXCHANGE,
                        RabbitMQConstants.STATUS_UPDATE_ROUTING_KEY,
                        message);

                // 알림 서비스로 메시지 발행
                rabbitTemplate.convertAndSend(
                        RabbitMQConstants.CONTENT_EXCHANGE,
                        RabbitMQConstants.NOTIFICATION_ROUTING_KEY,
                        message);

                log.info("Processed content stored successfully: {}, url: {}", message.getId(), accessUrl);
            } else {
                // 저장 실패
                message.withError("Failed to store processed content");

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

                log.warn("Processed content storage failed: {}", message.getId());
            }
        } catch (Exception e) {
            log.error("Error storing processed content: {}", message.getId(), e);

            // 오류 상태로 업데이트
            message.withError("Storage error: " + e.getMessage());

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