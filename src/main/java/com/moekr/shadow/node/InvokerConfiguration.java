package com.moekr.shadow.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@EqualsAndHashCode
@ToString
@Configuration
@ConfigurationProperties("shadow.invoker")
public class InvokerConfiguration {
	private Integer nodeId;
	private Panel panel = new Panel();
	private String executable = "/usr/bin/env bash /usr/local/shadow/run.sh";
	private String confLocation = "/usr/local/shadow/mudb.json";

	@Data
	@EqualsAndHashCode
	@ToString
	public static class Panel {
		private String url;
		private String username;
		private String password;
	}
}
