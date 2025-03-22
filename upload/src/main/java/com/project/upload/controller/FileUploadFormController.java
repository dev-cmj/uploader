package com.project.upload.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FileUploadFormController {

    @GetMapping("/upload-form")
    public String showUploadForm(Model model) {
        return "upload-form";
    }
}