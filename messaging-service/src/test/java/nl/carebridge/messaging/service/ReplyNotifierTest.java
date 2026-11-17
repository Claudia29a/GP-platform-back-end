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

	private static final Question QUESTION = new Question(UUID.fromString("3f1c2a4e-0000-4000-8000-000000000001"), "Anna de Vries", "anna@example.com");

	@Mock
	private JavaMailSender sender;

	private ReplyNotifier notifier() {
		return new ReplyNotifier(sender, "CareBridge <no-reply@carebridge.test>", "https://carebridge.test");
	}

	@Test
	void tellsThePatientThereIsAReplyWithALinkAndNoHealthDetails() {
		assertThat(notifier().replyArrived(QUESTION)).isTrue();

		ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(sender).send(mail.capture());
		assertThat(mail.getValue().getTo()).containsExactly("anna@example.com");
		assertThat(mail.getValue().getSubject()).isEqualTo("Your GP practice has replied");
		assertThat(mail.getValue().getText())
				.contains("Sign in to read it", "https://carebridge.test/questions/3f1c2a4e-0000-4000-8000-000000000001",
						"does not contain the reply")
				.doesNotContain("Rash", "cream");
	}

	@Test
	void aFailedEmailIsReportedAndDoesNotLoseTheReply() {
		doThrow(new MailSendException("server down")).when(sender).send(any(SimpleMailMessage.class));

		assertThat(notifier().replyArrived(QUESTION)).isFalse();
	}
}
