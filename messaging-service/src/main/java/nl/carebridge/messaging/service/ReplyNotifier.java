package nl.carebridge.messaging.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import nl.carebridge.messaging.client.IntakeClient.Question;

/**
 * Tells the patient by email that the GP replied. The email never contains the reply or other health details
 * (GDPR checklist): only a link, so the patient reads the reply after signing in.
 */
@Service
public class ReplyNotifier {

	private static final Logger log = LoggerFactory.getLogger(ReplyNotifier.class);

	private final JavaMailSender sender;
	private final String from;
	private final String siteUrl;
	private final AppointmentLink appointmentLink;

	public ReplyNotifier(JavaMailSender sender, @Value("${carebridge.mail.from}") String from,
			@Value("${carebridge.site-url}") String siteUrl, AppointmentLink appointmentLink) {
		this.sender = sender;
		this.from = from;
		this.siteUrl = siteUrl;
		this.appointmentLink = appointmentLink;
	}

	/**
	 * Returns false when the email could not be sent; the reply is stored either way. With {@code appointment}, the
	 * email also links to the practice's booking page (User story nr 9), still without health details.
	 */
	public boolean replyArrived(Question question, boolean appointment) {
		SimpleMailMessage mail = new SimpleMailMessage();
		mail.setFrom(from);
		mail.setTo(question.patientEmail());
		mail.setSubject("Your GP practice has replied");
		String invitation = appointment ? """

				Your GP would like you to make an appointment.
				Make an appointment: %s
				""".formatted(appointmentLink.url()) : "";
		mail.setText("""
				Your GP practice has replied to your question in CareBridge.

				Sign in to read it: %s/questions/%s
				%s
				For your privacy, this email does not contain the reply.
				""".formatted(siteUrl, question.id(), invitation));
		try {
			sender.send(mail);
			return true;
		}
		catch (MailException ex) {
			// Never log the address: only that sending failed.
			log.warn("Could not send a reply notification: {}", ex.getClass().getSimpleName());
			return false;
		}
	}
}
