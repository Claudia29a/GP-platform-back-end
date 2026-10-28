package nl.carebridge.intake.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

class AccountTokenValidatorsTest {

	private final OAuth2TokenValidator<Jwt> validator = AccountTokenValidators.forIssuer("carebridge-account-service");

	@Test
	void acceptsATokenFromTheAccountService() {
		assertThat(valid(token(claims -> { }))).isTrue();
	}

	@Test
	void acceptsAnUnverifiedEmailBecauseThatOnlyLimitsTheRole() {
		assertThat(valid(token(claims -> claims.claim("email_verified", false)))).isTrue();
	}

	@Test
	void rejectsAnotherIssuer() {
		assertThat(valid(token(claims -> claims.claim("iss", "someone-else")))).isFalse();
	}

	@Test
	void rejectsATokenMeantForAnotherAudience() {
		assertThat(valid(token(claims -> claims.audience(List.of("other-app"))))).isFalse();
	}

	@Test
	void rejectsATokenWithoutEmail() {
		assertThat(valid(token(claims -> claims.claims(map -> map.remove("email"))))).isFalse();
	}

	@Test
	void rejectsAnExpiredToken() {
		Instant past = Instant.now().minusSeconds(7200);
		assertThat(valid(token(claims -> claims.issuedAt(past).expiresAt(past.plusSeconds(3600))))).isFalse();
	}

	private boolean valid(Jwt token) {
		return !validator.validate(token).hasErrors();
	}

	private static Jwt token(Consumer<Jwt.Builder> changes) {
		Instant now = Instant.now();
		Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256")
				.claim("iss", "carebridge-account-service")
				.audience(List.of("carebridge"))
				.subject("7d1b6c2e-0000-4000-8000-000000000001")
				.claim("email", "bram@example.com")
				.claim("email_verified", false)
				.issuedAt(now)
				.expiresAt(now.plusSeconds(3600));
		changes.accept(builder);
		return builder.build();
	}
}
