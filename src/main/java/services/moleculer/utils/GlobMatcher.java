package services.moleculer.utils;

public final class GlobMatcher {

	public static final boolean matches(String text, String pattern) {
		String rest = null;
		int pos = pattern.indexOf('*');
		if (pos != -1) {
			rest = pattern.substring(pos + 1);
			pattern = pattern.substring(0, pos);
		}
		if (pattern.length() > text.length()) {
			return false;
		}

		// Handle the part up to the first *
		for (int i = 0; i < pattern.length(); i++) {
			if (pattern.charAt(i) != '?' && !pattern.substring(i, i + 1).equalsIgnoreCase(text.substring(i, i + 1))) {
				return false;
			}
		}

		// Recurse for the part after the first *, if any
		if (rest == null) {
			return pattern.length() == text.length();
		} else {
			for (int i = pattern.length(); i <= text.length(); i++) {
				if (matches(text.substring(i), rest)) {
					return true;
				}
			}
			return false;
		}
	}

}
