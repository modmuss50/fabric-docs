package net.fabricmc.docs.staticgen;

import java.nio.file.Files;
import java.nio.file.Path;

public final class StaticDocsTest {
	public static void main(String[] args) throws Exception {
		testRoutes();
		testFrontMatter();
		testSnippetRegions();
		testFixtureBuild();
		System.out.println("Static docs tests passed");
	}

	private static void testRoutes() {
		assertEquals("/", Page.from(Path.of("."), Path.of("index.md")).route());
		assertEquals("/players/installing-java/", Page.from(Path.of("."), Path.of("players/installing-java/index.md")).route());
		assertEquals("/de_de/", Page.from(Path.of("."), Path.of("translated/de_de/index.md")).route());
		assertEquals("/1.0/develop/page", Page.from(Path.of("."), Path.of("versions/1.0/develop/page.md")).route());
		assertEquals("/1.0/de_de/", Page.from(Path.of("."), Path.of("versions/1.0/translated/de_de/index.md")).route());
	}

	private static void testFrontMatter() {
		FrontMatter matter = FrontMatter.parse("""
				---
				title: Hello
				authors:
				  - one
				  - two
				resources:
				  https://example.com: Example
				---
				Body
				""");
		assertEquals("Hello", matter.string("title"));
		assertEquals(2, matter.stringList("authors").size());
		assertEquals("Example", matter.stringMap("resources").get("https://example.com"));
		assertEquals("Body\n", matter.content());
	}

	private static void testSnippetRegions() throws Exception {
		Path root = Files.createTempDirectory("static-docs-test");
		Path source = root.resolve("reference/latest/src/main/java/com/example/Test.java");
		Files.createDirectories(source.getParent());
		Files.writeString(source, """
				class Test {
					// #region sample
					void test() {}
					// #endregion sample

					// :::other
					void other() {}
					// :::other
				}
				""");
		Files.writeString(root.resolve("website_translations.json"), "{}");
		Files.writeString(root.resolve("sidebar_translations.json"), "{}");
		Files.createDirectories(root.resolve("versions"));
		BuildContext context = BuildContext.load(new Options(root, root.resolve("out"), 1, true));
		String expanded = new SnippetResolver(context).expand("<<< @/reference/latest/src/main/java/com/example/Test.java#sample");
		assertContains(expanded, "void test()");
		expanded = new SnippetResolver(context).expand("@[code lang=java transcludeWith=:::other](@/reference/latest/src/main/java/com/example/Test.java)");
		assertContains(expanded, "void other()");
	}

	private static void testFixtureBuild() throws Exception {
		Path fixture = Path.of("src/test/resources/fixtures").toAbsolutePath().normalize();
		if (!Files.isDirectory(fixture)) fixture = Path.of("static-docs/src/test/resources/fixtures").toAbsolutePath().normalize();
		Path out = Files.createTempDirectory("static-docs-out");
		SiteGenerator.run(new Options(fixture, out, 2, true));
		assertTrue(Files.exists(out.resolve("index.html")), "index generated");
		assertTrue(Files.exists(out.resolve("de_de/index.html")), "translated index generated");
		assertTrue(Files.exists(out.resolve("1.0/index.html")), "versioned index generated");
		String html = Files.readString(out.resolve("develop/page.html"));
		assertContains(html, "language-java");
		assertContains(html, "choice-card");
		assertContains(html, "</div>");
		assertContains(html, "line-highlight");
		assertTrue(Files.exists(out.resolve("assets/example.txt")), "asset copied");
	}

	private static void assertEquals(Object expected, Object actual) {
		if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + " but got " + actual);
	}

	private static void assertContains(String value, String needle) {
		if (!value.contains(needle)) throw new AssertionError("Expected to find " + needle + " in:\n" + value);
	}

	private static void assertTrue(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}
}
