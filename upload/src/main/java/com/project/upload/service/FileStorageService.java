package com.project.upload.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;



@Service
@Slf4j
public class FileStorageService {

    private final Path fileStorageLocation;
    private final String uploadDir;  // 추가된 필드

    public FileStorageService(@Value("${file.upload-dir:./uploads/temp}") String uploadDir) {
        this.uploadDir = uploadDir;  // 필드 초기화
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        // 원래 파일명 정리
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        // 저장할 파일명 생성 (UUID + 원래 확장자)
        String fileExtension = "";
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            fileExtension = originalFileName.substring(lastDotIndex);
        }
        String fileName = UUID.randomUUID().toString() + fileExtension;

        try {
            // 파일명에 잘못된 문자가 있는지 확인
            if (fileName.contains("..")) {
                throw new RuntimeException("Filename contains invalid path sequence " + fileName);
            }

            // 파일 저장 경로 생성
            Path targetLocation = this.fileStorageLocation.resolve(fileName);

            // 파일 저장
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return targetLocation.toString();
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public void deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            // 파일 삭제 실패 로그만 남기고 예외는 던지지 않음
            System.err.println("Could not delete file " + filePath + ": " + ex.getMessage());
        }
    }

    public String storeTemporaryFile(MultipartFile file) {
        // 임시 디렉토리에 저장
        String tempDir = uploadDir + "/temp";
        Path tempDirPath = Paths.get(tempDir);

        try {
            Files.createDirectories(tempDirPath);
            String fileName = UUID.randomUUID() + getFileExtension(file.getOriginalFilename());
            Path targetPath = tempDirPath.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toString();
        } catch (IOException e) {
            throw new RuntimeException("임시 파일 저장 실패", e);
        }
    }


    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex);
        }
        return "";
    }

    public String moveToFinalLocation(String tempFilePath) {
        try {
            Path source = Paths.get(tempFilePath);
            String fileName = source.getFileName().toString();
            Path destination = Paths.get(uploadDir).resolve(fileName);
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            return destination.toString();
        } catch (IOException e) {
            throw new RuntimeException("파일 이동 실패", e);
        }
    }
}