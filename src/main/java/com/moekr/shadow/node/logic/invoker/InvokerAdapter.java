package com.moekr.shadow.node.logic.invoker;

import com.moekr.shadow.node.logic.service.RpcService;
import com.moekr.shadow.node.logic.vo.Statistic;
import com.moekr.shadow.node.logic.vo.Status;

public abstract class InvokerAdapter implements Invoker {
	@Override
	public void setRpcService(RpcService rpcService) {
	}

	@Override
	public void setInvokerConfiguration(InvokerConfiguration invokerConfiguration) {
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void restart() {
	}

	@Override
	public Status status() {
		return null;
	}

	@Override
	public void conf(String conf) {
	}

	@Override
	public Statistic statistic() {
		return null;
	}
}
