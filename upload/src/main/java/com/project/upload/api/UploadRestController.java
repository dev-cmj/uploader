package com.project.upload.api;

import com.project.common.constants.RabbitMQConstants;
import com.project.common.model.ContentMessage;
import com.project.upload.service.FileStorageService;
import com.project.upload.service.StatusUpdateService;
import com.project.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
@Slf4j
public class UploadRestController {

    private final UploadService uploadService;
    private final FileStorageService fileStorageService;
    private final StatusUpdateService statusUpdateService;
    private final RabbitTemplate rabbitTemplate;

    @PostMapping
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam(value = "priority", defaultValue = "NORMAL") String priority) {

        try {
            log.info("File upload request received: {}, size: {}", file.getOriginalFilename(), file.getSize());

            // 1. 컨텐츠 ID 생성
            UUID contentId = UUID.randomUUID();

            // 2. 상태 초기화 - 검증 대기 중
            statusUpdateService.sendStatusUpdate(contentId, "UPLOADED", "파일 업로드가 완료되었습니다. 검증 대기 중...");

            // 3. 임시 디렉토리에 파일 저장
            String tempFilePath = fileStorageService.storeTemporaryFile(file);

            ContentMessage message = uploadService.processUpload(
                    contentId, userId, file.getOriginalFilename(),
                    file.getContentType(), file.getSize(), tempFilePath, priority
            );

            // 5. RabbitMQ로 검증 요청 전송
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.CONTENT_EXCHANGE,
                    RabbitMQConstants.VALIDATION_REQUEST_ROUTING_KEY,
                    message);

            log.info("Validation request sent for content: {}, path: {}", contentId, tempFilePath);

            // 6. 응답 생성
            Map<String, Object> response = new HashMap<>();
            response.put("id", contentId);
            response.put("fileName", file.getOriginalFilename());
            response.put("status", "UPLOADED");
            response.put("message", "파일 업로드가 완료되었으며 검증이 진행 중입니다");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error uploading file", e);

            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            response.put("status", "FAILED");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUploadStatus(@PathVariable("id") UUID id) {
        try {
            ContentMessage message = uploadService.getUploadStatus(id);

            Map<String, Object> response = new HashMap<>();
            response.put("id", message.getId());
            response.put("fileName", message.getFileName());
            response.put("status", message.getStatus());
            response.put("createdAt", message.getCreatedAt());
            response.put("updatedAt", message.getUpdatedAt());
            response.put("statusMessage", getStatusMessage(message));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting upload status", e);

            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    private String getStatusMessage(ContentMessage message) {
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