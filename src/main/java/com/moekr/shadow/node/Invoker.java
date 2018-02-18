package com.moekr.shadow.node;

import com.alibaba.dubbo.config.annotation.Reference;
import com.moekr.shadow.rpc.RpcService;
import com.moekr.shadow.rpc.vo.*;
import lombok.extern.apachecommons.CommonsLog;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;

@Component
@CommonsLog
public class Invoker {
	@Reference(version = RpcService.VERSION)
	private RpcService rpcService;
	private final InvokerConfiguration invokerConfiguration;
	private final ExecutorService executorService;

	private Set<Server> servers = new HashSet<>();
	private Set<VirtualServer> virtualServers = new HashSet<>();
	private Map<Integer, Traffic> trafficMap = new HashMap<>();
	private Process shadowProcess;

	@Autowired
	public Invoker(InvokerConfiguration invokerConfiguration, ExecutorService executorService) {
		this.invokerConfiguration = invokerConfiguration;
		this.executorService = executorService;
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownHook));
	}

	@Scheduled(cron = "30 * * * * *")
	private void exchange() {
		Statistic statistic;
		if (isRunning()) {
			try {
				statistic = collectStatistic();
			} catch (Exception e) {
				log.error("Failed to collect statistic [" + e.getClass().getName() + "]: " + e.getMessage());
				return;
			}
		} else {
			statistic = new Statistic();
			statistic.setRunning(false);
		}
		Configuration configuration;
		try {
			configuration = rpcService.exchange(invokerConfiguration.getNodeId(), statistic);
		} catch (Throwable e) {
			log.error("Failed to communicate with shadow panel [" + e.getClass().getName() + "]: " + e.getMessage());
			return;
		}
		if (configuration == null) {
			log.error("Configuration sent from shadow panel is NULL");
			return;
		}
		if (!Objects.equals(configuration.getServers(), servers) || !Objects.equals(configuration.getVirtualServers(), virtualServers)) {
			servers = configuration.getServers();
			virtualServers = configuration.getVirtualServers();
			try {
				writeConf();
				trafficMap.clear();
				restart();
			} catch (Exception e) {
				log.error("Failed to write configuration [" + e.getClass().getName() + "]: " + e.getMessage());
			}
		} else {
			doAction(configuration.getAction());
		}
	}

	private boolean isRunning() {
		return shadowProcess != null && shadowProcess.isAlive();
	}

	private void doAction(Action action) {
		switch (action) {
			case START:
				if (!isRunning()) start();
				break;
			case STOP:
				if (isRunning()) stop();
				break;
			case RESTART:
				if (isRunning()) restart();
		}
	}

	private void start() {
		if (isRunning()) {
			return;
		}
		if (servers.isEmpty() || virtualServers.isEmpty()) {
			return;
		}
		try {
			shadowProcess = Runtime.getRuntime().exec(invokerConfiguration.getExecutable());
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

	private Statistic collectStatistic() throws Exception {
		Statistic statistic = new Statistic();
		statistic.setRunning(isRunning());
		File confFile = new File(invokerConfiguration.getConfLocation());
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(confFile)));
		StringBuilder stringBuilder = new StringBuilder();
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			stringBuilder.append(line);
		}
		bufferedReader.close();
		JSONArray array = new JSONArray(stringBuilder.toString());
		for (int index = 0; index < array.length(); index++) {
			JSONObject object = array.optJSONObject(index);
			if (object != null && object.isNull("protocol_param")) {
				int port = object.optInt("port");
				long download = object.optLong("d", 0);
				long upload = object.optLong("u", 0);
				Traffic oldTraffic = trafficMap.computeIfAbsent(port, key -> new Traffic());
				Traffic newTraffic = new Traffic();
				newTraffic.setDownload(Math.max(0, download - oldTraffic.getDownload()));
				newTraffic.setUpload(Math.max(0, upload - oldTraffic.getUpload()));
				oldTraffic.setDownload(download);
				oldTraffic.setUpload(upload);
				statistic.getTrafficMap().put(port, newTraffic);
			}
		}
		return statistic;
	}

	private void writeConf() throws Exception {
		JSONArray array = new JSONArray();
		JSONObject object;
		for (Server server : servers) {
			object = new JSONObject();
			object.put("port", server.getPort());
			object.put("passwd", server.getPassword());
			object.put("method", server.getMethod());
			object.put("protocol", server.getProtocol());
			object.put("protocol_param", "#");
			object.put("obfs", server.getObfs());
			object.put("obfs_param", server.getObfsParam());
			object.put("enable", 1);
			array.put(object);
		}
		for (VirtualServer virtualServer : virtualServers) {
			object = new JSONObject();
			object.put("port", virtualServer.getPort());
			object.put("passwd", virtualServer.getPassword());
			object.put("transfer_enable", 1125899906842624L);
			object.put("d", 0);
			object.put("u", 0);
			object.put("enable", 1);
			array.put(object);
		}
		File confFile = new File(invokerConfiguration.getConfLocation());
		BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(confFile)));
		bufferedWriter.write(array.toString(4));
		bufferedWriter.flush();
		bufferedWriter.close();
	}

	private void shutdownHook() {
		if (isRunning()) {
			shadowProcess.destroy();
		}
	}
}
