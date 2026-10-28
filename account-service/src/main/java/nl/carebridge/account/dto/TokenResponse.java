package nl.carebridge.account.dto;

/** A signed token to send as "Authorization: Bearer ..." to the other CareBridge services. */
public record TokenResponse(String token, String tokenType, long expiresIn) {

	public static TokenResponse bearer(String token, long expiresInSeconds) {
		return new TokenResponse(token, "Bearer", expiresInSeconds);
	}
}
