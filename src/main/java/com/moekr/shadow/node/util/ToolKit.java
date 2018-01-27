package com.moekr.shadow.node.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class ToolKit {
	public static Map<String, Object> emptyResponseBody() {
		Map<String, Object> responseBody = new HashMap<>();
		responseBody.put("err", 0);
		return responseBody;
	}

	public static Map<String, Object> assemblyResponseBody(Object res) {
		Map<String, Object> responseBody = new HashMap<>();
		responseBody.put("err", 0);
		responseBody.put("res", res);
		return responseBody;
	}

	public static Map<String, Object> assemblyResponseBody(int error, String message) {
		Map<String, Object> responseBody = new HashMap<>();
		responseBody.put("err", error);
		responseBody.put("msg", message);
		return responseBody;
	}

	private static ObjectMapper OBJECT_MAPPER;

	public static <T> T parse(String json, Class<T> clazz) throws IOException {
		if (OBJECT_MAPPER == null) {
			OBJECT_MAPPER = new ObjectMapper();
			OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		}
		return OBJECT_MAPPER.readValue(json, clazz);
	}

	public static HttpStatus httpStatus(HttpServletRequest request) {
		Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
		if (statusCode == null) {
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}
		try {
			return HttpStatus.valueOf(statusCode);
		} catch (Exception e) {
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}
	}
}
