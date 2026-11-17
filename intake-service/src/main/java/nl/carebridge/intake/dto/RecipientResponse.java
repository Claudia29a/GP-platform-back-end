package nl.carebridge.intake.dto;

import java.time.Instant;
import java.util.UUID;

import nl.carebridge.intake.model.Intake;

/**
 * Who gets the notification of a reply, and when the question was sent so its replies are deleted with it: never
 * the health details.
 */
public record RecipientResponse(UUID id, String patientName, String patientEmail, Instant submittedAt) {

	public static RecipientResponse from(Intake intake) {
		return new RecipientResponse(intake.getId(), intake.getPatientName(), intake.getPatientEmail(), intake.getSubmittedAt());
	}
}
