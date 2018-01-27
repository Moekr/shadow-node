package com.moekr.shadow.node.web.controller;

import com.moekr.shadow.node.util.ToolKit;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class GlobalErrorController implements ErrorController {
	private static final String ERROR_PATH = "/error";

	@RequestMapping(ERROR_PATH)
	public Map<String, Object> error(HttpServletRequest request) {
		HttpStatus httpStatus = ToolKit.httpStatus(request);
		return ToolKit.assemblyResponseBody(httpStatus.value(), httpStatus.getReasonPhrase());
	}

	@Override
	public String getErrorPath() {
		return ERROR_PATH;
	}
}
