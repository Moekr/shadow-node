package com.moekr.shadow.node.logic.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.ValidationException;
import java.util.Map;

@Data
@EqualsAndHashCode
@ToString
public class Configuration {
	private String method;
	private String protocol;
	private String protocolParam;
	private String obfs;
	private String obfsParam;
	private Map<Integer, String> portPassword;

	public void validate() {
		if (method == null) {
			throw new ValidationException("[method] is not provided");
		}
		if (protocol == null) {
			throw new ValidationException("[protocol] is not provided");
		}
		if (obfs == null) {
			throw new ValidationException("[obfs] is not provided");
		}
		if (portPassword == null || portPassword.size() == 0) {
			throw new ValidationException("[portPassword] is not provided or is empty");
		}
		if (protocolParam == null) {
			protocolParam = "";
		}
		if (obfsParam == null) {
			obfsParam = "";
		}
	}
}
