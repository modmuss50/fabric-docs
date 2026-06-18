package net.fabricmc.docs.staticgen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

final class SiteGenerator {
	private SiteGenerator() {
	}

	static void run(Options options) throws Exception {
		Files.createDirectories(options.out());
		copyPublic(options.root(), options.out());
		copyStatic(options.out());

		BuildContext context = BuildContext.load(options);
		ExecutorService executor = Executors.newFixedThreadPool(options.threads());
		List<Callable<Path>> tasks = new ArrayList<>();

		for (Page page : context.pages()) {
			tasks.add(() -> {
				try {
					String html = new PageRenderer(context, page).render();
					Path output = options.out().resolve(page.outputPath()).normalize();
					if (!output.startsWith(options.out())) throw new IOException("Refusing to write outside output: " + output);
					Files.createDirectories(output.getParent());
					Files.writeString(output, html);
					return output;
				} catch (Exception e) {
					throw new IllegalStateException(page.source() + ": " + e.getMessage(), e);
				}
			});
		}

		List<String> failures = new ArrayList<>();
		for (var future : executor.invokeAll(tasks)) {
			try {
				future.get();
			} catch (Exception e) {
				failures.add(e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
			}
		}
		executor.shutdown();

		if (!failures.isEmpty()) {
			throw new IllegalStateException("Static docs build failed:\n- " + String.join("\n- ", failures));
		}

		writeNotFound(options.out(), context);
		writeSitemap(options.out(), context);
		System.out.println("Generated " + context.pages().size() + " pages into " + options.out());
	}

	private static void copyPublic(Path root, Path out) throws IOException {
		Path publicDir = root.resolve("public");
		if (!Files.isDirectory(publicDir)) return;

		try (Stream<Path> stream = Files.walk(publicDir)) {
			for (Path source : stream.filter(Files::isRegularFile).toList()) {
				Path target = out.resolve(publicDir.relativize(source)).normalize();
				if (!target.startsWith(out)) throw new IOException("Refusing to copy outside output: " + target);
				Files.createDirectories(target.getParent());
				Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	private static void copyStatic(Path out) throws IOException {
		copyResource("static/site.css", out.resolve("static-docs/site.css"));
		copyResource("static/highlight.js", out.resolve("static-docs/highlight.js"));
	}

	private static void copyResource(String name, Path target) throws IOException {
		Files.createDirectories(target.getParent());
		try (var in = SiteGenerator.class.getClassLoader().getResourceAsStream(name)) {
			if (in == null) throw new IOException("Missing classpath resource " + name);
			Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void writeNotFound(Path out, BuildContext context) {
		try {
			Files.writeString(out.resolve("404.html"), Template.page(context, null, "Page not found", "<h1>404</h1><p>Page not found.</p>", ""));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void writeSitemap(Path out, BuildContext context) {
		StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
		for (Page page : context.pages()) {
			String route = page.route().equals("/") ? "/" : page.route() + (page.route().endsWith("/") ? "" : ".html");
			xml.append("\t<url><loc>https://docs.fabricmc.net").append(Html.escape(route)).append("</loc></url>\n");
		}
		xml.append("</urlset>\n");
		try {
			Files.writeString(out.resolve("sitemap.xml"), xml.toString());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
