package com.project.validate.config;

import com.project.common.model.FileType;
import com.project.validate.validator.FileValidator;
import com.project.validate.validator.ImageValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ValidatorConfig {

    @Bean
    public Map<FileType, FileValidator> validatorMap(ImageValidator imageValidator) {
        Map<FileType, FileValidator> validators = new HashMap<>();
        validators.put(FileType.IMAGE, imageValidator);

        // 나머지 validator 구현체는 추후 추가

        return validators;
    }
}