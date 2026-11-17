package nl.carebridge.messaging.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import nl.carebridge.messaging.TestMessages;
import nl.carebridge.messaging.client.IntakeClient;
import nl.carebridge.messaging.client.IntakeClient.Question;
import nl.carebridge.messaging.encryption.DataProtection;
import nl.carebridge.messaging.model.Reply;
import nl.carebridge.messaging.repository.ReplyRepository;
import nl.carebridge.messaging.security.CurrentUser;
import nl.carebridge.messaging.security.Role;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ReplyServiceTest {

	private static final UUID QUESTION = UUID.fromString("3f1c2a4e-0000-4000-8000-000000000001");
	private static final CurrentUser GP = new CurrentUser("google-gp", "Dr. Jansen", "gp@example.com", Role.GP);
	private static final CurrentUser PATIENT = new CurrentUser("google-anna", "Anna", "anna@example.com", Role.PATIENT);
	private static final Question ANNAS_QUESTION = new Question(QUESTION, "Anna de Vries", "anna@example.com");

	@Mock
	private ReplyRepository repository;

	@Mock
	private IntakeClient intakeClient;

	@Mock
	private ReplyNotifier notifier;

	private final DataProtection protection = new DataProtection(TestMessages.DATA_KEY);
	private ReplyService service;

	@BeforeEach
	void createService() {
		service = new ReplyService(repository, intakeClient, notifier, protection);
		org.mockito.Mockito.lenient().when(repository.save(any(Reply.class))).thenAnswer(call -> call.getArgument(0));
	}

	@Test
	void theGpRepliesToAQuestionTheyMaySee() {
		when(intakeClient.find(QUESTION, "Bearer gp.token")).thenReturn(Optional.of(ANNAS_QUESTION));
		Instant before = Instant.now();

		Reply reply = service.reply(QUESTION, "  Please use the cream twice a day. ", GP, "Bearer gp.token");

		assertThat(reply.getIntakeId()).isEqualTo(QUESTION);
		assertThat(reply.getText()).isEqualTo("Please use the cream twice a day.");
		assertThat(reply.getAuthorName()).isEqualTo("Dr. Jansen");
		assertThat(reply.getAuthorHash()).isEqualTo(protection.hash("google-gp"));
		assertThat(reply.getCreatedAt()).isAfterOrEqualTo(before);
		verify(notifier).replyArrived(ANNAS_QUESTION);
	}

	@Test
	void aQuestionTheIntakeServiceDoesNotShowIsNotFoundAndNothingIsStored() {
		when(intakeClient.find(QUESTION, "Bearer gp.token")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.reply(QUESTION, "Hello", GP, "Bearer gp.token"))
				.isInstanceOfSatisfying(ResponseStatusException.class, ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
		verify(repository, never()).save(any());
		verify(notifier, never()).replyArrived(any());
	}

	@Test
	void thePatientReadsTheRepliesToTheirOwnQuestionOldestFirst() {
		Reply first = new Reply(QUESTION, "hash", "Dr. Jansen", "First", Instant.parse("2026-11-17T10:00:00Z"));
		when(intakeClient.find(QUESTION, "Bearer patient.token")).thenReturn(Optional.of(ANNAS_QUESTION));
		when(repository.findAllByIntakeIdOrderByCreatedAtAsc(QUESTION)).thenReturn(List.of(first));

		assertThat(service.replies(QUESTION, PATIENT, "Bearer patient.token")).containsExactly(first);
	}

	@Test
	void someoneElsesQuestionShowsNoReplies() {
		when(intakeClient.find(QUESTION, "Bearer patient.token")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.replies(QUESTION, PATIENT, "Bearer patient.token")).isInstanceOf(ResponseStatusException.class);
		verify(repository, never()).findAllByIntakeIdOrderByCreatedAtAsc(any());
	}
}
