package nl.carebridge.account.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import nl.carebridge.account.config.TokenProperties;
import nl.carebridge.account.model.Account;

/**
 * Issues signed tokens (JWT, RS256) with the same claims a Google ID token has (sub, email, name,
 * email_verified), so the other services treat both kinds of sign-in the same way (ADR-010).
 */
@Service
public class TokenService {

	private final JwtEncoder encoder;
	private final String keyId;
	private final TokenProperties properties;
	private final Clock clock;

	public TokenService(SigningKey signingKey, TokenProperties properties, Clock clock) {
		this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(signingKey.rsaKey())));
		this.keyId = signingKey.rsaKey().getKeyID();
		this.properties = properties;
		this.clock = clock;
	}

	public String issue(Account account) {
		Instant now = Instant.now(clock);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.issuer())
				.audience(List.of(properties.audience()))
				.subject(account.getId().toString())
				.issuedAt(now)
				.expiresAt(now.plus(properties.lifetime()))
				.claim("email", account.getEmail())
				.claim("name", account.getName())
				.claim("email_verified", account.isEmailVerified())
				.build();
		JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(keyId).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public long lifetimeSeconds() {
		return properties.lifetime().toSeconds();
	}
}
