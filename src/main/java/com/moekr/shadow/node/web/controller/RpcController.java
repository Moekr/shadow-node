package com.moekr.shadow.node.web.controller;

import com.moekr.shadow.node.logic.service.RpcService;
import com.moekr.shadow.node.util.ToolKit;
import com.moekr.shadow.node.web.dto.Instruction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

@RestController
public class RpcController {
	private final RpcService rpcService;

	@Autowired
	public RpcController(RpcService rpcService) {
		this.rpcService = rpcService;
	}

	@PostMapping("/rpc")
	public Map<String, Object> call(@RequestBody @Valid Instruction instruction) {
		Object result = rpcService.call(instruction);
		if (result == null) {
			return ToolKit.emptyResponseBody();
		}
		return ToolKit.assemblyResponseBody(result);
	}
}
