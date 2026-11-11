package nl.carebridge.account.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.carebridge.account.config.VerificationProperties;
import nl.carebridge.account.encryption.DataProtection;
import nl.carebridge.account.model.SignUp;
import nl.carebridge.account.repository.SignUpRepository;

/** Stops one visitor creating accounts in bulk: at most a few per hour from the same IP address. */
@Service
public class SignUpLimiter {

	static final String TOO_MANY = "Too many accounts were created from this device. Please try again later.";
	private static final Duration WINDOW = Duration.ofHours(1);

	private final SignUpRepository repository;
	private final DataProtection protection;
	private final VerificationProperties properties;
	private final Clock clock;

	public SignUpLimiter(SignUpRepository repository, DataProtection protection, VerificationProperties properties, Clock clock) {
		this.repository = repository;
		this.protection = protection;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public void check(Optional<String> address) {
		if (address.isEmpty()) {
			return;
		}
		String ipHash = protection.hash(address.get());
		Instant since = Instant.now(clock).minus(WINDOW);
		if (repository.countByIpHashAndCreatedAtAfter(ipHash, since) >= properties.maxSignUpsPerHour()) {
			Instant oldest = repository.findOldestCreatedAt(ipHash, since).orElse(since);
			throw new TooManyRequestsException(TOO_MANY, Duration.between(since, oldest));
		}
	}

	@Transactional
	public void record(Optional<String> address) {
		if (address.isEmpty()) {
			return;
		}
		Instant now = Instant.now(clock);
		repository.deleteByCreatedAtBefore(now.minus(WINDOW));
		repository.save(new SignUp(protection.hash(address.get()), now));
	}
}
