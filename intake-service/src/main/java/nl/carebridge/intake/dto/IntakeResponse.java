package nl.carebridge.intake.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import nl.carebridge.intake.model.ComplaintArea;
import nl.carebridge.intake.model.ComplaintDuration;
import nl.carebridge.intake.model.Intake;
import nl.carebridge.intake.model.Symptom;

/**
 * A question as patients and staff read it. It has no severity score or queue position, so those
 * can never reach a patient (Story 4 TAC9); staff get them through {@link QueueItemResponse}.
 */
public record IntakeResponse(
		UUID id,
		String patientId,
		String patientName,
		String patientEmail,
		ComplaintArea complaintArea,
		String complaintAreaOther,
		ComplaintDuration duration,
		int severity,
		Set<Symptom> symptoms,
		String symptomsOther,
		boolean takesMedication,
		String currentMedication,
		boolean triedTreatment,
		String treatmentTried,
		String description,
		Intake.Status status,
		Instant submittedAt) {

	public static IntakeResponse from(Intake intake) {
		return new IntakeResponse(intake.getId(), intake.getPatientId(), intake.getPatientName(), intake.getPatientEmail(),
				intake.getComplaintArea(), intake.getComplaintAreaOther(), intake.getDuration(), intake.getSeverity(),
				intake.getSymptoms(), intake.getSymptomsOther(), intake.isTakesMedication(), intake.getCurrentMedication(),
				intake.isTriedTreatment(), intake.getTreatmentTried(), intake.getDescription(), intake.getStatus(),
				intake.getSubmittedAt());
	}
}
