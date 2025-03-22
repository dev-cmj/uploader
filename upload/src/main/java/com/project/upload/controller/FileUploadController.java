package com.project.upload.controller;

import com.project.common.constants.RabbitMQConstants;
import com.project.common.model.ContentMessage;
import com.project.common.model.ContentStatus;
import com.project.upload.dto.ContentStatusDTO;
import com.project.upload.service.StatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final StatusService statusService;

    @GetMapping
    public String showUploadForm(Model model) {
        // 현재 로그인한 사용자 ID 가져오기
        String userId = "user1";

        // 사용자의 파일 업로드 상태 목록 가져오기
        List<ContentStatusDTO> uploadList = statusService.getUserContentStatus(userId);

        model.addAttribute("userId", userId);
        model.addAttribute("uploadList", uploadList);
        return "file-upload";
    }

}