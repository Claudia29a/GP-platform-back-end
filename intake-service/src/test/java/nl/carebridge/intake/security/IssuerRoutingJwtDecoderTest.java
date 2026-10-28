package nl.carebridge.intake.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;

class IssuerRoutingJwtDecoderTest {

	private final JwtDecoder google = mock(JwtDecoder.class);
	private final JwtDecoder account = mock(JwtDecoder.class);
	private final IssuerRoutingJwtDecoder decoder = new IssuerRoutingJwtDecoder(Map.of(
			"https://accounts.google.com", google,
			"carebridge-account-service", account));

	@Test
	void sendsEachTokenToTheDecoderOfItsIssuer() {
		String googleToken = tokenFrom("https://accounts.google.com");
		String accountToken = tokenFrom("carebridge-account-service");
		Jwt decoded = mock(Jwt.class);
		when(google.decode(googleToken)).thenReturn(decoded);
		when(account.decode(accountToken)).thenReturn(decoded);

		assertThat(decoder.decode(googleToken)).isSameAs(decoded);
		assertThat(decoder.decode(accountToken)).isSameAs(decoded);
		verify(google, never()).decode(accountToken);
	}

	@Test
	void refusesTokensFromAnUnknownIssuer() {
		assertThatThrownBy(() -> decoder.decode(tokenFrom("https://evil.example.com")))
				.isInstanceOf(BadJwtException.class).hasMessageContaining("unknown issuer");
	}

	@Test
	void refusesTokensWithoutAnIssuer() {
		assertThatThrownBy(() -> decoder.decode(new PlainJWT(new JWTClaimsSet.Builder().subject("x").build()).serialize()))
				.isInstanceOf(BadJwtException.class).hasMessageContaining("without an issuer");
	}

	@Test
	void refusesSomethingThatIsNotAToken() {
		assertThatThrownBy(() -> decoder.decode("not-a-token")).isInstanceOf(BadJwtException.class);
	}

	private static String tokenFrom(String issuer) {
		return new PlainJWT(new JWTClaimsSet.Builder().issuer(issuer).subject("someone").build()).serialize();
	}
}
