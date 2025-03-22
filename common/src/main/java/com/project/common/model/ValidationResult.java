package com.project.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult implements Serializable {
    private UUID contentId;
    private String userId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String filePath;
    private String priority;
    private boolean valid;
    private String errorMessage;
}