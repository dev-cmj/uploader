package com.project.storage.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StorageRepository extends JpaRepository<StorageEntity, UUID> {

    List<StorageEntity> findByUserId(String userId);

    List<StorageEntity> findByStatus(String status);
}