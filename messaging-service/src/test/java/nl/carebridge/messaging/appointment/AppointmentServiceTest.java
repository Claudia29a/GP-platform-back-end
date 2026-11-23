package nl.carebridge.messaging.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
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
import nl.carebridge.messaging.service.ReplyNotifier;

/** User story nr 10 AC3 AC4 AC5 AC7 TAC2 TAC6: the booking rules. */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AppointmentServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-30T14:00:00Z");
	private static final Instant TUESDAY_TEN = Instant.parse("2026-12-01T09:00:00Z");
	private static final UUID QUESTION = UUID.fromString("3f1c2a4e-0000-4000-8000-000000000001");
	private static final CurrentUser ANNA = new CurrentUser("account-anna", "Anna de Vries", "anna@example.com", Role.PATIENT);
	private static final Question ANNAS_QUESTION = new Question(QUESTION, "Anna de Vries", "anna@example.com", Instant.parse("2026-11-01T09:00:00Z"));

	@Mock
	private AppointmentRepository appointments;

	@Mock
	private ReplyRepository replies;

	@Mock
	private IntakeClient intakeClient;

	@Mock
	private ReplyNotifier notifier;

	private final DataProtection protection = new DataProtection(TestMessages.DATA_KEY);
	private final Clock clock = Clock.fixed(NOW, ZoneId.of("Europe/Amsterdam"));
	private AppointmentService service;
	private Reply invitation;

	@BeforeEach
	void createService() {
		service = new AppointmentService(appointments, replies, intakeClient, notifier, protection,
				new SlotCalendar(SlotCalendarTest.settings(), clock), clock);
		invitation = reply(true);
	}

	private static Reply reply(boolean appointment) {
		return new Reply(QUESTION, "gp-hash", "Dr. Jansen", "Please come in.", appointment, NOW.minusSeconds(3600), ANNAS_QUESTION.submittedAt());
	}

	private void invitationOfAnna(Reply reply) {
		UUID id = UUID.randomUUID();
		when(replies.findById(id)).thenReturn(Optional.of(reply));
		org.mockito.Mockito.lenient().when(intakeClient.find(QUESTION, "Bearer anna")).thenReturn(Optional.of(ANNAS_QUESTION));
		lastReplyId = id;
	}

	private UUID lastReplyId;

	@Test
	void freeTimesLeaveBookedTimesOut() {
		when(appointments.bookedFrom(any())).thenReturn(List.of(TUESDAY_TEN));

		assertThat(service.free()).doesNotContain(TUESDAY_TEN).contains(Instant.parse("2026-12-01T09:15:00Z"));
	}

	@Test
	void thePatientBooksAFreeTimeForTheirInvitationAndGetsAnEmail() {
		invitationOfAnna(invitation);
		when(appointments.saveAndFlush(any(Appointment.class))).thenAnswer(call -> call.getArgument(0));

		Appointment booked = service.book(lastReplyId, TUESDAY_TEN, ANNA, "Bearer anna");

		assertThat(booked.getStartsAt()).isEqualTo(TUESDAY_TEN);
		assertThat(booked.getIntakeId()).isEqualTo(QUESTION);
		assertThat(booked.getReplyId()).isEqualTo(lastReplyId);
		assertThat(booked.getPatientName()).isEqualTo("Anna de Vries");
		assertThat(booked.getPatientHash()).isEqualTo(protection.hash("account-anna"));
		verify(notifier).appointmentBooked(ANNAS_QUESTION, "Tuesday 1 December 2026", "10:00");
	}

	@Test
	void aReplyWithoutAnInvitationCannotBeBooked() {
		invitationOfAnna(reply(false));

		assertStatus(() -> service.book(lastReplyId, TUESDAY_TEN, ANNA, "Bearer anna"), HttpStatus.NOT_FOUND);
		verify(appointments, never()).saveAndFlush(any());
	}

	@Test
	void someoneElsesInvitationIsNotFound() {
		UUID id = UUID.randomUUID();
		when(replies.findById(id)).thenReturn(Optional.of(invitation));
		when(intakeClient.find(QUESTION, "Bearer bram")).thenReturn(Optional.empty());
		CurrentUser bram = new CurrentUser("account-bram", "Bram", "bram@example.com", Role.PATIENT);

		assertStatus(() -> service.book(id, TUESDAY_TEN, bram, "Bearer bram"), HttpStatus.NOT_FOUND);
		verify(appointments, never()).saveAndFlush(any());
	}

	@Test
	void oneAppointmentPerInvitation() {
		invitationOfAnna(invitation);
		when(appointments.findByReplyId(lastReplyId)).thenReturn(Optional.of(new Appointment(lastReplyId, QUESTION, "hash", "Anna", TUESDAY_TEN, NOW)));

		assertStatus(() -> service.book(lastReplyId, Instant.parse("2026-12-01T09:15:00Z"), ANNA, "Bearer anna"), HttpStatus.CONFLICT);
		verify(appointments, never()).saveAndFlush(any());
	}

	@Test
	void aTimeThatIsNotOfferedIsRefused() {
		invitationOfAnna(invitation);

		for (Instant wrong : List.of(Instant.parse("2026-11-30T15:00:00Z"), Instant.parse("2026-12-05T09:00:00Z"), Instant.parse("2026-12-01T09:05:00Z"))) {
			assertStatus(() -> service.book(lastReplyId, wrong, ANNA, "Bearer anna"), HttpStatus.BAD_REQUEST);
		}
		verify(appointments, never()).saveAndFlush(any());
	}

	@Test
	void aTimeTakenAtTheSameMomentGivesAClearConflict() {
		invitationOfAnna(invitation);
		when(appointments.saveAndFlush(any(Appointment.class))).thenThrow(new DataIntegrityViolationException("uq_appointment_starts_at"));

		assertThatThrownBy(() -> service.book(lastReplyId, TUESDAY_TEN, ANNA, "Bearer anna"))
				.isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
					assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(ex.getReason()).isEqualTo("This time was just taken. Please choose another.");
				});
		verify(notifier, never()).appointmentBooked(any(), any(), any());
	}

	@Test
	void thePatientCancelsBeforeItStartsAndGetsAnEmail() {
		Appointment appointment = new Appointment(UUID.randomUUID(), QUESTION, protection.hash("account-anna"), "Anna", TUESDAY_TEN, NOW);
		UUID id = UUID.randomUUID();
		when(appointments.findById(id)).thenReturn(Optional.of(appointment));
		when(intakeClient.find(QUESTION, "Bearer anna")).thenReturn(Optional.of(ANNAS_QUESTION));

		service.cancel(id, ANNA, "Bearer anna");

		verify(appointments).delete(appointment);
		verify(notifier).appointmentCancelled(ANNAS_QUESTION, "Tuesday 1 December 2026", "10:00");
	}

	@Test
	void onlyThePatientWhoBookedCanCancel() {
		UUID id = UUID.randomUUID();
		when(appointments.findById(id)).thenReturn(Optional.of(new Appointment(UUID.randomUUID(), QUESTION, protection.hash("account-bram"), "Bram", TUESDAY_TEN, NOW)));

		assertStatus(() -> service.cancel(id, ANNA, "Bearer anna"), HttpStatus.NOT_FOUND);
		verify(appointments, never()).delete(any());
	}

	@Test
	void anAppointmentThatStartedCannotBeCancelled() {
		UUID id = UUID.randomUUID();
		when(appointments.findById(id)).thenReturn(Optional.of(new Appointment(UUID.randomUUID(), QUESTION, protection.hash("account-anna"), "Anna", NOW.minusSeconds(60), NOW)));

		assertStatus(() -> service.cancel(id, ANNA, "Bearer anna"), HttpStatus.BAD_REQUEST);
		verify(appointments, never()).delete(any());
	}

	@Test
	void theStaffListShowsOneDayInThePracticesTimeZone() {
		service.onDay(java.time.LocalDate.of(2026, 12, 1));

		verify(appointments).findAllByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
				Instant.parse("2026-11-30T23:00:00Z"), Instant.parse("2026-12-01T23:00:00Z"));
	}

	private static void assertStatus(Runnable call, HttpStatus status) {
		assertThatThrownBy(call::run).isInstanceOfSatisfying(ResponseStatusException.class, ex -> assertThat(ex.getStatusCode()).isEqualTo(status));
	}
}
