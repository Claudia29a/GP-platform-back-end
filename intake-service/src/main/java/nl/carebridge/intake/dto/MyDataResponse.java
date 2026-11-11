package nl.carebridge.intake.dto;

import java.time.Instant;
import java.util.List;

/** Everything the Intake Service stores about a patient, for "My data" and its download (GDPR Art. 15, 20). */
public record MyDataResponse(UserData user, ConsentData privacyConsent, List<QuestionData> questions) {

	public record UserData(String id, String name, String email) {
	}

	public record ConsentData(String version, Instant acceptedAt) {
	}

	/** A question with everyone who opened it. */
	public record QuestionData(IntakeResponse question, List<AccessLogResponse> openedBy) {
	}
}
