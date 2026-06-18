package net.fabricmc.docs.staticgen;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Highlighter {
	private static final Set<String> JAVA = Set.of("abstract", "assert", "boolean", "break", "case", "catch", "default", "else", "enum", "extends", "false", "final", "for", "if", "implements", "import", "instanceof", "interface", "new", "null", "package", "private", "protected", "public", "record", "return", "static", "switch", "this", "throw", "true", "try", "var", "void", "while");

	private Highlighter() {
	}

	static String highlight(String language, String escapedCode) {
		String lang = language == null ? "" : language;
		if (lang.equals("java") || lang.equals("groovy")) return keywords(strings(comments(escapedCode)), JAVA);
		if (lang.equals("json") || lang.equals("toml") || lang.equals("properties")) return strings(escapedCode);
		if (lang.equals("xml") || lang.equals("html")) return escapedCode.replaceAll("(&lt;/?)([A-Za-z0-9:_-]+)", "$1<span class=\"tok-keyword\">$2</span>");
		return escapedCode;
	}

	private static String comments(String code) {
		return code.replaceAll("(?m)(//.*)$", "<span class=\"tok-comment\">$1</span>");
	}

	private static String strings(String code) {
		Matcher matcher = Pattern.compile("&quot;(?:\\\\.|[^&])*?&quot;").matcher(code);
		StringBuffer out = new StringBuffer();
		while (matcher.find()) matcher.appendReplacement(out, "<span class=\"tok-string\">" + Matcher.quoteReplacement(matcher.group()) + "</span>");
		matcher.appendTail(out);
		return out.toString();
	}

	private static String keywords(String code, Set<String> keywords) {
		for (String keyword : keywords) {
			code = code.replaceAll("\\b" + keyword + "\\b", "<span class=\"tok-keyword\">" + keyword + "</span>");
		}
		return code;
	}
}
