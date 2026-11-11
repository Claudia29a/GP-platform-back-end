package nl.carebridge.account.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.nimbusds.jose.JOSEException;

import nl.carebridge.account.config.TokenProperties;

/**
 * Reads the account from a sign-in token this service issued itself, so a user can only see or delete their own
 * account. The token must carry this service's signature and issuer and must not have expired.
 */
@Service
public class OwnTokens {

	private static final String BEARER = "Bearer ";

	private final JwtDecoder decoder;

	public OwnTokens(SigningKey signingKey, TokenProperties properties) {
		try {
			NimbusJwtDecoder nimbus = NimbusJwtDecoder.withPublicKey(signingKey.rsaKey().toRSAPublicKey())
					.signatureAlgorithm(SignatureAlgorithm.RS256).build();
			nimbus.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
			this.decoder = nimbus;
		}
		catch (JOSEException ex) {
			throw new IllegalStateException("The signing key has no usable public key", ex);
		}
	}

	public UUID accountId(String authorization) {
		if (authorization == null || !authorization.startsWith(BEARER)) {
			throw signInAgain();
		}
		try {
			return UUID.fromString(decoder.decode(authorization.substring(BEARER.length())).getSubject());
		}
		catch (JwtException | IllegalArgumentException ex) {
			throw signInAgain();
		}
	}

	private static ResponseStatusException signInAgain() {
		return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again");
	}
}
