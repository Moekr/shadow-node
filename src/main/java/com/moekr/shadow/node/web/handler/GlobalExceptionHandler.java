package com.moekr.shadow.node.web.handler;

import com.moekr.shadow.node.util.ServiceException;
import com.moekr.shadow.node.util.ToolKit;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice("com.moekr.shadow.node.web.controller")
public class GlobalExceptionHandler {
	private static final int DEFAULT_ERROR_CODE = 500;

	@ExceptionHandler(Throwable.class)
	@ResponseBody
	public Map<String, Object> handle(Throwable exception) {
		int error = DEFAULT_ERROR_CODE;
		if (exception instanceof ServiceException) {
			error = ((ServiceException) exception).getError();
		}
		return ToolKit.assemblyResponseBody(error, exception.getMessage());
	}
}
