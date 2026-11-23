package nl.carebridge.messaging.dto;

import java.time.Instant;
import java.util.UUID;

import nl.carebridge.messaging.appointment.AppointmentResponse;

/**
 * A reply as patients and staff read it: never the hash of the GP's account. When the GP asked for an appointment,
 * {@code appointmentUrl} is where it is made (User story nr 9) and {@code booked} the appointment once it is made
 * (User story nr 10); otherwise they are left out.
 */
public record ReplyResponse(UUID id, UUID questionId, String authorName, String text, Instant createdAt, boolean appointment,
		String appointmentUrl, AppointmentResponse booked) {
}
