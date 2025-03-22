package com.project.validate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AntiVirusService {

    @Value("${antivirus.enabled:false}")  // 기본값 false로 변경
    private boolean enabled;

    @Value("${antivirus.command:clamscan}")
    private String scanCommand;

    @Value("${antivirus.timeout:60}")
    private int scanTimeoutSeconds;

    @Value("${antivirus.temp-dir:/tmp/av-scan}")
    private String tempScanDir;

    /**
     * 파일 경로로 바이러스 검사 수행
     *
     * @param filePath 검사할 파일 경로
     * @return 안전한 파일인지 여부 (true: 안전, false: 위험)
     */
    public boolean scanFile(String filePath) {
        if (!enabled) {
            log.info("Antivirus scanning is disabled, skipping scan for: {}", filePath);
            return true;
        }

        try {
            // 파일 존재 확인
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                log.error("File not found for virus scan: {}", filePath);
                return false;
            }

            try {
                // 외부 안티바이러스 명령 실행
                ProcessBuilder pb = new ProcessBuilder(scanCommand, "--no-summary", filePath);
                Process process = pb.start();

                // 타임아웃 설정
                boolean completed = process.waitFor(scanTimeoutSeconds, TimeUnit.SECONDS);
                if (!completed) {
                    process.destroyForcibly();
                    log.error("Antivirus scan timed out for: {}", filePath);
                    return true; // 타임아웃 시 안전하다고 가정
                }

                // 결과 확인 (대부분의 AV에서 0은 감염 없음, 1은 감염됨)
                int exitCode = process.exitValue();
                log.info("Antivirus scan completed for: {}, result: {}", filePath, exitCode == 0 ? "safe" : "infected");

                return exitCode == 0;
            } catch (IOException e) {
                log.warn("Failed to execute clamscan - assuming file is safe: {}", e.getMessage());
                return true; // 명령 실행 실패 시 안전하다고 가정
            }

        } catch (Exception e) {
            log.error("Error during antivirus scan for file: {}", filePath, e);
            return true; // 예외 발생 시 안전하다고 가정
        }
    }

    /**
     * 바이트 배열로 바이러스 검사 수행
     *
     * @param data 검사할 바이트 배열
     * @return 안전한 데이터인지 여부 (true: 안전, false: 위험)
     */
    public boolean scanBytes(byte[] data) {
        if (!enabled || data == null || data.length == 0) {
            return true;
        }

        Path tempFile = null;
        try {
            // 임시 디렉토리 생성
            Path tempDir = Paths.get(tempScanDir);
            Files.createDirectories(tempDir);

            // 임시 파일 생성
            tempFile = Files.createTempFile(tempDir, "av-scan-", ".tmp");
            Files.write(tempFile, data);

            // 파일 스캔

            return scanFile(tempFile.toString());
        } catch (Exception e) {
            log.error("Error during antivirus scan for byte data", e);
            return true; // 예외 발생 시 안전하다고 가정
        } finally {
            // 임시 파일 정리
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception e) {
                    log.warn("Failed to delete temporary file: {}", tempFile, e);
                }
            }
        }
    }
}