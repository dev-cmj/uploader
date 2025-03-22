package com.project.upload.entity;

import com.project.common.model.ContentStatus;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(
        name = "content_status",
        indexes = {
                @Index(name = "idx_content_id", columnList = "content_id", unique = true),
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_created_at", columnList = "created_at")
        }
)
public class ContentStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_id", nullable = false, length = 50)
    private String contentId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ContentStatus status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "access_url", length = 1000)
    private String accessUrl;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    // 기본 생성자
    public ContentStatusEntity() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
}