package com.project.upload.service;

import com.project.common.constants.RabbitMQConstants;
import com.project.common.model.ContentMessage;
import com.project.upload.entity.UploadEntity;
import com.project.upload.entity.UploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.project.common.model.ContentMessage.determineFileType;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    private final RabbitTemplate rabbitTemplate;
    private final UploadRepository uploadRepository;

    @Transactional
    public ContentMessage processUpload(UUID contentId, String userId, String fileName, String contentType,
                                        Long fileSize, String filePath, String priority) {

        // contentId를 사용하여 메시지 생성
        ContentMessage message = ContentMessage.builder()
                .id(contentId)
                .correlationId(contentId)
                .userId(userId)
                .fileName(fileName)
                .contentType(contentType)
                .fileType(determineFileType(contentType))
                .fileSize(fileSize)
                .sourcePath(filePath)
                .status(ContentMessage.ProcessingStatus.VALIDATED) // 이미 검증된 상태
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // DB에 업로드 정보 저장
        UploadEntity entity = new UploadEntity();
        entity.setId(contentId);
        entity.setUserId(userId);
        entity.setFileName(fileName);
        entity.setContentType(contentType);
        entity.setFileType(message.getFileType());
        entity.setFileSize(fileSize);
        entity.setFilePath(filePath);
        entity.setStatus(message.getStatus().name());
        entity.setCreatedAt(message.getCreatedAt());
        entity.setUpdatedAt(message.getUpdatedAt());

        uploadRepository.save(entity);

        return message;
    }

    public ContentMessage getUploadStatus(UUID id) {
        UploadEntity entity = uploadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Upload not found with id: " + id));

        return mapEntityToMessage(entity);
    }

    private ContentMessage mapEntityToMessage(UploadEntity entity) {
        return ContentMessage.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .fileName(entity.getFileName())
                .contentType(entity.getContentType())
                .fileType(entity.getFileType())
                .fileSize(entity.getFileSize())
                .sourcePath(entity.getFilePath())
                .status(ContentMessage.ProcessingStatus.valueOf(entity.getStatus()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private int determinePriority(String priority) {
        return switch (priority.toUpperCase()) {
            case "HIGH" -> RabbitMQConstants.PRIORITY_HIGH;
            case "LOW" -> RabbitMQConstants.PRIORITY_LOW;
            default -> RabbitMQConstants.PRIORITY_NORMAL;
        };
    }
}