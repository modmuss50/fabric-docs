package net.fabricmc.docs.staticgen;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

final class PageRenderer {
	private final BuildContext context;
	private final Page page;

	PageRenderer(BuildContext context, Page page) {
		this.context = context;
		this.page = page;
	}

	String render() throws IOException {
		FrontMatter frontMatter = FrontMatter.parse(Files.readString(page.absoluteSource()));
		String markdown = transform(frontMatter);
		markdown = new SnippetResolver(context).expand(markdown);
		String body = new MarkdownRenderer(page).render(markdown);
		body += references(frontMatter);
		if ("home".equals(frontMatter.string("layout"))) body = renderHome(frontMatter, body);
		return Template.page(context, page, title(frontMatter), body, frontMatter.string("description"), "home".equals(frontMatter.string("layout")));
	}

	private String transform(FrontMatter frontMatter) {
		StringBuilder out = new StringBuilder();
		String title = title(frontMatter);
		if (!title.isBlank() && !"home".equals(frontMatter.string("layout"))) {
			out.append("# ").append(title).append(" <Badge type=\"").append(page.versionType() == VersionType.LATEST ? "tip" : "warning").append("\">")
					.append(versionLabel()).append("</Badge> {#h1}\n\n");
			if (!frontMatter.string("description").isBlank()) out.append(frontMatter.string("description")).append("\n\n");
		}
		if (page.versionType() == VersionType.OLD && !page.version().isBlank()) {
			out.append("::: warning\n")
					.append(context.translations().website(page.locale(), "version.reminder.old_version").replace("%s", page.version()))
					.append("\n:::\n\n");
		}
		out.append(replaceComponents(frontMatter.content()));
		return out.toString();
	}

	private String replaceComponents(String content) {
		return content
				.replace("<References />", "")
				.replace("<AuthorsComponent />", "")
				.replace("<Authors />", "");
	}

	private String references(FrontMatter frontMatter) {
		StringBuilder out = new StringBuilder();
		if (!frontMatter.stringList("authors").isEmpty() || !frontMatter.stringList("authors-nogithub").isEmpty()) {
			out.append("<section class=\"authors\"><h2>").append(Html.escape(context.translations().website(page.locale(), "authors.heading"))).append("</h2><ul>");
			for (String author : frontMatter.stringList("authors")) out.append("<li><a href=\"https://github.com/").append(Html.escape(author)).append("\">").append(Html.escape(author)).append("</a></li>");
			for (String author : frontMatter.stringList("authors-nogithub")) out.append("<li>").append(Html.escape(author)).append("</li>");
			out.append("</ul></section>\n");
		}
		Map<String, String> resources = frontMatter.stringMap("resources");
		if (!resources.isEmpty()) {
			out.append("<section class=\"references\"><h2>").append(Html.escape(context.translations().website(page.locale(), "references.resources"))).append("</h2><ul>");
			for (var entry : resources.entrySet()) out.append("<li><a href=\"").append(Html.escape(entry.getKey())).append("\">").append(Html.escape(entry.getValue())).append("</a></li>");
			out.append("</ul></section>\n");
		}
		return out.toString();
	}

	private String renderHome(FrontMatter frontMatter, String content) {
		Map<String, String> hero = frontMatter.stringMap("hero");
		String name = hero.getOrDefault("name", title(frontMatter));
		String tagline = hero.getOrDefault("tagline", frontMatter.string("description"));
		StringBuilder out = new StringBuilder();
		out.append("<section class=\"home-hero\"><h1>").append(Html.escape(name)).append("</h1>");
		if (!tagline.isBlank()) out.append("<p>").append(Html.escape(tagline)).append("</p>");
		out.append("</section>\n<section class=\"home-features\">");
		for (Map<String, String> feature : frontMatter.mapList("features")) {
			String link = Html.href(feature.getOrDefault("link", "#"), page);
			out.append("<a class=\"home-feature\" href=\"").append(Html.escape(link)).append("\">");
			out.append("<span class=\"home-feature-icon\">").append(Html.escape(feature.getOrDefault("icon", ""))).append("</span>");
			out.append("<strong>").append(Html.escape(feature.getOrDefault("title", ""))).append("</strong>");
			out.append("<span>").append(Html.escape(feature.getOrDefault("details", ""))).append("</span>");
			if (feature.containsKey("linkText")) out.append("<em>").append(Html.escape(feature.get("linkText"))).append("</em>");
			out.append("</a>");
		}
		out.append("</section>\n<section class=\"home-content\">").append(content).append("</section>\n");
		return out.toString();
	}

	private String title(FrontMatter frontMatter) {
		String title = frontMatter.string("title");
		return title.isBlank() ? context.translations().website(page.locale(), "title") : title;
	}

	private String versionLabel() {
		if (!page.version().isBlank()) return page.version();
		return context.versions().isEmpty() ? "latest" : context.versions().get(context.versions().size() - 1);
	}
}
