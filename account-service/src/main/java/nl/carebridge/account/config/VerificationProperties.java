package nl.carebridge.account.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Email verification and the sign-up limit (carebridge.verification.* in application.properties). */
@ConfigurationProperties(prefix = "carebridge.verification")
public record VerificationProperties(Duration codeLifetime, int maxAttempts, Duration resendInterval, int maxCodesPerHour,
		int maxSignUpsPerHour) {
}
