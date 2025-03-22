package com.project.validate;

import com.project.common.config.RabbitMQConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RabbitMQConfig.class)
public class ValidateApplication {

    public static void main(String[] args) {
        SpringApplication.run(ValidateApplication.class, args);
    }

}
