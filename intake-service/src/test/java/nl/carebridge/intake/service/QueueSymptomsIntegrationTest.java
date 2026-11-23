package nl.carebridge.intake.service;

import static nl.carebridge.intake.TestIntakes.validRequest;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import nl.carebridge.intake.dto.QueueItemResponse;
import nl.carebridge.intake.model.Symptom;
import nl.carebridge.intake.repository.IntakeRepository;
import nl.carebridge.intake.security.CurrentUser;
import nl.carebridge.intake.security.Role;

/** Story 8 TAC1: the ticked symptoms reach the queue from a real PostgreSQL, decrypted and in form order. */
@SpringBootTest
@Testcontainers
@Tag("integration")
class QueueSymptomsIntegrationTest {

	private static final CurrentUser PATIENT = new CurrentUser("google-anna", "Anna", "anna@example.com", Role.PATIENT);

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private IntakeService intakeService;

	@Autowired
	private QueueService queueService;

	@Autowired
	private PrivacyConsentService privacyConsentService;

	@Autowired
	private IntakeRepository repository;

	@Test
	void theQueueCarriesTheSymptomsTheyTicked() {
		repository.deleteAll();
		privacyConsentService.accept(PATIENT);
		intakeService.submit(validRequest(), PATIENT);

		QueueItemResponse item = QueueItemResponse.from(queueService.currentQueue().getFirst());

		assertThat(item.symptoms()).containsExactly(Symptom.SWELLING, Symptom.RASH_ITCHING);
	}
}
