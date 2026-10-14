package nl.carebridge.intake.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

class GoogleTokenValidatorsTest {

	private static final String CLIENT_ID = "carebridge.apps.googleusercontent.com";

	private final OAuth2TokenValidator<Jwt> validator = GoogleTokenValidators.forClient(CLIENT_ID);

	@Test
	void acceptsValidGoogleToken() {
		assertThat(valid(token(claims -> { }))).isTrue();
	}

	@Test
	void acceptsBothGoogleIssuerSpellings() {
		assertThat(valid(token(claims -> claims.claim("iss", "accounts.google.com")))).isTrue();
	}

	@Test
	void rejectsTokenFromAnotherIssuer() {
		assertThat(valid(token(claims -> claims.claim("iss", "https://evil.example.com")))).isFalse();
	}

	@Test
	void rejectsTokenMeantForAnotherApp() {
		assertThat(valid(token(claims -> claims.audience(List.of("other-app.apps.googleusercontent.com"))))).isFalse();
	}

	@Test
	void rejectsUnverifiedEmail() {
		assertThat(valid(token(claims -> claims.claim("email_verified", false)))).isFalse();
	}

	@Test
	void rejectsExpiredToken() {
		Instant past = Instant.now().minusSeconds(7200);
		assertThat(valid(token(claims -> claims.issuedAt(past).expiresAt(past.plusSeconds(3600))))).isFalse();
	}

	private boolean valid(Jwt token) {
		return !validator.validate(token).hasErrors();
	}

	private static Jwt token(Consumer<Jwt.Builder> changes) {
		Instant now = Instant.now();
		Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256")
				.claim("iss", "https://accounts.google.com")
				.audience(List.of(CLIENT_ID))
				.subject("google-anna")
				.claim("email", "anna@example.com")
				.claim("email_verified", true)
				.issuedAt(now)
				.expiresAt(now.plusSeconds(3600));
		changes.accept(builder);
		return builder.build();
	}
}
