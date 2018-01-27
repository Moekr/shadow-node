package com.moekr.shadow.node.logic.invoker;

import com.moekr.shadow.node.logic.invoker.impl.DefaultInvoker;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode
@ToString
@Configuration
@ConfigurationProperties("shadow.invoker")
public class InvokerConfiguration {
	private String impl = DefaultInvoker.class.getName();
	private String pythonExecutable = "/usr/bin/env python";
	private String shadowExecutable = "/usr/local/shadow/server.py";
	private String confLocation = "/etc/shadow.conf";
	private Map<String, Object> confOverride = new HashMap<>();
	private Map<String, String> properties = new HashMap<>();

	@Bean
	public Invoker invoker() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		Class<?> clazz = Class.forName(impl);
		if (!Invoker.class.isAssignableFrom(clazz)) {
			throw new ClassCastException("Can't use class [" + impl + " ] as an invoker implementation");
		}
		Invoker invoker = (Invoker) clazz.newInstance();
		invoker.setInvokerConfiguration(this);
		return invoker;
	}
}
