package nl.carebridge.account.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Brute-force protection (carebridge.login.* in application.properties). */
@ConfigurationProperties(prefix = "carebridge.login")
public record LoginProperties(int maxFailedAttempts, Duration lockDuration) {
}
