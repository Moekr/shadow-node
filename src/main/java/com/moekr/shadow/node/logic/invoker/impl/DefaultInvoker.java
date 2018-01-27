package com.moekr.shadow.node.logic.invoker.impl;

import com.moekr.shadow.node.logic.invoker.InvokerAdapter;
import com.moekr.shadow.node.logic.invoker.InvokerConfiguration;
import com.moekr.shadow.node.logic.vo.Configuration;
import com.moekr.shadow.node.logic.vo.Statistic;
import com.moekr.shadow.node.logic.vo.Status;
import com.moekr.shadow.node.util.ServiceException;
import com.moekr.shadow.node.util.ToolKit;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@CommonsLog
public class DefaultInvoker extends InvokerAdapter {
	private InvokerConfiguration invokerConfiguration;
	private Configuration configuration;
	private Process shadowProcess;

	public DefaultInvoker() {
		List<String> output;
		try {
			output = exec("iptables -V");
		} catch (Throwable e) {
			log.fatal("Fail to detect iptables, please check iptables support", e);
			throw new UnsupportedOperationException("Fail to detect iptables, please check iptables support", e);
		}
		if (!output.isEmpty() && output.get(0).contains("iptables")) {
			log.info("Using " + output.get(0));
		} else {
			log.fatal("Fail to detect iptables, please check iptables support");
			throw new UnsupportedOperationException("Fail to detect iptables, please check iptables support");
		}
	}

	@Override
	public void setInvokerConfiguration(InvokerConfiguration invokerConfiguration) {
		this.invokerConfiguration = invokerConfiguration;
	}

	@Override
	public void start() {
		if (configuration == null) {
			throw new ServiceException(ServiceException.NOT_CONFIGURED);
		}
		if (shadowProcess != null && shadowProcess.isAlive()) {
			throw new ServiceException(ServiceException.INOPERABLE_STATUS, "Shadow process is already running");
		}
		String chain = invokerConfiguration.getProperties().getOrDefault("iptables-chain", "shadow-node");
		exec("iptables -F " + chain);
		Set<Integer> portSet = configuration.getPortPassword().keySet();
		for (int port : portSet) {
			exec("iptables -A " + chain + " -p tcp --dport " + port);
		}
		String command = invokerConfiguration.getPythonExecutable() + " " +
				invokerConfiguration.getShadowExecutable() + " " +
				"-c " + invokerConfiguration.getConfLocation();
		try {
			shadowProcess = Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			log.error("Failed to start shadow process", e);
			throw new ServiceException(ServiceException.NATIVE_INVOKE_FAILED,
					"Failed to start shadow process with exception: " + e.getMessage());
		}
	}

	@Override
	public void stop() {
		if (shadowProcess == null || !shadowProcess.isAlive()) {
			throw new ServiceException(ServiceException.INOPERABLE_STATUS, "Shadow process is not running");
		}
		shadowProcess.destroy();
	}

	@Override
	public void restart() {
		stop();
		start();
	}

	@Override
	public Status status() {
		if (shadowProcess != null && shadowProcess.isAlive()) {
			return Status.RUNNING;
		} else if (configuration != null) {
			return Status.IDLE;
		} else {
			return Status.NO_CONF;
		}
	}

	@Override
	public void conf(String conf) {
		try {
			configuration = ToolKit.parse(conf, Configuration.class);
		} catch (IOException e) {
			log.error("Failed to parse shadow conf", e);
			throw new ServiceException(e);
		}
		configuration.validate();
		try {
			writeConf(generateConf(configuration));
		} catch (IOException | JSONException e) {
			log.error("Failed to write shadow conf", e);
			throw new ServiceException(e);
		}
	}

	@Override
	public Statistic statistic() {
		String chain = invokerConfiguration.getProperties().getOrDefault("iptables-chain", "shadow-node");
		String command = "iptables -nvxL " + chain;
		List<String> output = exec(command);
		Map<Integer, Long> traffic = new HashMap<>();
		output.stream().skip(2).forEach(row -> {
			List<String> columns = Arrays.stream(row.split(" ")).filter(StringUtils::isNotBlank).collect(Collectors.toList());
			traffic.put(Integer.valueOf(columns.get(9).split(":")[1]), Long.valueOf(columns.get(1)));
		});
		Statistic statistic = new Statistic();
		statistic.setTraffic(traffic);
		return statistic;
	}

	private JSONObject generateConf(Configuration configuration) throws JSONException {
		JSONObject conf = new JSONObject();
		conf.put("server", "0.0.0.0");
		conf.put("server_ipv6", "[::]");
		conf.put("local_address", "127.0.0.1");
		conf.put("local_port", 1080);
		conf.put("timeout", 300);
		conf.put("method", configuration.getMethod());
		conf.put("protocol", configuration.getProtocol());
		conf.put("protocol_param", configuration.getProtocolParam());
		conf.put("obfs", configuration.getObfs());
		conf.put("obfs_param", configuration.getObfsParam());
		conf.put("redirect", "");
		conf.put("dns_ipv6", false);
		conf.put("fast_open", false);
		conf.put("workers", 1);
		JSONObject portPassword = new JSONObject();
		for (Map.Entry<Integer, String> entry : configuration.getPortPassword().entrySet()) {
			portPassword.put(String.valueOf(entry.getKey()), entry.getValue());
		}
		conf.put("port_password", portPassword);
		for (Map.Entry<String, Object> entry : invokerConfiguration.getConfOverride().entrySet()) {
			conf.put(entry.getKey(), entry.getValue());
		}
		return conf;
	}

	private void writeConf(JSONObject conf) throws IOException, JSONException {
		File confFile = new File(invokerConfiguration.getConfLocation());
		BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(confFile)));
		bufferedWriter.write(conf.toString(4));
		bufferedWriter.flush();
		bufferedWriter.close();
	}

	private List<String> exec(String command) {
		log.debug("Execute command: " + command);
		try {
			Process process = Runtime.getRuntime().exec(command);
			if (process.waitFor() == 0) {
				log.debug("Command stdout:");
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				List<String> output = reader.lines().peek(log::debug).collect(Collectors.toList());
				reader.close();
				return output;
			} else {
				log.error("Failed to invoke command with exit value " + process.exitValue());
				log.error("command stderr:");
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				StringBuilder builder = new StringBuilder();
				reader.lines().peek(log::error).forEach(line -> builder.append(line).append('\n'));
				reader.close();
				throw new ServiceException(ServiceException.NATIVE_INVOKE_FAILED,
						"Failed to invoke command: [" + command + "] with stderr: " + builder.toString());
			}
		} catch (IOException | InterruptedException e) {
			log.error("Failed to invoke command", e);
			throw new ServiceException(ServiceException.NATIVE_INVOKE_FAILED,
					"Failed to invoke command: [" + command + "] with exception: " + e.getMessage());
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if (shadowProcess != null && shadowProcess.isAlive()) {
			shadowProcess.destroy();
		}
	}
}
