package net.fabricmc.docs.staticgen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record FrontMatter(Map<String, Object> data, String content) {
	static FrontMatter parse(String source) {
		if (!source.startsWith("---\n")) return new FrontMatter(Map.of(), source);
		int end = source.indexOf("\n---", 4);
		if (end < 0) throw new IllegalArgumentException("Unclosed frontmatter");

		Map<String, Object> data = parseYaml(source.substring(4, end));
		String content = source.substring(source.indexOf('\n', end + 1) + 1);
		return new FrontMatter(data, content);
	}

	String string(String key) {
		Object value = data.get(key);
		return value instanceof String s ? s : "";
	}

	@SuppressWarnings("unchecked")
	List<String> stringList(String key) {
		Object value = data.get(key);
		if (value instanceof List<?> list) return list.stream().map(String::valueOf).toList();
		if (value instanceof String s && !s.isBlank()) return List.of(s);
		return List.of();
	}

	@SuppressWarnings("unchecked")
	Map<String, String> stringMap(String key) {
		Object value = data.get(key);
		if (value instanceof Map<?, ?> map) {
			Map<String, String> out = new LinkedHashMap<>();
			for (var entry : map.entrySet()) out.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
			return out;
		}
		return Map.of();
	}

	@SuppressWarnings("unchecked")
	List<Map<String, String>> mapList(String key) {
		Object value = data.get(key);
		if (value instanceof List<?> list) {
			List<Map<String, String>> out = new ArrayList<>();
			for (Object item : list) {
				if (item instanceof Map<?, ?> map) {
					Map<String, String> converted = new LinkedHashMap<>();
					for (var entry : map.entrySet()) converted.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
					out.add(converted);
				}
			}
			return out;
		}
		return List.of();
	}

	private static Map<String, Object> parseYaml(String yaml) {
		Map<String, Object> data = new LinkedHashMap<>();
		String currentKey = null;
		Map<String, String> currentListMap = null;
		for (String raw : yaml.split("\\R")) {
			if (raw.isBlank()) continue;
			if (!raw.startsWith(" ") && raw.endsWith(":")) {
				currentKey = raw.substring(0, raw.length() - 1).trim();
				data.put(currentKey, new ArrayList<String>());
				currentListMap = null;
				continue;
			}
			if (raw.startsWith("  - ") && currentKey != null) {
				Object value = data.get(currentKey);
				if (!(value instanceof List<?> list)) {
					value = new ArrayList<Object>();
					data.put(currentKey, value);
				}
				@SuppressWarnings("unchecked")
				List<Object> list = (List<Object>) value;
				String item = raw.substring(4).trim();
				int colon = item.indexOf(": ");
				if (colon > 0) {
					currentListMap = new LinkedHashMap<>();
					currentListMap.put(item.substring(0, colon).trim(), unquote(item.substring(colon + 1).trim()));
					list.add(currentListMap);
				} else {
					currentListMap = null;
					list.add(unquote(item));
				}
				continue;
			}
			if (raw.startsWith("    ") && currentListMap != null) {
				int colon = raw.indexOf(": ");
				if (colon > 0) currentListMap.put(raw.substring(0, colon).trim(), unquote(raw.substring(colon + 1).trim()));
				continue;
			}
			if (raw.startsWith("  ") && currentKey != null) {
				Object value = data.get(currentKey);
				if (!(value instanceof Map<?, ?>)) {
					value = new LinkedHashMap<String, String>();
					data.put(currentKey, value);
				}
				int colon = raw.indexOf(": ");
				if (colon > 0) {
					@SuppressWarnings("unchecked")
					Map<String, String> map = (Map<String, String>) value;
					map.put(raw.substring(0, colon).trim(), unquote(raw.substring(colon + 1).trim()));
				}
				continue;
			}
			int colon = raw.indexOf(':');
			if (colon > 0) {
				currentKey = raw.substring(0, colon).trim();
				String value = raw.substring(colon + 1).trim();
				data.put(currentKey, parseScalar(value));
				currentListMap = null;
			}
		}
		return data;
	}

	private static Object parseScalar(String value) {
		if (value.equals("true")) return true;
		if (value.equals("false")) return false;
		return unquote(value);
	}

	private static String unquote(String value) {
		if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}
}
