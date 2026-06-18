package net.fabricmc.docs.staticgen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

record BuildContext(Options options, List<Page> pages, Translations translations, List<String> versions, List<String> locales, Map<String, Page> routes) {
	static BuildContext load(Options options) throws IOException {
		List<Page> pages = discoverPages(options.root());
		Set<String> versions = new TreeSet<>(SemanticVersionComparator.INSTANCE);

		try (Stream<Path> stream = Files.list(options.root().resolve("versions"))) {
			stream.filter(Files::isDirectory).map(p -> p.getFileName().toString()).forEach(versions::add);
		} catch (IOException ignored) {
		}

		String latest = latestVersion(options.root());
		if (!latest.isBlank()) versions.add(latest);

		Translations translations = Translations.load(options.root());
		List<String> locales = discoverLocales(options.root());
		Map<String, Page> routes = pages.stream().collect(java.util.stream.Collectors.toMap(Page::route, p -> p, (a, b) -> a));
		return new BuildContext(options, List.copyOf(pages), translations, List.copyOf(versions), locales, routes);
	}

	private static List<Page> discoverPages(Path root) throws IOException {
		List<Page> pages = new ArrayList<>();
		try (Stream<Path> stream = Files.walk(root)) {
			for (Path file : stream.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".md")).toList()) {
				Path relative = root.relativize(file);
				if (isIgnored(relative)) continue;
				pages.add(Page.from(root, relative));
			}
		}
		pages.sort(Comparator.comparing(Page::route));
		return pages;
	}

	private static boolean isIgnored(Path relative) {
		String path = relative.toString().replace('\\', '/');
		if (path.equals("README.md")) return true;
		return path.startsWith(".")
				|| path.startsWith("node_modules/")
				|| path.startsWith(".pnpm-store/")
				|| path.startsWith("static-docs/src/")
				|| path.startsWith("static-docs/build/")
				|| path.startsWith("reference/")
				|| path.contains("/build/");
	}

	private static String latestVersion(Path root) {
		Path build = root.resolve("reference/latest/build.gradle");
		if (!Files.exists(build)) return "";
		try {
			for (String line : Files.readAllLines(build)) {
				int index = line.indexOf("def minecraftVersion = ");
				if (index >= 0) return line.substring(line.indexOf('"') + 1, line.lastIndexOf('"'));
			}
		} catch (IOException ignored) {
		}
		return "";
	}

	private static List<String> discoverLocales(Path root) throws IOException {
		List<String> locales = new ArrayList<>();
		locales.add("en_us");
		Path translated = root.resolve("translated");
		if (Files.isDirectory(translated)) {
			try (Stream<Path> stream = Files.list(translated)) {
				stream.filter(Files::isDirectory).map(path -> path.getFileName().toString()).sorted().forEach(locales::add);
			}
		}
		return List.copyOf(locales);
	}
}
