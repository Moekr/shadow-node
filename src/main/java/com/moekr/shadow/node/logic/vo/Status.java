package com.moekr.shadow.node.logic.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode
@ToString
public class Status {
	private Boolean running;
	private Configuration configuration;
}
