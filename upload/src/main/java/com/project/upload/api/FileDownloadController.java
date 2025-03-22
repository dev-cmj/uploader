package com.project.upload.api;

import com.project.upload.entity.UploadEntity;
import com.project.upload.entity.UploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FileDownloadController {

    private final UploadRepository uploadRepository;

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("id") UUID id) {
        try {
            // 파일 정보 조회
            UploadEntity entity = uploadRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "파일을 찾을 수 없습니다: " + id));

            // 파일 경로
            Path filePath = Paths.get(entity.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new ResponseStatusException(NOT_FOUND, "파일을 찾을 수 없습니다: " + filePath);
            }

            // 원본 파일명 추출
            String encodedFilename = URLEncoder.encode(entity.getFileName(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            String contentDisposition = "attachment; filename*=UTF-8''" + encodedFilename;

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(entity.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("파일 다운로드 중 오류", e);
            throw new ResponseStatusException(NOT_FOUND, "파일을 찾을 수 없습니다");
        }
    }
}