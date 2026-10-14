package nl.carebridge.intake.dto;

import java.util.Set;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import nl.carebridge.intake.model.ComplaintArea;
import nl.carebridge.intake.model.ComplaintDuration;
import nl.carebridge.intake.model.Symptom;

/**
 * The patient's answers to the question form (SAD v2, section 2.3).
 * Who the patient is comes from the sign-in token, never from this request.
 */
public record SubmitIntakeRequest(
		@NotNull ComplaintArea complaintArea,
		@Size(max = 100) String complaintAreaOther,
		@NotNull ComplaintDuration duration,
		@NotNull @Min(1) @Max(10) Integer severity,
		Set<@NotNull Symptom> symptoms,
		@Size(max = 200) String symptomsOther,
		@NotNull Boolean takesMedication,
		@Size(max = 500) String currentMedication,
		@NotNull Boolean triedTreatment,
		@Size(max = 500) String treatmentTried,
		@NotBlank @Size(max = 1000) String description) {

	public SubmitIntakeRequest {
		symptoms = symptoms == null ? Set.of() : symptoms;
	}

	@AssertTrue(message = "describe where the problem is")
	boolean isComplaintAreaOtherGiven() {
		return complaintArea != ComplaintArea.OTHER || hasText(complaintAreaOther);
	}

	@AssertTrue(message = "say which medication or treatment you take")
	boolean isCurrentMedicationGiven() {
		return !Boolean.TRUE.equals(takesMedication) || hasText(currentMedication);
	}

	@AssertTrue(message = "say what you already tried")
	boolean isTreatmentTriedGiven() {
		return !Boolean.TRUE.equals(triedTreatment) || hasText(treatmentTried);
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
