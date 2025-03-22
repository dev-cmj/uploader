package com.project.upload.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UploadRepository extends JpaRepository<UploadEntity, UUID> {

    List<UploadEntity> findByUserId(String userId);

    List<UploadEntity> findByStatus(String status);
}