package net.fabricmc.docs.staticgen;

import java.nio.file.Path;
import java.util.Locale;

record Options(Path root, Path out, int threads, boolean strict) {
	static Options parse(String[] args) {
		Path root = Path.of("..");
		Path out = Path.of("build/site");
		int threads = Runtime.getRuntime().availableProcessors();
		boolean strict = true;

		for (int i = 0; i < args.length; i++) {
			String key = args[i];
			String value = switch (key) {
			case "--root", "--out", "--threads", "--strict" -> {
				if (i + 1 >= args.length) throw new IllegalArgumentException("Missing value for " + key);
				yield args[++i];
			}
			default -> throw new IllegalArgumentException("Unknown argument: " + key);
			};

			switch (key) {
			case "--root" -> root = Path.of(value);
			case "--out" -> out = Path.of(value);
			case "--threads" -> threads = value.toLowerCase(Locale.ROOT).equals("auto")
					? Runtime.getRuntime().availableProcessors()
					: Integer.parseInt(value);
			case "--strict" -> strict = Boolean.parseBoolean(value);
			default -> throw new IllegalStateException(key);
			}
		}

		return new Options(root.toAbsolutePath().normalize(), out.toAbsolutePath().normalize(), Math.max(1, threads), strict);
	}
}
