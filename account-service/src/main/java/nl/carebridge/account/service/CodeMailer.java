package nl.carebridge.account.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Emails the verification code. The email holds the code only, never health data. */
@Service
public class CodeMailer {

	private static final Logger log = LoggerFactory.getLogger(CodeMailer.class);

	private final JavaMailSender sender;
	private final String from;

	public CodeMailer(JavaMailSender sender, @Value("${carebridge.mail.from}") String from) {
		this.sender = sender;
		this.from = from;
	}

	/** Returns false when the email could not be sent; the user can then ask for a new code. */
	public boolean send(String to, String code, Duration lifetime) {
		SimpleMailMessage mail = new SimpleMailMessage();
		mail.setFrom(from);
		mail.setTo(to);
		mail.setSubject("Your CareBridge code");
		mail.setText("""
				Your CareBridge code is %s

				Type this code in CareBridge to confirm your email address. It works for %d minutes.

				If you did not create a CareBridge account, you can ignore this email.
				""".formatted(code, lifetime.toMinutes()));
		try {
			sender.send(mail);
			return true;
		}
		catch (MailException ex) {
			// Never log the code or the address: only that sending failed.
			log.warn("Could not send a verification email: {}", ex.getClass().getSimpleName());
			return false;
		}
	}
}
