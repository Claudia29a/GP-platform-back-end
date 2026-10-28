package nl.carebridge.account.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings for the tokens this service signs (carebridge.token.* in application.properties). */
@ConfigurationProperties(prefix = "carebridge.token")
public record TokenProperties(String issuer, String audience, Duration lifetime, String privateKey) {
}
