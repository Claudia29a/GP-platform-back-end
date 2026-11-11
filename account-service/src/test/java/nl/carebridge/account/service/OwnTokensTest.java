package nl.carebridge.account.service;

import static nl.carebridge.account.TestAccounts.account;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import nl.carebridge.account.config.TokenProperties;
import nl.carebridge.account.model.Account;

@Tag("unit")
class OwnTokensTest {

	private final TokenProperties properties = new TokenProperties("carebridge-account-service", "carebridge", Duration.ofHours(1), "");
	private final SigningKey signingKey = new SigningKey(properties);
	private final OwnTokens ownTokens = new OwnTokens(signingKey, properties);
	private final Account anna = account("anna@example.com", "$2a$10$hash");

	@Test
	void findsTheAccountInATokenThisServiceIssued() {
		String token = new TokenService(signingKey, properties, Clock.systemUTC()).issue(anna);

		assertThat(ownTokens.accountId("Bearer " + token)).isEqualTo(anna.getId());
	}

	@Test
	void refusesAMissingOrMalformedHeader() {
		assertThatThrownBy(() -> ownTokens.accountId(null)).satisfies(OwnTokensTest::isUnauthorized);
		assertThatThrownBy(() -> ownTokens.accountId("Basic abc")).satisfies(OwnTokensTest::isUnauthorized);
		assertThatThrownBy(() -> ownTokens.accountId("Bearer not-a-token")).satisfies(OwnTokensTest::isUnauthorized);
	}

	@Test
	void refusesATokenSignedWithAnotherKey() {
		String forged = new TokenService(new SigningKey(properties), properties, Clock.systemUTC()).issue(anna);

		assertThatThrownBy(() -> ownTokens.accountId("Bearer " + forged)).satisfies(OwnTokensTest::isUnauthorized);
	}

	@Test
	void refusesAnExpiredToken() {
		Clock twoHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
		String expired = new TokenService(signingKey, properties, twoHoursAgo).issue(anna);

		assertThatThrownBy(() -> ownTokens.accountId("Bearer " + expired)).satisfies(OwnTokensTest::isUnauthorized);
	}

	private static void isUnauthorized(Throwable ex) {
		assertThat(ex).isInstanceOf(ResponseStatusException.class);
		assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}
}
