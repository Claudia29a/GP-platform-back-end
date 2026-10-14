package nl.carebridge.intake.security;

import java.util.Collection;
import java.util.Set;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;

/**
 * Checks on a Google ID token on top of its signature: not expired, issued by Google,
 * meant for the CareBridge client and with a verified e-mail address (staff roles depend on it).
 */
public final class GoogleTokenValidators {

	/** Google uses both spellings of its issuer. */
	static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

	private GoogleTokenValidators() {
	}

	public static OAuth2TokenValidator<Jwt> forClient(String clientId) {
		return new DelegatingOAuth2TokenValidator<>(
				new JwtTimestampValidator(),
				new JwtClaimValidator<Object>("iss", iss -> iss != null && GOOGLE_ISSUERS.contains(iss.toString())),
				new JwtClaimValidator<Collection<String>>("aud", aud -> aud != null && aud.contains(clientId)),
				new JwtClaimValidator<Object>("email_verified", verified -> "true".equals(String.valueOf(verified))));
	}
}
