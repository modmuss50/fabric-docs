package net.fabricmc.docs.staticgen;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Html {
	private Html() {
	}

	static String escape(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	static String inline(String text, Page page) {
		String escaped = escape(text);
		escaped = escaped.replaceAll("&lt;Badge type=&quot;([^&]+)&quot;&gt;([^&]+)&lt;/Badge&gt;", "<span class=\"badge $1\">$2</span>");
		escaped = escaped.replaceAll("`([^`]+)`", "<code>$1</code>");
		escaped = replaceLinks(escaped, page);
		escaped = escaped.replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>");
		escaped = escaped.replaceAll("(?<!\\*)\\*([^*]+)\\*(?!\\*)", "<em>$1</em>");
		return escaped;
	}

	static String replaceLinks(String escaped, Page page) {
		Matcher images = Pattern.compile("!\\[([^]]*)]\\(([^)]+)\\)").matcher(escaped);
		StringBuffer imageBuffer = new StringBuffer();
		while (images.find()) {
			images.appendReplacement(imageBuffer, Matcher.quoteReplacement("<img src=\"" + href(images.group(2), page) + "\" alt=\"" + images.group(1) + "\" loading=\"lazy\">"));
		}
		images.appendTail(imageBuffer);

		Matcher links = Pattern.compile("\\[([^]]+)]\\(([^)]+)\\)").matcher(imageBuffer.toString());
		StringBuffer linkBuffer = new StringBuffer();
		while (links.find()) {
			links.appendReplacement(linkBuffer, Matcher.quoteReplacement("<a href=\"" + href(links.group(2), page) + "\">" + links.group(1) + "</a>"));
		}
		links.appendTail(linkBuffer);
		return linkBuffer.toString();
	}

	static String href(String href, Page page) {
		if (href.startsWith("http://") || href.startsWith("https://") || href.startsWith("#")) return href;
		if (href.startsWith("/")) return relativeAssetPrefix(page) + href.substring(1);
		if (href.endsWith(".md")) href = href.substring(0, href.length() - 3);
		if (!href.contains(".") && !href.endsWith("/") && !href.contains("#")) href += ".html";
		return href;
	}

	static String relativeAssetPrefix(Page page) {
		int depth = Math.max(0, page.outputPath().getNameCount() - 1);
		return "../".repeat(depth);
	}
}
