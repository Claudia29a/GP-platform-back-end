package nl.carebridge.intake.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import nl.carebridge.intake.model.ComplaintArea;
import nl.carebridge.intake.model.ComplaintDuration;
import nl.carebridge.intake.model.Intake;
import nl.carebridge.intake.model.Symptom;

/**
 * Runs against a real PostgreSQL in Docker, with the Flyway migrations, so the schema and the
 * JSON symptom column are tested exactly as they run in production.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class IntakeRepositoryTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private IntakeRepository repository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void storesAndReadsBackAllAnswers() {
		Intake saved = repository.save(intake("google-anna", Set.of(Symptom.FEVER, Symptom.COUGH), Instant.now()));
		entityManager.flush();
		entityManager.clear();

		Intake loaded = repository.findById(saved.getId()).orElseThrow();
		assertThat(loaded.getSymptoms()).containsExactlyInAnyOrder(Symptom.FEVER, Symptom.COUGH);
		assertThat(loaded.getComplaintArea()).isEqualTo(ComplaintArea.CHEST_BREATHING);
		assertThat(loaded.getDuration()).isEqualTo(ComplaintDuration.FOUR_TO_SEVEN_DAYS);
		assertThat(loaded.getSeverity()).isEqualTo(6);
		assertThat(loaded.isTakesMedication()).isTrue();
		assertThat(loaded.getCurrentMedication()).isEqualTo("Inhaler");
	}

	@Test
	void storesAnEmptySymptomList() {
		Intake saved = repository.save(intake("google-anna", EnumSet.noneOf(Symptom.class), Instant.now()));
		entityManager.flush();
		entityManager.clear();

		assertThat(repository.findById(saved.getId()).orElseThrow().getSymptoms()).isEmpty();
	}

	@Test
	void findsOnlyThePatientsOwnQuestionsOldestFirst() {
		// PostgreSQL stores time to the microsecond.
		Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
		repository.save(intake("google-anna", Set.of(), now.minusSeconds(60)));
		repository.save(intake("google-bob", Set.of(), now.minusSeconds(30)));
		repository.save(intake("google-anna", Set.of(), now.minusSeconds(120)));

		assertThat(repository.findAllByPatientIdOrderBySubmittedAtAsc("google-anna"))
				.extracting(Intake::getSubmittedAt)
				.containsExactly(now.minusSeconds(120), now.minusSeconds(60));
	}

	private static Intake intake(String patientId, Set<Symptom> symptoms, Instant submittedAt) {
		Intake intake = new Intake();
		intake.setPatientId(patientId);
		intake.setPatientName("Patient " + patientId);
		intake.setPatientEmail(patientId + "@example.com");
		intake.setComplaintArea(ComplaintArea.CHEST_BREATHING);
		intake.setDuration(ComplaintDuration.FOUR_TO_SEVEN_DAYS);
		intake.setSeverity(6);
		intake.setSymptoms(symptoms);
		intake.setTakesMedication(true);
		intake.setCurrentMedication("Inhaler");
		intake.setTriedTreatment(false);
		intake.setDescription("Coughing at night");
		intake.setStatus(Intake.Status.SUBMITTED);
		intake.setSubmittedAt(submittedAt.truncatedTo(ChronoUnit.MICROS));
		return intake;
	}
}
