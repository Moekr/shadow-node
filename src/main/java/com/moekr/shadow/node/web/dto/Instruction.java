package com.moekr.shadow.node.web.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode
@ToString
public class Instruction {
	@NotNull(message = "[command] is not provided")
	private String command;
	private String payload;
}
