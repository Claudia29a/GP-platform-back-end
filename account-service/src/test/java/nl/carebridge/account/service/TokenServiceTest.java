package nl.carebridge.account.service;

import static nl.carebridge.account.TestAccounts.account;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.nimbusds.jose.jwk.RSAKey;

import nl.carebridge.account.config.TokenProperties;
import nl.carebridge.account.model.Account;

class TokenServiceTest {

	private static final Instant NOW = Instant.now();

	@Test
	void issuesATokenOtherServicesCanVerifyWithThePublicKey() throws Exception {
		SigningKey key = new SigningKey(properties(""));
		TokenService tokens = new TokenService(key, properties(""), Clock.fixed(NOW, ZoneOffset.UTC));
		Account account = account("anna@example.com", "hash");

		String token = tokens.issue(account);

		RSAKey publicKey = (RSAKey) key.publicKeys().getKeys().getFirst();
		Jwt jwt = NimbusJwtDecoder.withPublicKey(publicKey.toRSAPublicKey()).build().decode(token);
		assertThat(jwt.getClaimAsString("iss")).isEqualTo("carebridge-account-service");
		assertThat(jwt.getAudience()).containsExactly("carebridge");
		assertThat(jwt.getSubject()).isEqualTo(account.getId().toString());
		assertThat(jwt.getClaimAsString("email")).isEqualTo("anna@example.com");
		assertThat(jwt.getClaimAsString("name")).isEqualTo("Anna de Vries");
		assertThat(jwt.getClaimAsBoolean("email_verified")).isFalse();
		assertThat(jwt.getHeaders()).containsEntry("kid", publicKey.getKeyID());
		assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(1));
		assertThat(tokens.lifetimeSeconds()).isEqualTo(3600);
	}

	@Test
	void publishesOnlyThePublicKey() {
		RSAKey published = (RSAKey) new SigningKey(properties("")).publicKeys().getKeys().getFirst();

		assertThat(published.isPrivate()).isFalse();
		assertThat(published.getKeyID()).isNotBlank();
	}

	@Test
	void readsTheConfiguredKeyAsPemOrBase64AndKeepsTheSameKeyId() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		String pem = "-----BEGIN PRIVATE KEY-----\n"
				+ Base64.getMimeEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded())
				+ "\n-----END PRIVATE KEY-----\n";
		String base64 = Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8));

		String fromPem = new SigningKey(properties(pem)).rsaKey().getKeyID();
		String fromBase64 = new SigningKey(properties(base64)).rsaKey().getKeyID();

		assertThat(fromPem).isEqualTo(fromBase64);
	}

	@Test
	void refusesAnInvalidKeyInsteadOfStartingWithoutOne() {
		assertThatThrownBy(() -> new SigningKey(properties("-----BEGIN PRIVATE KEY-----\nbm90IGEga2V5\n-----END PRIVATE KEY-----")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("not a valid");
	}

	private static TokenProperties properties(String privateKey) {
		return new TokenProperties("carebridge-account-service", "carebridge", Duration.ofHours(1), privateKey);
	}
}
