package net.fabricmc.docs.staticgen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Translations {
	private final Map<String, Map<String, String>> website;
	private final Map<String, Map<String, String>> sidebar;

	private Translations(Map<String, Map<String, String>> website, Map<String, Map<String, String>> sidebar) {
		this.website = website;
		this.sidebar = sidebar;
	}

	static Translations load(Path root) throws IOException {
		Map<String, Map<String, String>> website = new HashMap<>();
		Map<String, Map<String, String>> sidebar = new HashMap<>();
		website.put("en_us", readFlatJson(root.resolve("website_translations.json")));
		sidebar.put("en_us", readFlatJson(root.resolve("sidebar_translations.json")));

		Path translated = root.resolve("translated");
		if (Files.isDirectory(translated)) {
			try (var stream = Files.list(translated)) {
				for (Path locale : stream.filter(Files::isDirectory).toList()) {
					String name = locale.getFileName().toString();
					website.put(name, readFlatJson(locale.resolve("website_translations.json")));
					sidebar.put(name, readFlatJson(locale.resolve("sidebar_translations.json")));
				}
			}
		}

		return new Translations(website, sidebar);
	}

	String website(String locale, String key) {
		return lookup(website, locale, key);
	}

	String sidebar(String locale, String key) {
		return lookup(sidebar, locale, key);
	}

	private static String lookup(Map<String, Map<String, String>> maps, String locale, String key) {
		String value = maps.getOrDefault(locale, Map.of()).get(key);
		if (value != null) return value;
		return maps.getOrDefault("en_us", Map.of()).getOrDefault(key, key);
	}

	private static Map<String, String> readFlatJson(Path path) throws IOException {
		if (!Files.exists(path)) return Map.of();
		String json = Files.readString(path);
		Map<String, String> out = new LinkedHashMap<>();
		Matcher matcher = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"(?:\\\\.|[^\"])*\"|\\[[^]]*])", Pattern.DOTALL).matcher(json);
		while (matcher.find()) {
			if (matcher.group(2).startsWith("\"")) out.put(unescape(matcher.group(1)), unescape(matcher.group(2).substring(1, matcher.group(2).length() - 1)));
		}
		return out;
	}

	private static String unescape(String value) {
		return value.replace("\\n", "\n").replace("\\\"", "\"").replace("\\/", "/");
	}
}
