package com.project.upload.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "uploads")
public class UploadEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String status;

    @Column
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}