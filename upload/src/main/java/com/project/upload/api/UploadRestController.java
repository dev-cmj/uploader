package com.project.upload.api;

import com.project.common.constants.RabbitMQConstants;
import com.project.common.model.ContentMessage;
import com.project.common.model.ContentPriority;
import com.project.common.model.ContentStatus;
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


@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadRestController {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 청크 기반 대용량 파일 업로드 API
     * 파일을 바로 스토리지로 스트리밍하고 검증 요청을 발행
     */
    @PostMapping("/stream")
    public ResponseEntity<Map<String, Object>> streamUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam(value = "priority", defaultValue = "LOW") String priority,
            @RequestParam(value = "chunkIndex", defaultValue = "0") int chunkIndex,
            @RequestParam(value = "totalChunks", defaultValue = "1") int totalChunks,
            @RequestParam(value = "contentId", required = false) String contentIdStr) {

        try {
            log.info("File chunk upload: {} [{}/{}], size: {}",
                    file.getOriginalFilename(), chunkIndex + 1, totalChunks, file.getSize());

            log.info("userId: {}, priority: {}", userId, priority);

            // 첫번째 청크인 경우 새 contentId 생성
            UUID contentId = (contentIdStr == null || contentIdStr.isEmpty())
                    ? UUID.randomUUID() : UUID.fromString(contentIdStr);

            // 파일 정보 준비
            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();

            // 청크 업로드 메시지 생성
            ContentMessage message = createStreamingMessage(
                    contentId, userId, originalFilename, contentType,
                    file.getBytes(), file.getSize(), chunkIndex, totalChunks, priority);

            // 저장 서비스로 바로 스트리밍
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.CONTENT_EXCHANGE,
                    RabbitMQConstants.CHUNK_STORAGE_ROUTING_KEY,
                    message);

            // 마지막 청크인 경우 검증 요청 발행
            if (chunkIndex == totalChunks - 1) {
                // 상태 업데이트
                message.setStatus(ContentStatus.UPLOADED);
                rabbitTemplate.convertAndSend(
                        RabbitMQConstants.CONTENT_EXCHANGE,
                        RabbitMQConstants.STATUS_UPDATE_ROUTING_KEY,
                        message);

                // 검증 요청
                rabbitTemplate.convertAndSend(
                        RabbitMQConstants.CONTENT_EXCHANGE,
                        RabbitMQConstants.VALIDATION_ROUTING_KEY,
                        message);

                log.info("All chunks uploaded, validation request sent for content: {}", contentId);
            }

            // 응답 생성
            Map<String, Object> response = new HashMap<>();
            response.put("id", contentId);
            response.put("fileName", originalFilename);
            response.put("chunkIndex", chunkIndex);
            response.put("totalChunks", totalChunks);
            response.put("status", chunkIndex == totalChunks - 1
                    ? ContentStatus.UPLOADED.name() : ContentStatus.UPLOADING.name());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error uploading file chunk", e);

            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            response.put("status", ContentStatus.FAILED.name());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 소형 파일용 단일 요청 업로드 API
     * 작은 파일은 한 번에 업로드하고 저장/검증 요청
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam(value = "priority", defaultValue = "NORMAL") String priority) {

        try {
            log.info("Small file upload request: {}, size: {}", file.getOriginalFilename(), file.getSize());

            // 컨텐츠 ID 생성
            UUID contentId = UUID.randomUUID();

            // ContentMessage 객체 생성
            ContentMessage message = createStreamingMessage(
                    contentId, userId, file.getOriginalFilename(),
                    file.getContentType(), file.getBytes(), file.getSize(), 0, 1, priority);

            message.setStatus(ContentStatus.UPLOADING);

            // 저장 서비스로 직접 전송
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.CONTENT_EXCHANGE,
                    RabbitMQConstants.CHUNK_STORAGE_ROUTING_KEY,
                    message);

            // 상태 업데이트 (업로드 완료)
            message.setStatus(ContentStatus.UPLOADED);
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.CONTENT_EXCHANGE,
                    RabbitMQConstants.STATUS_UPDATE_ROUTING_KEY,
                    message);

            // 검증 서비스 요청
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.CONTENT_EXCHANGE,
                    RabbitMQConstants.VALIDATION_ROUTING_KEY,
                    message);

            log.info("File uploaded and sent for validation: {}", contentId);

            // 응답 생성
            Map<String, Object> response = new HashMap<>();
            response.put("id", contentId);
            response.put("fileName", file.getOriginalFilename());
            response.put("status", ContentStatus.UPLOADED.name());
            response.put("message", "파일 업로드가 완료되었으며 검증이 진행 중입니다");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error uploading file", e);

            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            response.put("status", ContentStatus.FAILED.name());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private ContentMessage createStreamingMessage(UUID contentId, String userId, String fileName,
                                                  String contentType, byte[] data, long fileSize,
                                                  int chunkIndex, int totalChunks, String priority) {

        ContentMessage message = new ContentMessage();
        message.setId(contentId.toString());
        message.setUserId(userId);
        message.setFileName(fileName);
        message.setContentType(contentType);
        message.setFileSize(fileSize);
        message.setChunkIndex(chunkIndex);
        message.setTotalChunks(totalChunks);
        message.setContentData(data);  // 청크 데이터
        message.setPriority(ContentPriority.valueOf(priority.toUpperCase()));
        message.setStatus(ContentStatus.UPLOADING);
        message.setTimestamp(System.currentTimeMillis());

        return message;
    }
}