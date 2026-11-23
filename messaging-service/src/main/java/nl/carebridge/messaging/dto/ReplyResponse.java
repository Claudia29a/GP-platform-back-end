package nl.carebridge.messaging.dto;

import java.time.Instant;
import java.util.UUID;

import nl.carebridge.messaging.model.Reply;
import nl.carebridge.messaging.service.AppointmentLink;

/**
 * A reply as patients and staff read it: never the hash of the GP's account. When the GP asked for an appointment,
 * {@code appointmentUrl} is the practice's booking page from the settings (User story nr 9); otherwise it is left out.
 */
public record ReplyResponse(UUID id, UUID questionId, String authorName, String text, Instant createdAt, boolean appointment,
		String appointmentUrl) {

	public static ReplyResponse from(Reply reply, AppointmentLink link) {
		return new ReplyResponse(reply.getId(), reply.getIntakeId(), reply.getAuthorName(), reply.getText(), reply.getCreatedAt(),
				reply.isAppointment(), reply.isAppointment() ? link.url() : null);
	}
}
