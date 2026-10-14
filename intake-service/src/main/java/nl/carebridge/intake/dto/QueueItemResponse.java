package nl.carebridge.intake.dto;

import java.time.Instant;
import java.util.UUID;

import nl.carebridge.intake.model.ComplaintArea;
import nl.carebridge.intake.model.ComplaintDuration;
import nl.carebridge.intake.model.Intake;

/** One question in the practice's queue, with the severity score that only staff see. */
public record QueueItemResponse(
		UUID id,
		int position,
		int severityScore,
		String patientName,
		ComplaintArea complaintArea,
		String complaintAreaOther,
		ComplaintDuration duration,
		int severity,
		String description,
		Instant submittedAt) {

	public static QueueItemResponse from(Intake intake) {
		return new QueueItemResponse(intake.getId(), intake.getQueuePosition(), intake.getSeverityScore(),
				intake.getPatientName(), intake.getComplaintArea(), intake.getComplaintAreaOther(), intake.getDuration(),
				intake.getSeverity(), intake.getDescription(), intake.getSubmittedAt());
	}
}
