package net.fabricmc.docs.staticgen;

final class Template {
	private Template() {
	}

	static String page(BuildContext context, Page page, String title, String body, String description, boolean home) {
		String prefix = page == null ? "" : Html.relativeAssetPrefix(page);
		String locale = page == null ? "en_us" : page.locale();
		String siteTitle = context.translations().website(locale, "title");
		String content = home || page == null
				? "<div class=\"home-layout\"><main class=\"home-main\">" + body + "</main></div>"
				: "<div class=\"layout\"><aside class=\"sidebar\">" + Sidebar.render(context, page) + "</aside><main>" + body + "</main></div>";
		return """
				<!doctype html>
				<html lang="%s">
				<head>
					<meta charset="utf-8">
					<meta name="viewport" content="width=device-width, initial-scale=1">
					<title>%s - %s</title>
					<meta name="description" content="%s">
					<link rel="stylesheet" href="%sstatic-docs/site.css">
				</head>
				<body>
					<header class="topbar">
						<a class="brand" href="%sindex.html"><img src="%slogo.png" alt=""> %s</a>
						<nav>
							<a href="%scontributing.html">%s</a>
							%s
							%s
							<a href="https://github.com/FabricMC/fabric-docs">GitHub</a>
						</nav>
					</header>
					%s
					<footer>%s</footer>
					<script src="%sstatic-docs/highlight.js"></script>
				</body>
				</html>
				""".formatted(
				locale.replace('_', '-'),
				Html.escape(title),
				Html.escape(siteTitle),
				Html.escape(description),
				prefix,
				prefix,
				prefix,
				Html.escape(siteTitle),
				prefix,
				Html.escape(context.translations().website(locale, "nav.contribute")),
				versionLinks(context, page),
				languageLinks(context, page),
				content,
				context.translations().website(locale, "footer.message"),
				prefix
		);
	}

	static String page(BuildContext context, Page page, String title, String body, String description) {
		return page(context, page, title, body, description, false);
	}

	private static String versionLinks(BuildContext context, Page page) {
		if (page == null || context.versions().isEmpty()) return "";
		StringBuilder out = new StringBuilder("<details class=\"nav-menu\"><summary>");
		out.append(Html.escape(context.versions().isEmpty() ? "Minecraft latest" : "Minecraft " + context.versions().get(context.versions().size() - 1))).append("</summary>");
		for (String version : context.versions()) {
			String route = version + page.route();
			if (context.routes().containsKey("/" + route.replaceAll("^/+", ""))) {
				out.append("<a href=\"").append(Html.relativeAssetPrefix(page)).append(Html.escape(route.replaceAll("^/+", ""))).append("\">Minecraft ").append(Html.escape(version)).append("</a>");
			}
		}
		return out.append("</details>").toString();
	}

	private static String languageLinks(BuildContext context, Page page) {
		if (page == null) return "";
		StringBuilder out = new StringBuilder("<details class=\"nav-menu\"><summary>");
		out.append(Html.escape(page.locale())).append("</summary>");
		for (String locale : context.locales()) {
			String route = locale.equals("en_us") ? page.route() : "/" + locale + page.route();
			if (page.route().startsWith("/" + page.locale() + "/")) {
				String unlocalized = page.route().substring(page.locale().length() + 1);
				route = locale.equals("en_us") ? unlocalized : "/" + locale + unlocalized;
			}
			if (context.routes().containsKey(route)) {
				out.append("<a href=\"").append(Html.relativeAssetPrefix(page)).append(Html.escape(route.replaceAll("^/+", "").isBlank() ? "index.html" : route.replaceAll("^/+", "") + (route.endsWith("/") ? "index.html" : ".html"))).append("\">")
						.append(Html.escape(locale)).append("</a>");
			}
		}
		return out.append("</details>").toString();
	}
}
