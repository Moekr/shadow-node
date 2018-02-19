package com.moekr.shadow.node;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Data
@ToString
@Configuration
@ConfigurationProperties("shadow.invoker")
public class InvokerConfiguration {
	private Integer nodeId;
	private String executable = "/bin/bash /usr/local/shadow/run.sh";
	private String confLocation = "/usr/local/shadow/mudb.json";

	@Bean
	public ScheduledExecutorService scheduledExecutor() {
		return Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 4);
	}
}
