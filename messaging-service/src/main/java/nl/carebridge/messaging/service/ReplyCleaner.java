package nl.carebridge.messaging.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import nl.carebridge.messaging.repository.ReplyRepository;

/**
 * Deletes replies once their question is older than the retention period, the same 6 months after which the Intake
 * Service deletes the question itself (GDPR storage limit). Runs shortly after start-up and then on a fixed interval.
 */
@Component
public class ReplyCleaner {

	private static final Logger log = LoggerFactory.getLogger(ReplyCleaner.class);

	private final ReplyRepository repository;
	private final Duration retention;
	private final Clock clock;

	public ReplyCleaner(ReplyRepository repository, @Value("${carebridge.retention.question-age}") Duration retention, Clock clock) {
		this.repository = repository;
		this.retention = retention;
		this.clock = clock;
	}

	@Scheduled(initialDelayString = "${carebridge.retention.initial-delay}", fixedDelayString = "${carebridge.retention.interval}")
	@Transactional
	public int cleanUp() {
		int deleted = repository.deleteQuestionSubmittedBefore(Instant.now(clock).minus(retention));
		if (deleted > 0) {
			// Only the number: never whose.
			log.info("Deleted {} reply(ies) whose question passed the retention period", deleted);
		}
		return deleted;
	}
}
