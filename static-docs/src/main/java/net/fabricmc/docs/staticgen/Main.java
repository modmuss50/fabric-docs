package net.fabricmc.docs.staticgen;

public final class Main {
	private Main() {
	}

	public static void main(String[] args) throws Exception {
		SiteGenerator.run(Options.parse(args));
	}
}
