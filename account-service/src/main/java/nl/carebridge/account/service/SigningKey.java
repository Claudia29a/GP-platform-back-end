package nl.carebridge.account.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import nl.carebridge.account.config.TokenProperties;

/**
 * The RSA key that signs CareBridge tokens. Only this service holds the private key; other services
 * check signatures with the public key, published at /.well-known/jwks.json (like Google does).
 */
@Component
public class SigningKey {

	private static final Logger log = LoggerFactory.getLogger(SigningKey.class);

	private final RSAKey rsaKey;

	public SigningKey(TokenProperties properties) {
		this.rsaKey = withKeyId(properties.privateKey() == null || properties.privateKey().isBlank()
				? generate()
				: parse(properties.privateKey()));
	}

	/** Private and public key, for signing. */
	public RSAKey rsaKey() {
		return rsaKey;
	}

	/** Only the public key, safe to publish. */
	public JWKSet publicKeys() {
		return new JWKSet(rsaKey.toPublicJWK());
	}

	private static RSAKey parse(String configured) {
		try {
			String pem = configured.contains("BEGIN")
					? configured
					: new String(Base64.getMimeDecoder().decode(configured), StandardCharsets.UTF_8);
			String base64 = pem.replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "").replaceAll("\\s", "");
			KeyFactory factory = KeyFactory.getInstance("RSA");
			RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) factory
					.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
			RSAPublicKey publicKey = (RSAPublicKey) factory
					.generatePublic(new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
			return new RSAKey.Builder(publicKey).privateKey(privateKey).build();
		}
		catch (GeneralSecurityException | IllegalArgumentException | ClassCastException ex) {
			throw new IllegalStateException("carebridge.token.private-key is not a valid PKCS#8 RSA private key", ex);
		}
	}

	private static RSAKey generate() {
		log.warn("No carebridge.token.private-key configured: using a temporary signing key. "
				+ "Every restart signs everyone out; configure a key outside local development.");
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			KeyPair pair = generator.generateKeyPair();
			return new RSAKey.Builder((RSAPublicKey) pair.getPublic()).privateKey(pair.getPrivate()).build();
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Could not generate a signing key", ex);
		}
	}

	/** The key id is the key's own fingerprint, so a new key automatically gets a new id. */
	private static RSAKey withKeyId(RSAKey key) {
		try {
			return new RSAKey.Builder(key).keyID(key.computeThumbprint().toString()).build();
		}
		catch (JOSEException ex) {
			throw new IllegalStateException("Could not compute the key id", ex);
		}
	}
}
