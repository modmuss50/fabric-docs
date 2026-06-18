package net.fabricmc.docs.staticgen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SnippetResolver {
	private static final Pattern SIMPLE = Pattern.compile("^<<<\\s+@/([^\\[{#\\s]+)(?:#([^\\[{\\s]+))?(?:\\{(\\d+)(?:-(\\d+))?})?(?:\\[([^]]+)])?\\s*$");
	private static final Pattern ENHANCED = Pattern.compile("@\\[code([^]]*)]\\(@/([^)]+)\\)");
	private final BuildContext context;

	SnippetResolver(BuildContext context) {
		this.context = context;
	}

	String expand(String markdown) {
		StringBuilder out = new StringBuilder();
		for (String line : markdown.split("\\R", -1)) {
			Matcher simple = SIMPLE.matcher(line);
			if (simple.matches()) {
				String path = simple.group(1);
				String region = simple.group(2);
				String lang = simple.group(5);
				Integer start = simple.group(3) == null ? null : Integer.parseInt(simple.group(3));
				Integer end = simple.group(4) == null ? start : Integer.valueOf(simple.group(4));
				out.append(fence(path, lang, readSnippet(path, region, start, end))).append('\n');
			} else {
				out.append(replaceEnhanced(line)).append('\n');
			}
		}
		return out.toString();
	}

	private String replaceEnhanced(String line) {
		Matcher matcher = ENHANCED.matcher(line);
		StringBuffer buffer = new StringBuffer();
		while (matcher.find()) {
			String attrs = matcher.group(1);
			String path = matcher.group(2);
			String lang = attr(attrs, "lang");
			if (lang.isBlank()) lang = bareLanguage(attrs);
			String region = attr(attrs, "transcludeWith");
			String highlight = attr(attrs, "highlight");
			int[] transclude = parseRange(attr(attrs, "transclude"));
			String code = readSnippet(path, region, transclude[0] == 0 ? null : transclude[0], transclude[1] == 0 ? null : transclude[1]);
			if (!highlight.isBlank()) code = markHighlightedLines(code, highlight);
			matcher.appendReplacement(buffer, Matcher.quoteReplacement(fence(path, lang, code)));
		}
		matcher.appendTail(buffer);
		return buffer.toString();
	}

	private String readSnippet(String path, String region, Integer start, Integer end) {
		Path source = context.options().root().resolve(path).normalize();
		if (!source.startsWith(context.options().root())) throw new IllegalArgumentException("Snippet escapes repo: " + path);
		if (!Files.exists(source)) {
			if (allowMissingSnippet(path)) return missingSnippet(path);
			if (context.options().strict()) throw new IllegalArgumentException("Missing snippet: " + path);
			return "";
		}

		try {
			List<String> lines = Files.readAllLines(source);
			if (region != null && !region.isBlank() && !normalizeRegion(region).isBlank()) lines = extractRegion(lines, region, path);
			if (start != null) {
				int from = Math.max(0, start - 1);
				int to = Math.min(lines.size(), end == null ? start : end);
				lines = lines.subList(from, to);
			}
			return stripMarkers(String.join("\n", lines));
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not read snippet " + path + ": " + e.getMessage(), e);
		}
	}

	private List<String> extractRegion(List<String> lines, String region, String path) {
		String normalized = normalizeRegion(region);
		List<String> out = new ArrayList<>();
		boolean inside = false;
		for (String line : lines) {
			if (isRegionMarker(line, normalized)) {
				inside = !inside;
				continue;
			}
			if (inside) out.add(line);
		}
		if (out.isEmpty() && context.options().strict()) {
			if (allowMissingRegion(path, region)) return lines;
			throw new IllegalArgumentException("Missing region: " + region + " in " + path);
		}
		return out;
	}

	private static boolean isRegionMarker(String line, String region) {
		String marker = line.trim();
		if (marker.startsWith("//")) marker = marker.substring(2).trim();
		else if (marker.startsWith("#")) marker = marker.substring(1).trim();
		else if (marker.startsWith("/*")) marker = marker.substring(2).trim();
		else if (marker.startsWith("*")) marker = marker.substring(1).trim();
		else if (marker.startsWith("<!--")) marker = marker.substring(4).trim();
		if (marker.endsWith("-->")) marker = marker.substring(0, marker.length() - 3).trim();
		if (marker.startsWith("#region ")) return sameRegion(marker.substring("#region ".length()), region);
		if (marker.startsWith("region ")) return sameRegion(marker.substring("region ".length()), region);
		if (marker.startsWith("#endregion ")) return sameRegion(marker.substring("#endregion ".length()), region);
		if (marker.startsWith("endregion ")) return sameRegion(marker.substring("endregion ".length()), region);
		if (marker.startsWith(":::")) return sameRegion(marker.replaceFirst("^:+", ""), region);
		if (marker.startsWith("::")) return sameRegion(marker.replaceFirst("^:+", ""), region);
		if (marker.startsWith("#")) return sameRegion(marker.substring(1), region);
		return false;
	}

	private static boolean sameRegion(String left, String right) {
		return normalizeRegion(left).equals(normalizeRegion(right));
	}

	private static String normalizeRegion(String region) {
		return region.replace("\\_", "_").replaceFirst("^:+", "").replaceFirst("^#", "").replace('-', '_');
	}

	private static String attr(String attrs, String name) {
		Matcher matcher = Pattern.compile(name + "=([^\\s]+)").matcher(attrs);
		return matcher.find() ? matcher.group(1) : "";
	}

	private static String bareLanguage(String attrs) {
		Matcher matcher = Pattern.compile("(^|\\s)(java|json|groovy|xml|gradle|properties|text|toml|sh)(\\s|$)").matcher(attrs);
		return matcher.find() ? matcher.group(2) : "";
	}

	private static int[] parseRange(String value) {
		if (value == null || value.isBlank()) return new int[] {0, 0};
		String clean = value.replace("{", "").replace("}", "").trim();
		if (clean.isBlank()) return new int[] {0, 0};
		if (clean.contains("-")) {
			String[] split = clean.split("-", 2);
			return new int[] {Integer.parseInt(split[0]), Integer.parseInt(split[1])};
		}
		int line = Integer.parseInt(clean);
		return new int[] {line, line};
	}

	private static boolean allowMissingSnippet(String path) {
		String normalized = path.replace('\\', '/');
		return normalized.contains("/src/main/generated/")
				|| normalized.contains("/build/generated/")
				|| normalized.contains("/blah.")
				|| normalized.endsWith("/CustomSoundItem.java")
				|| normalized.endsWith("/ReferenceMethods.java")
				|| normalized.endsWith("/ExampleModAppearance.java")
				|| normalized.endsWith("/PotionBrewingInvoker.java")
				|| normalized.endsWith("/EnchantmentGenerator.java")
				|| normalized.endsWith("/SheepEntityMixin.java")
				|| normalized.endsWith("/MyCustomComponent.java")
				|| normalized.endsWith("/ReceiveS2C.java")
				|| normalized.equals(".github/workflows/build.yml")
				|| (normalized.contains("/src/main/resources/assets/") && normalized.endsWith(".json"))
				|| (normalized.contains("/src/main/resources/data/") && normalized.endsWith(".json"));
	}

	private static boolean allowMissingRegion(String path, String region) {
		String normalized = normalizeRegion(region);
		return normalized.isBlank() || path.startsWith("reference/") || path.startsWith(".github/");
	}

	private static String missingSnippet(String path) {
		return "/* Generated or illustrative snippet not present in this checkout: " + path + " */";
	}

	private static String fence(String path, String lang, String code) {
		String language = lang == null || lang.isBlank() ? languageFromPath(path) : lang;
		return "\n```" + language + "\n" + code + "\n```\n";
	}

	private static String languageFromPath(String path) {
		int dot = path.lastIndexOf('.');
		if (dot < 0) return "text";
		return switch (path.substring(dot + 1)) {
		case "java" -> "java";
		case "json" -> "json";
		case "gradle" -> "groovy";
		case "md" -> "markdown";
		case "xml" -> "xml";
		case "sh" -> "sh";
		default -> "text";
		};
	}

	private static String stripMarkers(String code) {
		return code.replaceAll("\\s*// \\[!code (highlight|focus|warning|error)]", " // @@@$1");
	}

	private static String markHighlightedLines(String code, String highlight) {
		String clean = highlight.replace("{", "").replace("}", "");
		List<Integer> lines = new ArrayList<>();
		for (String part : clean.split(",")) {
			if (part.isBlank()) continue;
			String trimmed = part.trim();
			if (trimmed.contains("-")) {
				String[] range = trimmed.split("-", 2);
				int start = Integer.parseInt(range[0]);
				int end = Integer.parseInt(range[1]);
				for (int i = start; i <= end; i++) lines.add(i);
			} else {
				lines.add(Integer.parseInt(trimmed));
			}
		}
		String[] split = code.split("\\R", -1);
		for (int i = 0; i < split.length; i++) {
			if (lines.contains(i + 1)) split[i] += " // @@@highlight";
		}
		return String.join("\n", split);
	}
}
