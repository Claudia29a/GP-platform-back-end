package nl.carebridge.intake.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Login settings (ADR-006). The staff e-mail lists come from the environment, never from the repository.
 */
@ConfigurationProperties(prefix = "carebridge.security")
public record SecurityProperties(String googleClientId, String accountIssuer, String accountJwkSetUri,
		List<String> gpEmails, List<String> assistantEmails) {

	public SecurityProperties {
		gpEmails = gpEmails == null ? List.of() : gpEmails;
		assistantEmails = assistantEmails == null ? List.of() : assistantEmails;
	}
}
