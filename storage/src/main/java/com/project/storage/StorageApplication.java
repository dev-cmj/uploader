package com.project.storage;

import com.project.common.config.RabbitMQConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RabbitMQConfig.class)
public class StorageApplication {

	public static void main(String[] args) {
		SpringApplication.run(StorageApplication.class, args);
	}

}
