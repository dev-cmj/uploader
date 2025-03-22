package com.project.upload.dto;

import com.project.common.model.ContentStatus;
import lombok.Data;

@Data
public class ContentStatusDTO {
    private String contentId;
    private String userId;
    private String fileName;
    private ContentStatus status;
    private String errorMessage;
    private String accessUrl;
    private Long createdAt;
    private Long updatedAt;
}