package com.project.storage.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String secretKey;

    @Value("${minio.bucket-name:content-pipeline}")
    private String bucketName;

    @Value("${minio.temp-bucket-name:temp-uploads}")
    private String tempBucketName;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    public boolean initializeMinio(MinioClient minioClient) {
        try {
            // 1. 메인 버킷 초기화
            initializeBucket(minioClient, bucketName);

            // 2. 임시 버킷 초기화
            initializeBucket(minioClient, tempBucketName);

            return true;
        } catch (Exception e) {
            log.error("Error initializing MinIO buckets", e);
            return false;
        }
    }

    private void initializeBucket(MinioClient client, String bucket) {
        try {
            // 버킷이 존재하는지 확인
            boolean bucketExists = client.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucket)
                            .build()
            );

            // 버킷이 없으면 생성
            if (!bucketExists) {
                client.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucket)
                                .build()
                );
                log.info("Created MinIO bucket: {}", bucket);
            } else {
                log.info("MinIO bucket already exists: {}", bucket);
            }
        } catch (Exception e) {
            log.error("Error initializing MinIO bucket: {}", bucket, e);
        }
    }

    @Bean
    public String bucketName() {
        return bucketName;
    }

    @Bean
    public String tempBucketName() {
        return tempBucketName;
    }
}