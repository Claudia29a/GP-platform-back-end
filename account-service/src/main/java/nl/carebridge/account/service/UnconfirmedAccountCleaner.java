package nl.carebridge.account.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import nl.carebridge.account.repository.AccountRepository;

/**
 * Deletes accounts whose email address was never confirmed, once they are older than the set age (GDPR storage
 * limit). Runs shortly after start-up and then on a fixed interval. With scale-to-zero the service may be asleep,
 * so the clean-up runs again as soon as it wakes.
 */
@Component
public class UnconfirmedAccountCleaner {

	private static final Logger log = LoggerFactory.getLogger(UnconfirmedAccountCleaner.class);

	private final AccountRepository repository;
	private final Duration maxAge;
	private final Clock clock;

	public UnconfirmedAccountCleaner(AccountRepository repository,
			@Value("${carebridge.cleanup.unconfirmed-account-age}") Duration maxAge, Clock clock) {
		this.repository = repository;
		this.maxAge = maxAge;
		this.clock = clock;
	}

	@Scheduled(initialDelayString = "${carebridge.cleanup.initial-delay}", fixedDelayString = "${carebridge.cleanup.interval}")
	@Transactional
	public int cleanUp() {
		int deleted = repository.deleteUnconfirmedCreatedBefore(Instant.now(clock).minus(maxAge));
		if (deleted > 0) {
			// Only the number: never who.
			log.info("Deleted {} unconfirmed account(s)", deleted);
		}
		return deleted;
	}
}
