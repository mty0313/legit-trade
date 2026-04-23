package com.trade.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.trade.TradeConfig;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.ResponseException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

public class WebServer extends NanoHTTPD {
	private static final Logger LOGGER = LoggerFactory.getLogger("legittrade");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static final int MAX_REQUEST_BODY_BYTES = 1024 * 1024;
	private static WebServer instance;
	private static Runnable configSavedCallback = () -> {};

	public static void start(WebConfig config) {
		if (instance != null) {
			LOGGER.warn("Web server already running");
			return;
		}

		if (!config.isEnabled()) {
			LOGGER.info("Web UI disabled in config");
			return;
		}

		try {
			instance = new WebServer(config.getBindAddress(), config.getPort());
			instance.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
			LOGGER.info("Web UI started at http://{}:{}", config.getBindAddress(), config.getPort());
		} catch (IOException e) {
			LOGGER.error("Failed to start web server", e);
		}
	}

	public static void stopServer() {
		if (instance != null) {
			instance.stop();
			instance = null;
			LOGGER.info("Web server stopped");
		}
		configSavedCallback = () -> {};
	}

	public static boolean isRunning() {
		return instance != null;
	}

	public static void setConfigSavedCallback(Runnable callback) {
		configSavedCallback = callback != null ? callback : () -> {};
	}

	public WebServer(String hostname, int port) {
		super(hostname, port);
	}

	@Override
	public Response serve(IHTTPSession session) {
		String uri = session.getUri();
		Method method = session.getMethod();

		try {
			// API routes
			if (uri.startsWith("/api/")) {
				return handleApi(uri, method, session);
			}

			// Static files
			return serveStatic(uri);
		} catch (Exception e) {
			LOGGER.error("Error serving request: " + uri, e);
			return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal error");
		}
	}

	private Response handleApi(String uri, Method method, IHTTPSession session) {
		try {
			switch (uri) {
				case "/api/items":
					return handleItems();
				case "/api/trades":
					if (method == Method.GET) {
						return handleGetTrades();
					} else if (method == Method.POST) {
						return handleSaveTrades(session);
					}
					return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Method not allowed");
				case "/api/nbt/validate":
					return handleNbtValidate(session);
				default:
					return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found");
			}
		} catch (Exception e) {
			LOGGER.error("API error: " + uri, e);
			return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal error");
		}
	}

	private Response handleItems() {
		JsonArray items = new JsonArray();
		Registries.ITEM.forEach(item -> {
			JsonObject obj = new JsonObject();
			Identifier id = Registries.ITEM.getId(item);
			obj.addProperty("id", id.toString());
			obj.addProperty("name", item.getName().getString());
			obj.addProperty("translationKey", item.getTranslationKey());
			obj.addProperty("maxCount", item.getMaxCount());
			items.add(obj);
		});
		return json(items.toString());
	}

	private Response handleGetTrades() {
		JsonArray groups = new JsonArray();
		for (TradeConfig.TradeGroup group : TradeConfig.getTradeGroups()) {
			JsonObject groupObj = new JsonObject();
			groupObj.addProperty("group", group.group);
			JsonArray trades = new JsonArray();
			for (TradeConfig.TradeEntry trade : group.trades) {
				JsonObject tradeObj = new JsonObject();
				tradeObj.addProperty("input", trade.input);
				tradeObj.addProperty("output", trade.output);
				if (trade.inputNbt != null) {
					tradeObj.addProperty("inputNbt", trade.inputNbt);
				}
				if (trade.outputNbt != null) {
					tradeObj.addProperty("outputNbt", trade.outputNbt);
				}
				tradeObj.addProperty("nbtMatchMode", trade.nbtMatchMode.name().toLowerCase());
				tradeObj.addProperty("inputCount", trade.inputCount);
				tradeObj.addProperty("outputCount", trade.outputCount);
				tradeObj.addProperty("xpReward", trade.xpReward);
				trades.add(tradeObj);
			}
			groupObj.add("trades", trades);
			groups.add(groupObj);
		}
		return json(groups.toString());
	}

	private Response handleSaveTrades(IHTTPSession session) {
		try {
			Map<String, String> headers = session.getHeaders();
			String contentLengthStr = headers.get("content-length");
			if (contentLengthStr == null) {
				return json(Response.Status.BAD_REQUEST, "{\"error\":\"Missing content-length\"}");
			}

			int contentLength;
			try {
				contentLength = Integer.parseInt(contentLengthStr);
			} catch (NumberFormatException e) {
				return json(Response.Status.BAD_REQUEST, "{\"error\":\"Invalid content-length\"}");
			}

			if (contentLength <= 0) {
				return json(Response.Status.BAD_REQUEST, "{\"error\":\"Empty request body\"}");
			}
			if (contentLength > MAX_REQUEST_BODY_BYTES) {
				return json(Response.Status.BAD_REQUEST, "{\"error\":\"Request body too large\"}");
			}

			InputStream is = session.getInputStream();
			byte[] buffer = new byte[contentLength];
			int bytesRead = 0;
			while (bytesRead < contentLength) {
				int read = is.read(buffer, bytesRead, contentLength - bytesRead);
				if (read == -1) {
					break;
				}
				bytesRead += read;
			}
			if (bytesRead != contentLength) {
				return json(Response.Status.BAD_REQUEST, "{\"error\":\"Incomplete request body\"}");
			}

			String jsonBody = new String(buffer, StandardCharsets.UTF_8);
			List<TradeConfig.TradeGroup> validGroups = TradeConfig.parseTradeGroups(jsonBody);
			if (validGroups.isEmpty()) {
				return json(Response.Status.BAD_REQUEST, "{\"error\":\"No valid trades in payload\"}");
			}

			Path configPath = FabricLoader.getInstance().getConfigDir().resolve("legittrade.json");
			Path tempPath = configPath.resolveSibling("legittrade.json.tmp");
			Files.writeString(tempPath, jsonBody, StandardCharsets.UTF_8);
			try {
				Files.move(tempPath, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException ignored) {
				Files.move(tempPath, configPath, StandardCopyOption.REPLACE_EXISTING);
			}

			TradeConfig.setTradeGroups(validGroups);
			configSavedCallback.run();
			return json(Response.Status.OK, "{\"success\":true}");
		} catch (Exception e) {
			LOGGER.error("Failed to save trades", e);
			return json(Response.Status.INTERNAL_ERROR, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
		}
	}

	private Response handleNbtValidate(IHTTPSession session) {
		Map<String, List<String>> params = session.getParameters();
		List<String> nbtParams = params.get("nbt");
		String nbt = nbtParams != null && !nbtParams.isEmpty() ? nbtParams.get(0) : null;

		if (nbt == null || nbt.isBlank()) {
			return json("{\"valid\":true}");
		}

		try {
			net.minecraft.nbt.StringNbtReader.parse(nbt);
			return json("{\"valid\":true}");
		} catch (Exception e) {
			return json("{\"valid\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
		}
	}

	private Response serveStatic(String uri) {
		if (uri.equals("/") || uri.isEmpty()) {
			uri = "/index.html";
		}

		String resourcePath = "/web" + uri;
		InputStream stream = getClass().getResourceAsStream(resourcePath);

		if (stream == null) {
			return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found");
		}

		String mime = getMimeType(uri);
		return newFixedLengthResponse(Response.Status.OK, mime, stream, -1);
	}

	private Response json(String body) {
		return json(Response.Status.OK, body);
	}

	private Response json(Response.Status status, String body) {
		return newFixedLengthResponse(status, "application/json", body);
	}

	private String getMimeType(String path) {
		if (path.endsWith(".html")) return "text/html";
		if (path.endsWith(".css")) return "text/css";
		if (path.endsWith(".js")) return "application/javascript";
		if (path.endsWith(".json")) return "application/json";
		if (path.endsWith(".png")) return "image/png";
		if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
		if (path.endsWith(".svg")) return "image/svg+xml";
		return "application/octet-stream";
	}

	private String escapeJson(String s) {
		if (s == null) return "";
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}
}
