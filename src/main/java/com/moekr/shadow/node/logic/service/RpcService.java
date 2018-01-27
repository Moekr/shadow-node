package com.moekr.shadow.node.logic.service;

import com.moekr.shadow.node.logic.invoker.Invoker;
import com.moekr.shadow.node.util.FunctionWrapper;
import com.moekr.shadow.node.web.dto.Instruction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class RpcService {
	private static final String RPC_VERSION = "1";

	private final Map<String, FunctionWrapper<String, Object>> functionMap = new HashMap<>();

	@Autowired
	public RpcService(Invoker invoker) {
		registerCommand("command", new FunctionWrapper<>(this::command));
		registerCommand("version", new FunctionWrapper<>(this::version));
		registerCommand("start", new FunctionWrapper<>(invoker::start));
		registerCommand("stop", new FunctionWrapper<>(invoker::stop));
		registerCommand("restart", new FunctionWrapper<>(invoker::restart));
		registerCommand("status", new FunctionWrapper<>(invoker::status));
		registerCommand("conf", new FunctionWrapper<>(invoker::conf));
		registerCommand("statistic", new FunctionWrapper<>(invoker::statistic));
		invoker.setRpcService(this);
	}

	public void registerCommand(String command, FunctionWrapper<String, Object> function) {
		functionMap.put(command, function);
	}

	public Object call(Instruction instruction) {
		FunctionWrapper<String, Object> function = functionMap.get(instruction.getCommand());
		return function.invoke(instruction.getPayload());
	}

	private Set<String> command() {
		return functionMap.keySet();
	}

	private String version() {
		return RPC_VERSION;
	}
}
