package nl.carebridge.intake.security;

import java.util.Map;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Every request except the health checks needs a valid token: from Google (ADR-006) or from the
 * CareBridge Account Service (ADR-010).
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

	static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, CurrentUserResolver currentUserResolver) throws Exception {
		JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
		authenticationConverter.setJwtGrantedAuthoritiesConverter(currentUserResolver::authorities);

		return http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/intakes").hasRole(Role.PATIENT.name())
						.requestMatchers("/queue/**").hasAnyRole(Role.GP_ASSISTANT.name(), Role.GP.name())
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter)))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				// The token travels in the Authorization header, which a browser never adds by itself, so CSRF does not apply.
				.csrf(csrf -> csrf.disable())
				.build();
	}

	/**
	 * Two kinds of sign-in, one decoder: a Google ID token (ADR-006) or a token from the CareBridge
	 * Account Service (ADR-010). Each is checked with its own issuer's public keys and rules.
	 */
	@Bean
	JwtDecoder jwtDecoder(SecurityProperties properties) {
		NimbusJwtDecoder google = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build();
		google.setJwtValidator(GoogleTokenValidators.forClient(properties.googleClientId()));

		NimbusJwtDecoder account = NimbusJwtDecoder.withJwkSetUri(properties.accountJwkSetUri()).build();
		account.setJwtValidator(AccountTokenValidators.forIssuer(properties.accountIssuer()));

		return new IssuerRoutingJwtDecoder(Map.of(
				"https://accounts.google.com", google,
				"accounts.google.com", google,
				properties.accountIssuer(), account));
	}
}
