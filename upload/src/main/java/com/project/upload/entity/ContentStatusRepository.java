package com.project.upload.entity;

import com.project.common.model.ContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContentStatusRepository extends JpaRepository<ContentStatusEntity, Long> {

    Optional<ContentStatusEntity> findByContentId(String contentId);

    List<ContentStatusEntity> findByUserId(String userId);

    List<ContentStatusEntity> findByUserIdAndStatus(String userId, ContentStatus status);

    List<ContentStatusEntity> findByStatus(ContentStatus status);

    List<ContentStatusEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}