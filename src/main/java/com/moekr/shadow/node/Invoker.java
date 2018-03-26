package com.moekr.shadow.node;

import com.moekr.shadow.node.Node.User;
import lombok.extern.apachecommons.CommonsLog;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

@Component
@CommonsLog
public class Invoker {
	private final InvokerConfiguration configuration;
	private final ExecutorService executorService;
	private final RestTemplate restTemplate;

	private Node node;
	private Map<Integer, Long> statisticsMap = new HashMap<>();
	private Process shadowProcess;
	private int failCount = 0;

	@Autowired
	public Invoker(InvokerConfiguration configuration, ExecutorService executorService, RestTemplate restTemplate) {
		if (configuration.getNodeId() == null) {
			throw new NullPointerException("Node id(shadow.invoker.node-id) is not set");
		}
		this.configuration = configuration;
		this.executorService = executorService;
		this.restTemplate = restTemplate;
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownHook));
	}

	@Scheduled(cron = "30 * * * * *")
	private void exchange() {
		String statistic = "[]";
		if (isRunning()) {
			try {
				statistic = collectStatistic();
			} catch (Exception e) {
				log.error("Failed to collect statistic [" + e.getClass().getName() + "]: " + e.getMessage());
			}
		}
		Node node;
		try {
			node = restTemplate.postForObject(new URI(configuration.getPanel().getUrl() + "/api/upload?n=" + configuration.getNodeId()), statistic, Node.class);
			failCount = 0;
		} catch (Exception e) {
			log.error("Failed to communicate with shadow panel [" + e.getClass().getName() + "]: " + e.getMessage());
			if (failCount++ > 10) {
				stop();
			}
			return;
		}
		if (node.getEnable() != null && node.getEnable()) {
			if (!Objects.equals(this.node, node)) {
				this.node = node;
				try {
					writeConf();
					restart();
				} catch (Exception e) {
					log.error("Failed to write config file [" + e.getClass().getName() + "]: " + e.getMessage());
				}
			} else {
				start();
			}
		} else {
			this.node = null;
			stop();
		}
	}

	private boolean isRunning() {
		return shadowProcess != null && shadowProcess.isAlive();
	}


	private void start() {
		if (isRunning()) {
			return;
		}
		try {
			shadowProcess = Runtime.getRuntime().exec(configuration.getExecutable());
			executorService.submit(() -> dropOutput(shadowProcess));
		} catch (IOException e) {
			log.error("Failed to start shadow process [" + e.getClass().getName() + "]: " + e.getMessage());
		}
	}

	private void stop() {
		if (!isRunning()) {
			return;
		}
		shadowProcess.destroy();
	}

	private void restart() {
		stop();
		if (shadowProcess != null) {
			try {
				shadowProcess.waitFor();
			} catch (InterruptedException e) {
				log.error("Failed to wait for process terminating [" + e.getClass().getName() + "]:" + e.getMessage());
			}
		}
		start();
	}

	private void dropOutput(Process process) {
		InputStream stdout = process.getInputStream();
		InputStream stderr = process.getErrorStream();
		InputStream stream = new SequenceInputStream(stdout, stderr);
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream))) {
			String line;
			do {
				line = bufferedReader.readLine();
			} while (line != null);
		} catch (IOException e) {
			log.error("Failed to drop process output [" + e.getClass().getName() + "]: " + e.getMessage());
		}
	}

	private String collectStatistic() throws Exception {
		File confFile = new File(configuration.getConfLocation());
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(confFile)));
		StringBuilder stringBuilder = new StringBuilder();
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			stringBuilder.append(line);
		}
		bufferedReader.close();
		JSONArray array = new JSONArray(stringBuilder.toString());
		JSONArray result = new JSONArray();
		for (int index = 0; index < array.length(); index++) {
			JSONObject object = array.optJSONObject(index);
			if (object != null && object.isNull("protocol_param")) {
				int port = object.optInt("port");
				long download = object.optLong("d");
				long upload = object.optLong("u");
				long origin = statisticsMap.getOrDefault(port, 0L);
				long current = download + upload;
				statisticsMap.put(port, current);
				long delta = Math.max(0, current - origin);
				if (delta > 0) {
					object = new JSONObject();
					object.put("id", port);
					object.put("value", delta);
					result.put(object);
				}
			}
		}
		return result.toString();
	}

	private void writeConf() throws Exception {
		JSONArray array = new JSONArray();
		JSONObject object = new JSONObject();
		object.put("port", node.getPort());
		object.put("passwd", node.getPassword());
		object.put("method", node.getMethod());
		object.put("protocol", node.getProtocol());
		object.put("protocol_param", "#");
		object.put("obfs", node.getObfs());
		object.put("obfs_param", node.getObfsParam());
		object.put("enable", 1);
		array.put(object);
		for (User user : node.getUsers()) {
			object = new JSONObject();
			object.put("port", user.getId());
			object.put("passwd", user.getToken());
			object.put("transfer_enable", 1125899906842624L);
			object.put("d", 0);
			object.put("u", 0);
			object.put("enable", 1);
			array.put(object);
		}
		File confFile = new File(configuration.getConfLocation());
		BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(confFile)));
		bufferedWriter.write(array.toString(4));
		bufferedWriter.flush();
		bufferedWriter.close();
		statisticsMap.clear();
	}

	private void shutdownHook() {
		if (isRunning()) {
			shadowProcess.destroy();
		}
	}
}
