package com.moekr.shadow.node.logic.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

@Data
@EqualsAndHashCode
@ToString
public class Statistic {
	private Map<Integer, Long> traffic;
}
