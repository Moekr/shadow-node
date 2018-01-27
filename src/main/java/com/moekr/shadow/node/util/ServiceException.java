package com.moekr.shadow.node.util;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ServiceException extends RuntimeException {
	private static final Map<Integer, String> ERROR_MESSAGE_MAP = new HashMap<>();
	private static final int UNSPECIFIC_INTERNAL_ERROR = 500;
	private static final String UNKNOWN_ERROR_MESSAGE = "Unknown error";

	public static final int OPERATION_UNSUPPORTED = 100;
	public static final int NOT_CONFIGURED = 200;
	public static final int INOPERABLE_STATUS = 300;
	public static final int NATIVE_INVOKE_FAILED = 400;

	static {
		ERROR_MESSAGE_MAP.put(OPERATION_UNSUPPORTED, "Operation is unsupported, use 'command' to list available commands");
		ERROR_MESSAGE_MAP.put(NOT_CONFIGURED, "Node is not configured, use 'conf' to issue a configuration");
		ERROR_MESSAGE_MAP.put(INOPERABLE_STATUS, "Operation can't run in the current status, use 'status' to check node status");
		ERROR_MESSAGE_MAP.put(NATIVE_INVOKE_FAILED, "Native invocation is failed, check invoker configuration and system support");
	}

	private int error;

	public ServiceException(Throwable throwable) {
		super(throwable);
		this.error = UNSPECIFIC_INTERNAL_ERROR;
	}

	public ServiceException(int error) {
		this(error, ERROR_MESSAGE_MAP.getOrDefault(error, UNKNOWN_ERROR_MESSAGE));
	}

	public ServiceException(int error, String message) {
		super(message);
		this.error = error;
	}

	@Override
	public String getMessage() {
		String message = super.getMessage();
		if (message == null) {
			message = super.getCause().getMessage();
		}
		if (message == null) {
			message = super.getCause().getClass().getName();
		}
		return message;
	}
}
