package net.fabricmc.docs.staticgen;

import java.nio.file.Path;

record Page(Path root, Path source, String route, Path outputPath, String locale, String version, VersionType versionType) {
	static Page from(Path root, Path relative) {
		String unix = relative.toString().replace('\\', '/');
		String locale = "en_us";
		String version = "";
		VersionType type = VersionType.LATEST;
		String routeSource = unix;

		if (unix.startsWith("versions/")) {
			String[] split = unix.split("/", 4);
			version = split.length > 1 ? split[1] : "";
			type = VersionType.OLD;
			routeSource = split.length > 2 ? version + "/" + unix.substring(("versions/" + version + "/").length()) : unix;

			String prefix = version + "/translated/";
			if (routeSource.startsWith(prefix)) {
				String rest = routeSource.substring(prefix.length());
				int slash = rest.indexOf('/');
				if (slash > 0) {
					locale = rest.substring(0, slash);
					routeSource = version + "/" + locale + "/" + rest.substring(slash + 1);
				}
			}
		} else if (unix.startsWith("translated/")) {
			String rest = unix.substring("translated/".length());
			int slash = rest.indexOf('/');
			if (slash > 0) {
				locale = rest.substring(0, slash);
				routeSource = locale + "/" + rest.substring(slash + 1);
			}
		}

		String withoutMd = routeSource.substring(0, routeSource.length() - ".md".length());
		String route = withoutMd.endsWith("/index") ? withoutMd.substring(0, withoutMd.length() - "/index".length()) + "/" : withoutMd;
		if (route.equals("index")) route = "/";
		if (!route.startsWith("/")) route = "/" + route;

		Path output = route.equals("/")
				? Path.of("index.html")
				: route.endsWith("/") ? Path.of(route.substring(1), "index.html") : Path.of(route.substring(1) + ".html");
		return new Page(root, relative, route, output, locale, version, type);
	}

	Path absoluteSource() {
		return root.resolve(source);
	}
}
