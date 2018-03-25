package com.moekr.shadow.node;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Application extends SpringApplication {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
