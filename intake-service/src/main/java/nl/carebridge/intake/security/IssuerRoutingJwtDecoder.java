package nl.carebridge.intake.security;

import java.text.ParseException;
import java.util.Map;

import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import com.nimbusds.jwt.JWTParser;

/**
 * Sends each token to the decoder of the party that issued it. The issuer is read from the token
 * before its signature is checked, but only to pick the decoder: that decoder then checks the
 * signature with the issuer's own keys, so a forged issuer gets nowhere. Unknown issuers are refused.
 */
public class IssuerRoutingJwtDecoder implements JwtDecoder {

	private final Map<String, JwtDecoder> decodersByIssuer;

	public IssuerRoutingJwtDecoder(Map<String, JwtDecoder> decodersByIssuer) {
		this.decodersByIssuer = Map.copyOf(decodersByIssuer);
	}

	@Override
	public Jwt decode(String token) throws JwtException {
		JwtDecoder decoder = decodersByIssuer.get(issuerOf(token));
		if (decoder == null) {
			throw new BadJwtException("Token from an unknown issuer");
		}
		return decoder.decode(token);
	}

	private static String issuerOf(String token) {
		try {
			String issuer = JWTParser.parse(token).getJWTClaimsSet().getIssuer();
			if (issuer == null) {
				throw new BadJwtException("Token without an issuer");
			}
			return issuer;
		}
		catch (ParseException ex) {
			throw new BadJwtException("Malformed token", ex);
		}
	}
}
