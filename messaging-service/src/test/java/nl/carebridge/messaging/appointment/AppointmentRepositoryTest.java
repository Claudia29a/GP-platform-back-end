package nl.carebridge.messaging.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import nl.carebridge.messaging.encryption.EncryptionTestConfig;
import nl.carebridge.messaging.model.Reply;
import nl.carebridge.messaging.repository.ReplyRepository;

/** User story nr 10 TAC3 TAC4 AC8, against a real PostgreSQL with the Flyway migrations. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(EncryptionTestConfig.class)
@Tag("integration")
class AppointmentRepositoryTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private AppointmentRepository appointments;

	@Autowired
	private ReplyRepository replies;

	@Autowired
	private TestEntityManager entityManager;

	private final Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

	private Reply invitation(UUID question) {
		return replies.saveAndFlush(new Reply(question, "gp", "Dr. Jansen", "Please come in.", true, now, now));
	}

	@Test
	void storesThePatientsNameEncryptedAndFindsADay() {
		UUID question = UUID.randomUUID();
		Instant start = now.plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
		appointments.saveAndFlush(new Appointment(invitation(question).getId(), question, "patient-hash", "Anna de Vries", start, now));
		entityManager.clear();

		assertThat(appointments.findAllByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(start.minusSeconds(1), start.plusSeconds(1)))
				.extracting(Appointment::getPatientName).containsExactly("Anna de Vries");
		assertThat(appointments.bookedFrom(now)).contains(start);
		Object stored = entityManager.getEntityManager().createNativeQuery("SELECT patient_name FROM appointment WHERE intake_id = :id")
				.setParameter("id", question).getSingleResult();
		assertThat(stored.toString()).doesNotContain("Anna");
	}

	@Test
	void theDatabaseNeverAllowsTheSameTimeTwice() {
		Instant start = now.plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
		UUID first = UUID.randomUUID();
		UUID second = UUID.randomUUID();
		appointments.saveAndFlush(new Appointment(invitation(first).getId(), first, "anna", "Anna", start, now));

		assertThatThrownBy(() -> appointments.saveAndFlush(new Appointment(invitation(second).getId(), second, "bram", "Bram", start, now)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void theDatabaseAllowsOneAppointmentPerInvitation() {
		UUID question = UUID.randomUUID();
		UUID reply = invitation(question).getId();
		appointments.saveAndFlush(new Appointment(reply, question, "anna", "Anna", now.plus(4, ChronoUnit.DAYS), now));

		assertThatThrownBy(() -> appointments.saveAndFlush(new Appointment(reply, question, "anna", "Anna", now.plus(5, ChronoUnit.DAYS), now)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void deletingTheRepliesDeletesTheirAppointments() {
		UUID question = UUID.randomUUID();
		appointments.saveAndFlush(new Appointment(invitation(question).getId(), question, "anna", "Anna", now.plus(6, ChronoUnit.DAYS), now));

		replies.deleteAllByIntakeIdIn(List.of(question));
		entityManager.flush();
		entityManager.clear();

		assertThat(appointments.findAll()).noneMatch(appointment -> appointment.getIntakeId().equals(question));
	}
}
