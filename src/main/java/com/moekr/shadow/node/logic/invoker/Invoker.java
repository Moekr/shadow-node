package com.moekr.shadow.node.logic.invoker;

import com.moekr.shadow.node.logic.service.RpcService;
import com.moekr.shadow.node.logic.vo.Statistic;
import com.moekr.shadow.node.logic.vo.Status;

public interface Invoker {
	void setRpcService(RpcService rpcService);

	void setInvokerConfiguration(InvokerConfiguration invokerConfiguration);

	void start();

	void stop();

	void restart();

	Status status();

	void conf(String conf);

	Statistic statistic();
}
