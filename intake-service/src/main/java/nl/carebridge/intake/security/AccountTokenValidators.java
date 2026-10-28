package nl.carebridge.intake.security;

import java.util.Collection;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;

/**
 * Checks on a token from the CareBridge Account Service on top of its signature: not expired, issued
 * by the Account Service and meant for CareBridge. Its email may still be unverified; that only
 * limits the role (see CurrentUserResolver).
 */
public final class AccountTokenValidators {

	static final String AUDIENCE = "carebridge";

	private AccountTokenValidators() {
	}

	public static OAuth2TokenValidator<Jwt> forIssuer(String issuer) {
		return new DelegatingOAuth2TokenValidator<>(
				new JwtTimestampValidator(),
				new JwtClaimValidator<Object>("iss", iss -> issuer.equals(String.valueOf(iss))),
				new JwtClaimValidator<Collection<String>>("aud", aud -> aud != null && aud.contains(AUDIENCE)),
				new JwtClaimValidator<Object>("email", email -> email != null && !String.valueOf(email).isBlank()));
	}
}
