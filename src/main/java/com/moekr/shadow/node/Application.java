package com.moekr.shadow.node;

import com.alibaba.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@DubboComponentScan("com.moekr.shadow.node")
public class Application extends SpringApplication {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
