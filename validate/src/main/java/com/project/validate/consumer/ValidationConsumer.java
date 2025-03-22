package com.project.validate.consumer;

import com.project.common.constants.RabbitMQConstants;
import com.project.common.model.ContentMessage;
import com.project.common.model.ValidationResult;
import com.project.validate.service.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.project.common.constants.RabbitMQConstants.VALIDATION_REQUEST_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationConsumer {

    private final ValidationService validationService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = VALIDATION_REQUEST_QUEUE)
    public void consumeValidationMessage(ContentMessage message) {
        log.info("Received validation request for content: {}", message.getId());

        try {
            // 검증 상태 업데이트 메시지 발행
            message.nextStage(ContentMessage.ProcessingStatus.VALIDATING);
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.CONTENT_EXCHANGE,
                    RabbitMQConstants.STATUS_UPDATE_ROUTING_KEY,
                    message);

            // 파일 검증 수행
            boolean isValid = validationService.validateContent(message);

            // 검증 결과 객체 생성
            ValidationResult validationResult = new ValidationResult(
                    message.getId(),
                    message.getUserId(),
                    message.getFileName(),
                    message.getContentType(),
                    message.getFileSize(),
                    message.getSourcePath(),
                    "NORMAL", // 우선순위
                    isValid,
                    isValid ? null : "파일 검증에 실패했습니다"
            );

            // 검증 결과 메시지 발행 - 이 부분이 누락되었었음
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.CONTENT_EXCHANGE,
                    RabbitMQConstants.VALIDATION_RESULT_ROUTING_KEY,
                    validationResult
            );

            // 상태 업데이트도 계속 발행
            message.nextStage(isValid ?
                    ContentMessage.ProcessingStatus.VALIDATED :
                    ContentMessage.ProcessingStatus.INVALID);

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.CONTENT_EXCHANGE,
                    RabbitMQConstants.STATUS_UPDATE_ROUTING_KEY,
                    message);

            log.info("Validation completed and result sent: contentId={}, valid={}",
                    message.getId(), isValid);
        } catch (Exception e) {
            // 오류 처리 및 결과 발행
            log.error("Error validating content", e);
        }
    }
}