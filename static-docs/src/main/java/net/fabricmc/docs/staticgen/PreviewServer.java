package net.fabricmc.docs.staticgen;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public final class PreviewServer {
	private PreviewServer() {
	}

	public static void main(String[] args) throws Exception {
		ServerOptions server = ServerOptions.parse(args);
		regenerate(server);
		HttpServer http = HttpServer.create(new InetSocketAddress(server.host, server.port), 0);
		http.createContext("/", exchange -> serve(server.out, exchange));
		http.setExecutor(Executors.newCachedThreadPool());
		http.start();
		System.out.println("Serving static docs at http://" + server.host + ":" + server.port + "/");
		if (server.watch) watch(server);
	}

	private static void regenerate(ServerOptions server) {
		try {
			SiteGenerator.run(new Options(server.root, server.out, server.threads, server.strict));
		} catch (Exception e) {
			System.err.println("Regeneration failed:\n" + e.getMessage());
		}
	}

	private static void serve(Path out, HttpExchange exchange) throws IOException {
		String rawPath = exchange.getRequestURI().getPath();
		Path target = out.resolve(rawPath.replaceFirst("^/+", "")).normalize();
		if (rawPath.endsWith("/")) target = target.resolve("index.html");
		if (!target.startsWith(out) || !Files.exists(target) || Files.isDirectory(target)) target = out.resolve("404.html");
		if (!Files.exists(target)) {
			byte[] body = "Not found".getBytes(java.nio.charset.StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(404, body.length);
			try (OutputStream response = exchange.getResponseBody()) {
				response.write(body);
			}
			return;
		}
		byte[] body = Files.readAllBytes(target);
		exchange.getResponseHeaders().set("Content-Type", contentType(target));
		exchange.sendResponseHeaders(target.getFileName().toString().equals("404.html") ? 404 : 200, body.length);
		try (OutputStream response = exchange.getResponseBody()) {
			response.write(body);
		}
	}

	private static void watch(ServerOptions server) throws InterruptedException, IOException {
		Map<Path, Long> snapshot = snapshot(server.root);
		while (true) {
			Thread.sleep(1000);
			Map<Path, Long> next = snapshot(server.root);
			if (!next.equals(snapshot)) {
				System.out.println("Change detected; regenerating static docs...");
				regenerate(server);
				snapshot = next;
			}
		}
	}

	private static Map<Path, Long> snapshot(Path root) throws IOException {
		Map<Path, Long> files = new HashMap<>();
		for (String directory : new String[] {"develop", "players", "translated", "versions", "reference", "public", "static-docs/src/main"}) {
			Path base = root.resolve(directory);
			if (!Files.exists(base)) continue;
			try (Stream<Path> stream = Files.walk(base)) {
				for (Path file : stream.filter(Files::isRegularFile).toList()) {
					String name = file.toString();
					if (name.contains("/build/") || name.contains("/node_modules/")) continue;
					files.put(root.relativize(file), Files.getLastModifiedTime(file).toMillis());
				}
			}
		}
		for (String file : new String[] {"index.md", "contributing.md", "website_translations.json", "sidebar_translations.json"}) {
			Path path = root.resolve(file);
			if (Files.exists(path)) files.put(root.relativize(path), Files.getLastModifiedTime(path).toMillis());
		}
		return files;
	}

	private static String contentType(Path path) {
		String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
		if (name.endsWith(".html")) return "text/html; charset=utf-8";
		if (name.endsWith(".css")) return "text/css; charset=utf-8";
		if (name.endsWith(".js")) return "text/javascript; charset=utf-8";
		if (name.endsWith(".json")) return "application/json; charset=utf-8";
		if (name.endsWith(".svg")) return "image/svg+xml";
		if (name.endsWith(".png")) return "image/png";
		if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
		if (name.endsWith(".webp")) return "image/webp";
		return "application/octet-stream";
	}

	private record ServerOptions(Path root, Path out, String host, int port, int threads, boolean strict, boolean watch) {
		static ServerOptions parse(String[] args) {
			Path root = Path.of("..");
			Path out = Path.of("build/site");
			String host = "localhost";
			int port = 8080;
			int threads = Runtime.getRuntime().availableProcessors();
			boolean strict = true;
			boolean watch = true;
			for (int i = 0; i < args.length; i++) {
				String key = args[i];
				if (key.equals("--open")) {
					i++;
					continue;
				}
				if (i + 1 >= args.length) throw new IllegalArgumentException("Missing value for " + key);
				String value = args[++i];
				switch (key) {
				case "--root" -> root = Path.of(value);
				case "--out" -> out = Path.of(value);
				case "--host" -> host = value;
				case "--port" -> port = Integer.parseInt(value);
				case "--threads" -> threads = value.equalsIgnoreCase("auto") ? Runtime.getRuntime().availableProcessors() : Integer.parseInt(value);
				case "--strict" -> strict = Boolean.parseBoolean(value);
				case "--watch" -> watch = Boolean.parseBoolean(value);
				default -> throw new IllegalArgumentException("Unknown argument: " + key);
				}
			}
			return new ServerOptions(root.toAbsolutePath().normalize(), out.toAbsolutePath().normalize(), host, port, Math.max(1, threads), strict, watch);
		}
	}
}
