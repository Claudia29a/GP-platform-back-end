package nl.carebridge.intake.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import nl.carebridge.intake.repository.IntakeRepository;

/**
 * Deletes questions older than the retention period (GDPR storage limit), then closes the gaps in the queue. The
 * access log stays: it is the record of who opened a question and holds no patient data. Runs shortly after
 * start-up and then on a fixed interval, so it also runs when the service wakes up after sleeping.
 */
@Component
public class QuestionCleaner {

	private static final Logger log = LoggerFactory.getLogger(QuestionCleaner.class);

	private final IntakeRepository repository;
	private final QueueService queueService;
	private final Duration retention;
	private final Clock clock;

	public QuestionCleaner(IntakeRepository repository, QueueService queueService,
			@Value("${carebridge.retention.question-age}") Duration retention, Clock clock) {
		this.repository = repository;
		this.queueService = queueService;
		this.retention = retention;
		this.clock = clock;
	}

	@Scheduled(initialDelayString = "${carebridge.retention.initial-delay}", fixedDelayString = "${carebridge.retention.interval}")
	@Transactional
	public int cleanUp() {
		int deleted = repository.deleteSubmittedBefore(Instant.now(clock).minus(retention));
		if (deleted > 0) {
			queueService.closeGaps();
			// Only the number: never whose.
			log.info("Deleted {} question(s) older than the retention period", deleted);
		}
		return deleted;
	}
}
