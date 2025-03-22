package com.project.upload.api;

import com.project.common.constants.RabbitMQConstants;
import com.project.common.model.ContentMessage;
import com.project.common.model.ValidationResult;
import com.project.upload.service.FileStorageService;
import com.project.upload.service.StatusUpdateService;
import com.project.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationResultListener {

    private final FileStorageService fileStorageService;
    private final StatusUpdateService statusUpdateService;
    private final UploadService uploadService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConstants.VALIDATION_RESULT_QUEUE)
    public void handleValidationResult(ValidationResult result) {
        log.info("검증 결과 수신: contentId={}, valid={}", result.getContentId(), result.isValid());

        if (result.isValid()) {
            try {
                // 검증 성공 - 상태 업데이트
                statusUpdateService.sendStatusUpdate(
                        result.getContentId(),
                        "VALIDATED",
                        "파일 검증이 완료되었습니다."
                );

                // 임시 파일을 최종 위치로 이동
                String finalPath = fileStorageService.moveToFinalLocation(result.getFilePath());
                log.info("파일 검증 성공, 최종 저장 위치: {}", finalPath);

                // DB에 업로드 정보 저장 및 다음 처리 단계 진행
                ContentMessage message = uploadService.processUpload(
                        result.getContentId(),
                        result.getUserId(),
                        result.getFileName(),
                        result.getContentType(),
                        result.getFileSize(),
                        finalPath,
                        result.getPriority()
                );

                // 처리 서비스로 메시지 전송 (필요시)
                if (shouldSendToProcessing()) {
                    rabbitTemplate.convertAndSend(
                            "content.exchange",
                            "content.processing",
                            message
                    );
                }

            } catch (Exception e) {
                log.error("검증 후 처리 중 오류 발생", e);
                statusUpdateService.sendStatusUpdate(
                        result.getContentId(),
                        "FAILED",
                        "검증 후 처리 중 오류 발생: " + e.getMessage()
                );
                fileStorageService.deleteFile(result.getFilePath());
            }
        } else {
            // 검증 실패 - 임시 파일 삭제
            fileStorageService.deleteFile(result.getFilePath());
            log.info("파일 검증 실패, 임시 파일 삭제: {}", result.getFilePath());

            // 실패 상태 업데이트
            statusUpdateService.sendStatusUpdate(
                    result.getContentId(),
                    "INVALID",
                    "파일 검증에 실패했습니다: " + result.getErrorMessage()
            );
        }
    }

    private boolean shouldSendToProcessing() {
        // 설정이나 조건에 따라 처리 서비스로 전송 여부 결정
        return true;
    }
}