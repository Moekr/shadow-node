package com.moekr.shadow.node;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.HttpUriRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.Charset;

@Configuration
public class RestTemplateConfiguration {
	private final InvokerConfiguration configuration;

	@Autowired
	public RestTemplateConfiguration(InvokerConfiguration configuration) {
		this.configuration = configuration;
	}

	@Bean
	public RestTemplate restTemplate() {
		ClientHttpRequestFactory factory = new CustomHttpComponentsClientHttpRequestFactory();
		return new RestTemplate(factory);
	}

	private class CustomHttpComponentsClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory {
		private final String authorization;

		CustomHttpComponentsClientHttpRequestFactory() {
			Base64 base64 = new Base64(0, null, false);
			String str = configuration.getPanel().getUsername() + ":" + configuration.getPanel().getPassword();
			authorization = "Basic " + base64.encodeToString(str.getBytes(Charset.forName("UTF-8")));
		}

		@Override
		protected HttpUriRequest createHttpUriRequest(HttpMethod httpMethod, URI uri) {
			HttpUriRequest request = super.createHttpUriRequest(httpMethod, uri);
			request.setHeader(HttpHeaders.AUTHORIZATION, authorization);
			return request;
		}
	}
}
