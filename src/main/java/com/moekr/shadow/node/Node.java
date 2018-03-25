package com.moekr.shadow.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode
@ToString
class Node {
	private Boolean enable;
	private Integer port;
	private String password;
	private String method;
	private String protocol;
	private String obfs;
	private String obfsParam;
	private List<User> users = new ArrayList<>();

	@Data
	@EqualsAndHashCode
	@ToString
	static class User {
		private Integer id;
		private String token;
	}
}
