package com.moekr.shadow.node.logic.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode
@ToString
public class Status {
	private ProcessStatus status;

	public enum ProcessStatus {
		NO_CONF, IDLE, RUNNING
	}
}
