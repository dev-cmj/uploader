package com.project.upload.listener;

import com.project.common.constants.RabbitMQConstants;
import com.project.common.model.ContentMessage;
import com.project.common.model.ValidationResult;
import com.project.upload.service.FileStorageService;
import com.project.upload.service.StatusUpdateService;
import com.project.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationResultListener {
    private final FileStorageService fileStorageService;
    private final StatusUpdateService statusUpdateService;
    private final UploadService uploadService;
    private final TaskExecutor taskExecutor;

    @RabbitListener(queues = RabbitMQConstants.VALIDATION_RESULT_QUEUE)
    public void handleValidationResult(ValidationResult result) {
        log.info("검증 결과 수신: contentId={}, valid={}", result.getContentId(), result.isValid());

        if (result.isValid()) {
            // 1. DB 상태 업데이트
            uploadService.updateStatus(result.getContentId(),
                    ContentMessage.ProcessingStatus.VALIDATED, null);
            statusUpdateService.sendStatusUpdate(
                    result.getContentId(), "VALIDATED", "파일 검증이 완료되었습니다.");

            // 2. 비동기 처리 시작
            taskExecutor.execute(() -> processAndStoreFile(result));
        } else {
            // 검증 실패 처리
            handleValidationFailure(result);
        }
    }

    private void processAndStoreFile(ValidationResult result) {
        try {
            // 1. 처리 단계 시작
            uploadService.updateStatus(result.getContentId(),
                    ContentMessage.ProcessingStatus.PROCESSING, null);
            statusUpdateService.sendStatusUpdate(
                    result.getContentId(), "PROCESSING", "파일 처리 중...");

            // 2. 파일 이동은 추후에 클라우드로 ...
//            String finalPath = fileStorageService.moveToFinalLocation(result.getFilePath());
//            uploadService.updateFilePath(result.getContentId(), finalPath);

            // 3. 처리 완료
            uploadService.updateStatus(result.getContentId(),
                    ContentMessage.ProcessingStatus.PROCESSED, null);
            statusUpdateService.sendStatusUpdate(
                    result.getContentId(), "PROCESSED", "파일 처리가 완료되었습니다.");

            // 4. 저장 단계
            uploadService.updateStatus(result.getContentId(),
                    ContentMessage.ProcessingStatus.STORING, null);
            statusUpdateService.sendStatusUpdate(
                    result.getContentId(), "STORING", "파일 저장 중...");

            // 5. 저장 완료
            uploadService.updateStatus(result.getContentId(),
                    ContentMessage.ProcessingStatus.STORED, null);
            statusUpdateService.sendStatusUpdate(
                    result.getContentId(), "STORED", "파일이 저장소에 저장되었습니다.");

            // 6. 최종 완료
            uploadService.updateStatus(result.getContentId(),
                    ContentMessage.ProcessingStatus.COMPLETED, null);
            statusUpdateService.sendStatusUpdate(
                    result.getContentId(), "COMPLETED", "모든 처리가 완료되었습니다.");

        } catch (Exception e) {
            log.error("파일 처리 중 오류", e);
            handleFailure(result.getContentId(), e.getMessage());
        }
    }

    private void handleValidationFailure(ValidationResult result) {
        fileStorageService.deleteFile(result.getFilePath());
        uploadService.updateStatus(result.getContentId(),
                ContentMessage.ProcessingStatus.INVALID, result.getErrorMessage());
        statusUpdateService.sendStatusUpdate(
                result.getContentId(), "INVALID",
                "파일 검증에 실패했습니다: " + result.getErrorMessage());
    }

    private void handleFailure(UUID contentId, String errorMessage) {
        uploadService.updateStatus(contentId,
                ContentMessage.ProcessingStatus.FAILED, errorMessage);
        statusUpdateService.sendStatusUpdate(
                contentId, "FAILED", "처리 중 오류 발생: " + errorMessage);
    }
}