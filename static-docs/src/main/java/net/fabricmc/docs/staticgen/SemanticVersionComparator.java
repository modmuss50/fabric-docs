package net.fabricmc.docs.staticgen;

import java.util.Comparator;

enum SemanticVersionComparator implements Comparator<String> {
	INSTANCE;

	@Override
	public int compare(String left, String right) {
		String[] a = left.split("[.+-]");
		String[] b = right.split("[.+-]");
		int max = Math.max(a.length, b.length);
		for (int i = 0; i < max; i++) {
			String av = i < a.length ? a[i] : "0";
			String bv = i < b.length ? b[i] : "0";
			int result = comparePart(av, bv);
			if (result != 0) return result;
		}
		return left.compareTo(right);
	}

	private static int comparePart(String left, String right) {
		if (left.matches("\\d+") && right.matches("\\d+")) return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
		return left.compareTo(right);
	}
}
