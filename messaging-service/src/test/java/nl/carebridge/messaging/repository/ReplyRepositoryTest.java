package nl.carebridge.messaging.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import nl.carebridge.messaging.encryption.EncryptionTestConfig;
import nl.carebridge.messaging.model.Reply;

/** Against a real PostgreSQL with the Flyway migrations. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(EncryptionTestConfig.class)
@Tag("integration")
class ReplyRepositoryTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private ReplyRepository repository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void storesTheReplyAndTheGpsNameEncryptedAndReadsThemBackInOrder() {
		UUID question = UUID.randomUUID();
		Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
		repository.save(new Reply(question, "gp-hash", "Dr. Jansen", "Second", now, now.minus(java.time.Duration.ofDays(1))));
		repository.save(new Reply(question, "gp-hash", "Dr. Jansen", "First", now.minusSeconds(60), now.minus(java.time.Duration.ofDays(1))));
		repository.save(new Reply(UUID.randomUUID(), "gp-hash", "Dr. Jansen", "Other question", now, now.minus(java.time.Duration.ofDays(1))));
		entityManager.flush();
		entityManager.clear();

		assertThat(repository.findAllByIntakeIdOrderByCreatedAtAsc(question)).extracting(Reply::getText).containsExactly("First", "Second");
		Object[] row = (Object[]) entityManager.getEntityManager()
				.createNativeQuery("SELECT text, author_name FROM reply WHERE intake_id = :id ORDER BY created_at LIMIT 1")
				.setParameter("id", question).getSingleResult();
		assertThat(row[0].toString()).doesNotContain("First");
		assertThat(row[1].toString()).doesNotContain("Jansen");
	}

	@Test
	void deletesRepliesWhoseQuestionIsOlderThanTheLimitAndAPatientsReplies() {
		Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
		UUID oldQuestion = UUID.randomUUID();
		UUID newQuestion = UUID.randomUUID();
		UUID annasQuestion = UUID.randomUUID();
		repository.save(new Reply(oldQuestion, "gp", "Dr. Jansen", "Old", now, now.minus(java.time.Duration.ofDays(200))));
		repository.save(new Reply(newQuestion, "gp", "Dr. Jansen", "New", now, now.minus(java.time.Duration.ofDays(10))));
		repository.save(new Reply(annasQuestion, "gp", "Dr. Jansen", "Anna's", now, now));
		entityManager.flush();

		assertThat(repository.deleteQuestionSubmittedBefore(now.minus(java.time.Duration.ofDays(183)))).isEqualTo(1);
		repository.deleteAllByIntakeIdIn(java.util.List.of(annasQuestion));
		entityManager.flush();
		entityManager.clear();

		assertThat(repository.findAll()).extracting(Reply::getText).containsExactly("New");
	}
}
