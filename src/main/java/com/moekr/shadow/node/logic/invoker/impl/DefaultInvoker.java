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
import org.springframework.scheduling.annotation.Scheduled;

import javax.validation.ValidationException;
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
			log.fatal("Fail to detect iptables, please check iptables support");
			throw new UnsupportedOperationException("Fail to detect iptables, please check iptables support", e);
		}
		if (!output.isEmpty() && output.get(0).contains("iptables")) {
			log.info("Using " + output.get(0));
		} else {
			log.fatal("Fail to detect iptables, please check iptables support");
			throw new UnsupportedOperationException("Fail to detect iptables, please check iptables support");
		}
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownHook));
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
		String command = invokerConfiguration.getPythonExecutable() + " " +
				invokerConfiguration.getShadowExecutable() + " " +
				"-c " + invokerConfiguration.getConfLocation();
		try {
			shadowProcess = Runtime.getRuntime().exec(command);
			new Thread(() -> dropOutput(shadowProcess)).start();
		} catch (IOException e) {
			String message = "Failed to start shadow process [" + e.getClass().getSimpleName() + "]: " + e.getMessage();
			log.error(message);
			throw new ServiceException(ServiceException.NATIVE_INVOKE_FAILED, message);
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
		try {
			shadowProcess.waitFor();
		} catch (InterruptedException ignored) {
		}
		start();
	}

	@Override
	public Status status() {
		Status status = new Status();
		status.setConfiguration(configuration);
		if (shadowProcess != null && shadowProcess.isAlive()) {
			status.setRunning(true);
		} else {
			status.setRunning(false);
		}
		return status;
	}

	@Override
	public void conf(String conf) {
		if (conf == null) {
			throw new ServiceException(ServiceException.PAYLOAD_NOT_PROVIDED);
		}
		try {
			configuration = ToolKit.parse(conf, Configuration.class);
		} catch (IOException e) {
			String message = "Failed to parse shadow conf [" + e.getClass().getSimpleName() + "]: " + e.getMessage();
			log.error(message);
			throw new ServiceException(ServiceException.PAYLOAD_FORMAT_ERROR, message);
		}
		try {
			configuration.validate();
		} catch (ValidationException e) {
			String message = "Failed to validate shadow conf: " + e.getMessage();
			log.error(message);
			throw new ServiceException(ServiceException.PAYLOAD_FORMAT_ERROR, message);
		}
		try {
			writeConf(generateConf(configuration));
		} catch (IOException | JSONException e) {
			String message = "Failed to write shadow conf [" + e.getClass().getSimpleName() + "]: " + e.getMessage();
			log.error(message);
			throw new ServiceException(ServiceException.NATIVE_INVOKE_FAILED, message);
		}
		String inputChain = invokerConfiguration.getProperties().getOrDefault("iptables-input-chain", "shadow-node-input");
		String outputChain = invokerConfiguration.getProperties().getOrDefault("iptables-output-chain", "shadow-node-output");
		configureFirewall(inputChain, true);
		configureFirewall(outputChain, false);
	}

	private void configureFirewall(String chain, boolean input) {
		String type = input ? "--dport" : "--sport";
		List<String> output = exec("iptables -nvxL " + chain);
		Set<Integer> existPortSet = output.stream()
				.skip(2)
				.map(row -> Integer.valueOf(
						Arrays.stream(row.split(" "))
								.filter(StringUtils::isNotBlank)
								.collect(Collectors.toList()).get(9).split(":")[1]
				))
				.collect(Collectors.toSet());
		Set<Integer> expectPortSet = configuration.getPortPassword().keySet();
		existPortSet.stream()
				.filter(port -> !expectPortSet.contains(port))
				.forEach(port -> exec("iptables -D " + chain + " -p tcp " + type + " " + port));
		expectPortSet.stream()
				.filter(port -> !existPortSet.contains(port))
				.forEach(port -> exec("iptables -A " + chain + " -p tcp " + type + " " + port));
	}

	@Override
	public Statistic statistic() {
		String inputChain = invokerConfiguration.getProperties().getOrDefault("iptables-input-chain", "shadow-node-input");
		String outputChain = invokerConfiguration.getProperties().getOrDefault("iptables-output-chain", "shadow-node-output");
		Map<Integer, Long> inputTraffic = traffic(inputChain);
		Map<Integer, Long> outputTraffic = traffic(outputChain);
		outputTraffic.forEach((key, value) -> inputTraffic.merge(key, value, (a, b) -> a + b));
		Statistic statistic = new Statistic();
		statistic.setTraffic(inputTraffic);
		return statistic;
	}

	private Map<Integer, Long> traffic(String chain) {
		List<String> output = exec("iptables -nvxL " + chain);
		exec("iptables -Z " + chain);
		Map<Integer, Long> traffic = new HashMap<>();
		output.stream().skip(2).forEach(row -> {
			List<String> columns = Arrays.stream(row.split(" ")).filter(StringUtils::isNotBlank).collect(Collectors.toList());
			traffic.put(Integer.valueOf(columns.get(9).split(":")[1]), Long.valueOf(columns.get(1)));
		});
		return traffic;
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
			String message = "Failed to invoke command [" + command + "] with [" + e.getClass().getSimpleName() + "]:" + e.getMessage();
			log.error(message);
			throw new ServiceException(ServiceException.NATIVE_INVOKE_FAILED, message);
		}
	}

	private void dropOutput(Process process) {
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			do {
				line = bufferedReader.readLine();
			} while (line != null);
		} catch (IOException e) {
			log.error("Drop process output fail with exception: " + e.getMessage());
		}
	}

	private void shutdownHook() {
		if (shadowProcess != null && shadowProcess.isAlive()) {
			shadowProcess.destroy();
		}
	}

	@Scheduled(cron = "*/15 * * * * *")
	private void checkDeadSocket() {
		if (shadowProcess == null || !shadowProcess.isAlive()) {
			return;
		}
		String property = invokerConfiguration.getProperties().get("dead-socket-threshold");
		int threshold;
		try {
			threshold = Integer.valueOf(property);
		} catch (NumberFormatException e) {
			return;
		}
		List<String> output = exec("netstat -tn");
		long count = output.stream().filter(line -> line.contains("CLOSE_WAIT")).count();
		log.debug("Check dead socket, count: " + count);
		if (count >= threshold) {
			restart();
		}
	}
}
