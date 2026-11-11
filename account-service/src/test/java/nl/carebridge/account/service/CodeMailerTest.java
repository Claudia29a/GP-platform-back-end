package nl.carebridge.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CodeMailerTest {

	@Mock
	private JavaMailSender sender;

	@Test
	void sendsTheCodeAndNothingElseAboutThePatient() {
		CodeMailer mailer = new CodeMailer(sender, "CareBridge <no-reply@carebridge.test>");

		assertThat(mailer.send("anna@example.com", "482913", Duration.ofMinutes(15))).isTrue();

		ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(sender).send(mail.capture());
		assertThat(mail.getValue().getTo()).containsExactly("anna@example.com");
		assertThat(mail.getValue().getFrom()).isEqualTo("CareBridge <no-reply@carebridge.test>");
		assertThat(mail.getValue().getSubject()).isEqualTo("Your CareBridge code");
		assertThat(mail.getValue().getText()).contains("482913", "15 minutes", "did not create");
	}

	@Test
	void aFailureIsReportedWithoutStoppingTheSignUp() {
		doThrow(new MailSendException("server down")).when(sender).send(any(SimpleMailMessage.class));

		assertThat(new CodeMailer(sender, "no-reply@carebridge.test").send("anna@example.com", "482913", Duration.ofMinutes(15))).isFalse();
	}
}
