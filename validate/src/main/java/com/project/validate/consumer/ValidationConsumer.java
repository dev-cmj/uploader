package com.project.validate.consumer;

import com.project.common.constants.RabbitMQConstants;
import com.project.common.model.ContentMessage;
import com.project.common.model.ContentStatus;
import com.project.common.model.ValidationResult;
import com.project.validate.service.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationConsumer {

    private final ValidationService validationService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConstants.VALIDATION_QUEUE)
    public void consumeValidationMessage(ContentMessage message) {
        log.info("Received validation request for content: {}", message.getId());

        try {
            // 검증 상태 업데이트 메시지 발행
            message.nextStage(ContentStatus.VALIDATING);
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.CONTENT_EXCHANGE,
                    RabbitMQConstants.STATUS_UPDATE_ROUTING_KEY,
                    message);

            // 파일 검증 수행
            ValidationResult validationResult = validationService.validateContent(message);
            boolean isValid = validationResult.isValid();

            // 상태 업데이트 발행
            if (isValid) {
                message.nextStage(ContentStatus.VALIDATED);
            } else {
                message.withError(validationResult.getErrorMessage());
                message.nextStage(ContentStatus.VALIDATION_FAILED);
            }

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.CONTENT_EXCHANGE,
                    RabbitMQConstants.STATUS_UPDATE_ROUTING_KEY,
                    message);

            // 다음 단계로 전달 또는 실패 알림
            if (isValid) {
                // 처리 서비스로 전달
                rabbitTemplate.convertAndSend(
                        RabbitMQConstants.CONTENT_EXCHANGE,
                        RabbitMQConstants.PROCESSING_ROUTING_KEY,
                        message);
                log.info("Validation passed, message sent to processing: {}", message.getId());
            } else {
                // 검증 실패 시 알림 서비스로 전달
                rabbitTemplate.convertAndSend(
                        RabbitMQConstants.CONTENT_EXCHANGE,
                        RabbitMQConstants.NOTIFICATION_ROUTING_KEY,
                        message);
                log.warn("Validation failed: {}, error: {}",
                        message.getId(), validationResult.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Error during validation for content: {}", message.getId(), e);

            // 오류 상태로 업데이트
            message.withError("Validation error: " + e.getMessage());

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