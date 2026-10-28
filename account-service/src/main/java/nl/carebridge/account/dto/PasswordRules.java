package nl.carebridge.account.dto;

/** The password rules, in one place; the front-end shows the same rules to the user. */
public final class PasswordRules {

	public static final int MIN_LENGTH = 8;
	public static final int MAX_LENGTH = 64;
	public static final String HAS_CAPITAL = "(?s).*\\p{Lu}.*";
	public static final String HAS_NUMBER = "(?s).*\\d.*";

	private PasswordRules() {
	}
}
