package com.moekr.shadow.node.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class FunctionWrapper<T, R> {
	private Function<T, R> function;
	private Supplier<R> supplier;
	private Consumer<T> consumer;
	private Method method;

	public FunctionWrapper(Function<T, R> function) {
		this.function = function;
	}

	public FunctionWrapper(Supplier<R> supplier) {
		this.supplier = supplier;
	}

	public FunctionWrapper(Consumer<T> consumer) {
		this.consumer = consumer;
	}

	public FunctionWrapper(Method method) {
		this.method = method;
	}

	public R invoke(T payload) {
		if (function != null) {
			return function.apply(payload);
		} else if (supplier != null) {
			return supplier.get();
		} else if (consumer != null) {
			consumer.accept(payload);
		} else if (method != null) {
			method.invoke();
		}
		return null;
	}

	@FunctionalInterface
	public interface Method {
		void invoke();
	}
}
