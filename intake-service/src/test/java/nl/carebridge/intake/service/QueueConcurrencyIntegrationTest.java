package nl.carebridge.intake.service;

import static nl.carebridge.intake.TestIntakes.validRequest;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import nl.carebridge.intake.model.Intake;
import nl.carebridge.intake.repository.IntakeRepository;
import nl.carebridge.intake.security.CurrentUser;
import nl.carebridge.intake.security.Role;

/**
 * Story 4 TAC6: when several people change the queue at the same moment, against a real PostgreSQL,
 * no position may be lost or used twice.
 */
@SpringBootTest
@Testcontainers
@Tag("integration")
class QueueConcurrencyIntegrationTest {

	private static final CurrentUser PATIENT = new CurrentUser("google-anna", "Anna", "anna@example.com", Role.PATIENT);

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private IntakeService intakeService;

	@Autowired
	private QueueService queueService;

	@Autowired
	private IntakeRepository repository;

	@BeforeEach
	void emptyQueue() {
		repository.deleteAll();
	}

	@Test
	void questionsArrivingAtTheSameMomentEachGetTheirOwnPosition() throws Exception {
		runAtTheSameTime(IntStream.range(0, 12).<Callable<Object>>mapToObj(i -> () -> intakeService.submit(validRequest(), PATIENT)).toList());

		assertThat(positions()).containsExactlyElementsOf(IntStream.rangeClosed(1, 12).boxed().toList());
	}

	@Test
	void reorderingWhileQuestionsArriveLosesNoPosition() throws Exception {
		for (int i = 0; i < 4; i++) {
			intakeService.submit(validRequest(), PATIENT);
		}
		List<UUID> reversed = queueService.currentQueue().stream().map(Intake::getId).toList().reversed();

		List<Callable<Object>> work = new ArrayList<>();
		work.add(() -> queueService.reorder(reversed));
		work.add(() -> queueService.reorder(reversed));
		for (int i = 0; i < 6; i++) {
			work.add(() -> intakeService.submit(validRequest(), PATIENT));
		}
		runAtTheSameTime(work);

		assertThat(positions()).containsExactlyElementsOf(IntStream.rangeClosed(1, 10).boxed().toList());
	}

	private List<Integer> positions() {
		return queueService.currentQueue().stream().map(Intake::getQueuePosition).toList();
	}

	/** A reorder that lost the race is refused with 409, which is correct, so only the outcome counts. */
	private static void runAtTheSameTime(List<Callable<Object>> work) throws InterruptedException {
		try (ExecutorService pool = Executors.newFixedThreadPool(work.size())) {
			for (Future<Object> done : pool.invokeAll(work)) {
				try {
					done.get();
				}
				catch (ExecutionException expectedForALateReorder) {
					// The positions are checked afterwards.
				}
			}
		}
	}
}
