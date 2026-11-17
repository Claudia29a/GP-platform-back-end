package nl.carebridge.intake.dto;

import java.util.UUID;

import nl.carebridge.intake.model.Intake;

/** Who gets the notification of a reply: only the name and email, never the health details. */
public record RecipientResponse(UUID id, String patientName, String patientEmail) {

	public static RecipientResponse from(Intake intake) {
		return new RecipientResponse(intake.getId(), intake.getPatientName(), intake.getPatientEmail());
	}
}
