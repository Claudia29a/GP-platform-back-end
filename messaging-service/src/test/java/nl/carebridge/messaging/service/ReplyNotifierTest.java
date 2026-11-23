package nl.carebridge.messaging.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import nl.carebridge.messaging.client.IntakeClient.Question;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ReplyNotifierTest {

	private static final Question QUESTION = new Question(UUID.fromString("3f1c2a4e-0000-4000-8000-000000000001"), "Anna de Vries", "anna@example.com", java.time.Instant.parse("2026-11-01T09:00:00Z"));

	@Mock
	private JavaMailSender sender;

	private ReplyNotifier notifier() {
		return new ReplyNotifier(sender, "CareBridge <no-reply@carebridge.test>", "https://carebridge.test",
				new AppointmentLink("", "https://carebridge.test"));
	}

	@Test
	void tellsThePatientThereIsAReplyWithALinkAndNoHealthDetails() {
		assertThat(notifier().replyArrived(QUESTION, reply(false))).isTrue();

		ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(sender).send(mail.capture());
		assertThat(mail.getValue().getTo()).containsExactly("anna@example.com");
		assertThat(mail.getValue().getSubject()).isEqualTo("Your GP practice has replied");
		assertThat(mail.getValue().getText())
				.contains("Sign in to read it", "https://carebridge.test/questions/3f1c2a4e-0000-4000-8000-000000000001",
						"does not contain the reply")
				.doesNotContain("Rash", "cream", "appointment");
	}

	/** User story nr 9 AC2: with an invitation, the email also links to the booking page, still without health details. */
	@Test
	void anInvitationAddsTheBookingLinkAndStillNoHealthDetails() {
		assertThat(notifier().replyArrived(QUESTION, reply(true))).isTrue();

		ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(sender).send(mail.capture());
		assertThat(mail.getValue().getText())
				.contains("would like you to make an appointment", "Make an appointment: https://carebridge.test/appointment?reply=",
						"Sign in to read it", "does not contain the reply")
				.doesNotContain("Rash", "cream");
	}

	@Test
	void aFailedEmailIsReportedAndDoesNotLoseTheReply() {
		doThrow(new MailSendException("server down")).when(sender).send(any(SimpleMailMessage.class));

		assertThat(notifier().replyArrived(QUESTION, reply(true))).isFalse();
		assertThat(notifier().appointmentBooked(QUESTION, "Tuesday 1 December 2026", "10:00")).isFalse();
		assertThat(notifier().appointmentCancelled(QUESTION, "Tuesday 1 December 2026", "10:00")).isFalse();
	}

	/** User story nr 10 AC5: a confirmation with the date and time and a link, without health details. */
	@Test
	void confirmsTheAppointmentWithItsDateAndTime() {
		assertThat(notifier().appointmentBooked(QUESTION, "Tuesday 1 December 2026", "10:00")).isTrue();

		ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(sender).send(mail.capture());
		assertThat(mail.getValue().getTo()).containsExactly("anna@example.com");
		assertThat(mail.getValue().getSubject()).isEqualTo("Your appointment at your GP practice");
		assertThat(mail.getValue().getText())
				.contains("Tuesday 1 December 2026 at 10:00", "https://carebridge.test/questions/3f1c2a4e-0000-4000-8000-000000000001")
				.doesNotContain("Rash", "cream");
	}

	/** User story nr 10 AC5: the patient also hears when the appointment is cancelled. */
	@Test
	void confirmsACancellation() {
		assertThat(notifier().appointmentCancelled(QUESTION, "Tuesday 1 December 2026", "10:00")).isTrue();

		ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(sender).send(mail.capture());
		assertThat(mail.getValue().getSubject()).isEqualTo("Your appointment has been cancelled");
		assertThat(mail.getValue().getText()).contains("Tuesday 1 December 2026 at 10:00", "cancelled");
	}

	private static nl.carebridge.messaging.model.Reply reply(boolean appointment) {
		return new nl.carebridge.messaging.model.Reply(QUESTION.id(), "gp", "Dr. Jansen", "Use the cream.", appointment,
				java.time.Instant.parse("2026-11-17T10:00:00Z"), QUESTION.submittedAt());
	}
}
