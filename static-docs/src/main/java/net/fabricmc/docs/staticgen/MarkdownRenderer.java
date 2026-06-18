package net.fabricmc.docs.staticgen;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MarkdownRenderer {
	private final Page page;
	private final StringBuilder html = new StringBuilder();
	private boolean paragraph;
	private boolean list;
	private boolean table;
	private boolean code;
	private boolean choiceComponent;
	private String choiceName;
	private String codeLanguage;
	private final StringBuilder codeBuffer = new StringBuilder();
	private final Deque<String> containers = new ArrayDeque<>();

	MarkdownRenderer(Page page) {
		this.page = page;
	}

	String render(String markdown) {
		for (String raw : markdown.split("\\R", -1)) renderLine(raw);
		closeParagraph();
		closeList();
		closeTable();
		if (choiceComponent) {
			html.append("</div>\n");
			choiceComponent = false;
		}
		while (!containers.isEmpty()) html.append(containers.pop());
		return html.toString();
	}

	private void renderLine(String raw) {
		String line = raw.stripTrailing();
		if (code) {
			if (line.startsWith("```")) {
				closeCode();
			} else {
				codeBuffer.append(line).append('\n');
			}
			return;
		}

		if (line.startsWith("```")) {
			closeParagraph();
			closeList();
			closeTable();
			code = true;
			codeLanguage = line.substring(3).trim().split("\\s+")[0];
			return;
		}

		if (line.isBlank()) {
			closeParagraph();
			closeList();
			closeTable();
			return;
		}

		if (choiceComponent && choiceLine(line)) return;
		if (component(line)) return;
		if (unsupportedComponent(line)) throw new IllegalArgumentException("Unsupported component syntax: " + line);
		if (container(line)) return;

		Matcher heading = Pattern.compile("^(#{1,6})\\s+(.+?)(?:\\s+\\{#([^}]+)})?$").matcher(line);
		if (heading.matches()) {
			closeParagraph();
			closeList();
			closeTable();
			int level = heading.group(1).length();
			String id = heading.group(3) == null ? slug(heading.group(2)) : heading.group(3);
			html.append("<h").append(level).append(" id=\"").append(Html.escape(id)).append("\">")
					.append(Html.inline(heading.group(2), page)).append("</h").append(level).append(">\n");
			return;
		}

		if (line.startsWith("|") && line.endsWith("|")) {
			closeParagraph();
			closeList();
			renderTableLine(line);
			return;
		}

		Matcher item = Pattern.compile("^\\s*(?:[-*]|\\d+[.])\\s+(.+)$").matcher(line);
		if (item.matches()) {
			closeParagraph();
			closeTable();
			if (!list) {
				html.append("<ul>\n");
				list = true;
			}
			html.append("<li>").append(Html.inline(item.group(1), page)).append("</li>\n");
			return;
		}

		if (line.startsWith("> ")) {
			closeParagraph();
			closeList();
			closeTable();
			html.append("<blockquote><p>").append(Html.inline(line.substring(2), page)).append("</p></blockquote>\n");
			return;
		}

		if (!paragraph) {
			closeList();
			closeTable();
			html.append("<p>");
			paragraph = true;
		} else {
			html.append(' ');
		}
		html.append(Html.inline(line, page));
	}

	private boolean component(String line) {
		if (line.startsWith("<Badge")) {
			html.append(componentBadge(line)).append('\n');
			return true;
		}
		if (line.startsWith("<DownloadEntry")) {
			closeParagraph();
			html.append(componentDownload(line)).append('\n');
			return true;
		}
		if (line.startsWith("<VideoPlayer")) {
			closeParagraph();
			html.append(componentVideo(line)).append('\n');
			return true;
		}
		if (line.startsWith("<ColorSwatch")) {
			html.append(componentColor(line)).append('\n');
			return true;
		}
		if (line.startsWith("<Range")) {
			html.append(componentRange(line)).append('\n');
			return true;
		}
		if (line.startsWith("<ChoiceComponent")) {
			closeParagraph();
			html.append("<div class=\"choice-grid\">\n");
			choiceComponent = true;
			return true;
		}
		return false;
	}

	private boolean choiceLine(String line) {
		if (line.matches("\\s*name:\\s*'[^']+',?")) {
			choiceName = line.substring(line.indexOf('\'') + 1, line.lastIndexOf('\''));
			return true;
		}
		if (line.matches("\\s*href:\\s*'[^']+',?")) {
			String href = line.substring(line.indexOf('\'') + 1, line.lastIndexOf('\''));
			String name = choiceName == null ? href : choiceName;
			html.append("<a class=\"choice-card\" href=\"").append(Html.escape(Html.href(href, page))).append("\" data-choice=\"")
					.append(Html.escape(name)).append("\">").append(Html.escape(name)).append("</a>\n");
			choiceName = null;
			return true;
		}
		String trimmed = line.trim();
		if (trimmed.matches("]\\s*/>.*")) {
			html.append("</div>\n");
			choiceComponent = false;
			return true;
		}
		return true;
	}

	private boolean unsupportedComponent(String line) {
		return line.matches("^<[A-Z][A-Za-z0-9]*(\\s|>|/).*") && !line.matches("^<[A-Z]>(.*)</[A-Z]>$");
	}

	private boolean container(String line) {
		Matcher open = Pattern.compile("^(:::+)\\s*([a-zA-Z-]+)(?:\\s+(.*))?$").matcher(line);
		if (open.matches()) {
			closeParagraph();
			closeList();
			closeTable();
			String kind = open.group(2);
			String title = open.group(3) == null ? kind : open.group(3);
			if (kind.equals("code-group") || kind.equals("tabs")) {
				html.append("<div class=\"").append(kind.equals("tabs") ? "tabs" : "code-group").append("\">\n");
				containers.push("</div>\n");
			} else if (kind.equals("details")) {
				html.append("<details class=\"custom-block\"><summary>").append(Html.inline(title, page)).append("</summary>\n");
				containers.push("</details>\n");
			} else {
				html.append("<div class=\"custom-block ").append(Html.escape(kind)).append("\"><p class=\"custom-block-title\">")
						.append(Html.inline(title, page)).append("</p>\n");
				containers.push("</div>\n");
			}
			return true;
		}
		if (line.matches("^:::+\\s*$")) {
			closeParagraph();
			closeList();
			closeTable();
			if (!containers.isEmpty()) html.append(containers.pop());
			return true;
		}
		return false;
	}

	private void renderTableLine(String line) {
		if (line.matches("^\\|\\s*:?-+:?\\s*(\\|\\s*:?-+:?\\s*)+\\|$")) return;
		String[] cells = line.substring(1, line.length() - 1).split("\\|");
		if (!table) {
			html.append("<table><tbody>\n");
			table = true;
		}
		html.append("<tr>");
		for (String cell : cells) html.append("<td>").append(Html.inline(cell.trim(), page)).append("</td>");
		html.append("</tr>\n");
	}

	private void closeCode() {
		String codeHtml = Html.escape(codeBuffer.toString());
		codeHtml = codeHtml.replaceAll("(?m)^(.*) // @@@highlight$", "<span class=\"line-highlight\">$1</span>");
		codeHtml = codeHtml.replaceAll("(?m)^(.*) // @@@focus$", "<span class=\"line-highlight\">$1</span>");
		codeHtml = codeHtml.replaceAll("(?m)^(.*) // @@@warning$", "<span class=\"line-warning\">$1</span>");
		codeHtml = codeHtml.replaceAll("(?m)^(.*) // @@@error$", "<span class=\"line-error\">$1</span>");
		codeHtml = Highlighter.highlight(codeLanguage, codeHtml);
		html.append("<pre><code class=\"language-").append(Html.escape(codeLanguage.isBlank() ? "text" : codeLanguage)).append("\">")
				.append(codeHtml).append("</code></pre>\n");
		code = false;
		codeBuffer.setLength(0);
	}

	private void closeParagraph() {
		if (paragraph) {
			html.append("</p>\n");
			paragraph = false;
		}
	}

	private void closeList() {
		if (list) {
			html.append("</ul>\n");
			list = false;
		}
	}

	private void closeTable() {
		if (table) {
			html.append("</tbody></table>\n");
			table = false;
		}
	}

	private static String slug(String text) {
		return text.toLowerCase(java.util.Locale.ROOT).replaceAll("<[^>]+>", "").replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
	}

	private String componentBadge(String line) {
		String type = attr(line, "type");
		String text = line.replaceAll("^<Badge[^>]*>", "").replaceAll("</Badge>$", "");
		return "<span class=\"badge " + Html.escape(type) + "\">" + Html.escape(text) + "</span>";
	}

	private String componentDownload(String line) {
		String visual = attr(line, "visualURL");
		String download = attr(line, "downloadURL");
		String text = line.replaceAll("^<DownloadEntry[^>]*>", "").replaceAll("</DownloadEntry>$", "");
		return "<div class=\"download-entry\"><img src=\"" + Html.href(visual, page) + "\" alt=\"\"><a href=\"" + Html.href(download, page) + "\" download>" + Html.escape(text) + "</a></div>";
	}

	private String componentVideo(String line) {
		String src = attr(line, "src");
		String text = line.replaceAll("^<VideoPlayer[^>]*>", "").replaceAll("</VideoPlayer>$", "");
		return "<p><a href=\"" + Html.escape(src) + "\">" + Html.escape(text.isBlank() ? src : text) + "</a></p>";
	}

	private static String componentColor(String line) {
		String color = attr(line, "color");
		return "<span class=\"swatch\" style=\"background:" + Html.escape(color) + "\"></span>";
	}

	private static String componentRange(String line) {
		String range = attr(line, "range");
		String href = "https://jubianchi.github.io/semver-check/#/" + Html.escape(range);
		return "<a href=\"" + href + "\"><code>" + Html.escape(range) + "</code></a>";
	}

	private static String attr(String line, String name) {
		Matcher matcher = Pattern.compile(name + "=\"([^\"]*)\"").matcher(line);
		return matcher.find() ? matcher.group(1) : "";
	}
}
