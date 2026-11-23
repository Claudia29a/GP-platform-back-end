package nl.carebridge.messaging.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import nl.carebridge.messaging.client.IntakeClient;
import nl.carebridge.messaging.client.IntakeClient.Question;
import nl.carebridge.messaging.model.Reply;
import nl.carebridge.messaging.repository.ReplyRepository;
import nl.carebridge.messaging.security.CurrentUser;
import nl.carebridge.messaging.security.Role;
import nl.carebridge.messaging.service.ReplyNotifier;

/** User story nr 10 AC3 TAC3: ten patients pick the same time at the same moment; exactly one gets it. */
@SpringBootTest
@Testcontainers
@Tag("integration")
class AppointmentConcurrencyIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private AppointmentService service;

	@Autowired
	private ReplyRepository replies;

	@Autowired
	private SlotCalendar calendar;

	@MockitoBean
	private IntakeClient intakeClient;

	@MockitoBean
	private ReplyNotifier notifier;

	@Test
	void onlyOnePatientGetsATimeThatEveryonePicksTogether() throws Exception {
		Instant time = calendar.offered().getFirst();
		List<Callable<Object>> work = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			UUID question = UUID.randomUUID();
			Reply invitation = replies.save(new Reply(question, "gp", "Dr. Jansen", "Please come in.", true, Instant.now(), Instant.now()));
			String token = "Bearer patient-" + i;
			when(intakeClient.find(question, token)).thenReturn(Optional.of(new Question(question, "Patient " + i, i + "@example.com", Instant.now())));
			CurrentUser patient = new CurrentUser("account-" + i, "Patient " + i, i + "@example.com", Role.PATIENT);
			work.add(() -> service.book(invitation.getId(), time, patient, token));
		}

		int booked = 0;
		int conflicts = 0;
		try (ExecutorService pool = Executors.newFixedThreadPool(work.size())) {
			for (Future<Object> result : pool.invokeAll(work)) {
				try {
					result.get();
					booked++;
				}
				catch (ExecutionException ex) {
					assertThat(ex.getCause()).isInstanceOfSatisfying(ResponseStatusException.class,
							conflict -> assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
					conflicts++;
				}
			}
		}

		assertThat(booked).isEqualTo(1);
		assertThat(conflicts).isEqualTo(9);
		assertThat(service.free()).doesNotContain(time);
		org.mockito.Mockito.verify(notifier, org.mockito.Mockito.times(1)).appointmentBooked(any(), any(), any());
	}
}
