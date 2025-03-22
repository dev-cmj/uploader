package com.project.upload.controller;

import com.project.upload.entity.UploadEntity;
import com.project.upload.entity.UploadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class FileUploadFormController {

    private final UploadRepository uploadRepository;

    @GetMapping("/list")
    public String listUploadedFiles(Model model) {
        List<UploadEntity> uploads = uploadRepository.findAll();
        model.addAttribute("uploads", uploads);
        return "file-list";
    }

    @GetMapping("/file/{id}")
    public String viewFile(@PathVariable("id") UUID id, Model model) {
        // 파일 정보 조회
        UploadEntity file = uploadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다: " + id));

        model.addAttribute("file", file);

        // 상태에 따른 메시지 설정
        String statusMessage = getStatusMessage(file.getStatus(), file.getErrorMessage());
        model.addAttribute("statusMessage", statusMessage);

        return "file-detail";
    }

    private String getStatusMessage(String status, String errorMessage) {
        return switch (status) {
            case "UPLOADED" -> "파일이 업로드되었습니다.";
            case "VALIDATING" -> "파일 검증 중입니다...";
            case "VALIDATED" -> "파일 검증이 완료되었습니다.";
            case "INVALID" -> "파일이 유효하지 않습니다: " + (errorMessage != null ? errorMessage : "");
            case "PROCESSING" -> "파일 처리 중입니다...";
            case "PROCESSED" -> "파일 처리가 완료되었습니다.";
            case "STORING" -> "파일 저장 중입니다...";
            case "STORED" -> "파일이 저장소에 저장되었습니다.";
            case "COMPLETED" -> "모든 처리가 완료되었습니다.";
            case "FAILED" -> "처리 중 오류가 발생했습니다: " + (errorMessage != null ? errorMessage : "");
            default -> "상태: " + status;
        };
    }


    @GetMapping("/upload-form")
    public String showUploadForm(Model model) {
        return "upload-form";
    }
}