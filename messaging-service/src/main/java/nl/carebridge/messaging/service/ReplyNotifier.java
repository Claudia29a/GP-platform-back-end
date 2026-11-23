package nl.carebridge.messaging.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import nl.carebridge.messaging.client.IntakeClient.Question;
import nl.carebridge.messaging.model.Reply;

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
	public boolean replyArrived(Question question, Reply reply) {
		SimpleMailMessage mail = new SimpleMailMessage();
		mail.setFrom(from);
		mail.setTo(question.patientEmail());
		mail.setSubject("Your GP practice has replied");
		String invitation = reply.isAppointment() ? """

				Your GP would like you to make an appointment.
				Make an appointment: %s
				""".formatted(appointmentLink.urlFor(reply.getId())) : "";
		mail.setText("""
				Your GP practice has replied to your question in CareBridge.

				Sign in to read it: %s/questions/%s
				%s
				For your privacy, this email does not contain the reply.
				""".formatted(siteUrl, question.id(), invitation));
		return send(mail);
	}

	/** User story nr 10 AC5: the date, time and a link; never the question or the reply. */
	public boolean appointmentBooked(Question question, String day, String time) {
		return send(mail(question, "Your appointment at your GP practice", """
				Your appointment at your GP practice: %s at %s.

				See or cancel it in CareBridge: %s/questions/%s

				For your privacy, this email does not contain your question.
				""".formatted(day, time, siteUrl, question.id())));
	}

	/** User story nr 10 AC5. */
	public boolean appointmentCancelled(Question question, String day, String time) {
		return send(mail(question, "Your appointment has been cancelled", """
				Your appointment at your GP practice on %s at %s has been cancelled.

				You can choose a new time in CareBridge: %s/questions/%s
				""".formatted(day, time, siteUrl, question.id())));
	}

	private SimpleMailMessage mail(Question question, String subject, String text) {
		SimpleMailMessage mail = new SimpleMailMessage();
		mail.setFrom(from);
		mail.setTo(question.patientEmail());
		mail.setSubject(subject);
		mail.setText(text);
		return mail;
	}

	/** Returns false when sending failed; what the patient did is stored either way. */
	private boolean send(SimpleMailMessage mail) {
		try {
			sender.send(mail);
			return true;
		}
		catch (MailException ex) {
			// Never log the address: only that sending failed.
			log.warn("Could not send an email to a patient: {}", ex.getClass().getSimpleName());
			return false;
		}
	}
}
