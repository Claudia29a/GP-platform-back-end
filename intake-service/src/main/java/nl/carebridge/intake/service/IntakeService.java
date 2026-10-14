package nl.carebridge.intake.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import nl.carebridge.intake.dto.SubmitIntakeRequest;
import nl.carebridge.intake.model.ComplaintArea;
import nl.carebridge.intake.model.Intake;
import nl.carebridge.intake.repository.IntakeRepository;
import nl.carebridge.intake.security.CurrentUser;

@Service
public class IntakeService {

	private final IntakeRepository intakeRepository;
	private final SeverityCalculator severityCalculator;
	private final QueueService queueService;

	public IntakeService(IntakeRepository intakeRepository, SeverityCalculator severityCalculator,
			QueueService queueService) {
		this.intakeRepository = intakeRepository;
		this.severityCalculator = severityCalculator;
		this.queueService = queueService;
	}

	/**
	 * Stores a validated question for the signed-in patient and places it in the practice's queue by
	 * severity score. Free-text answers are trimmed, and a follow-up answer is only kept when its
	 * question applies (e.g. "which medication" only after "yes").
	 */
	@Transactional
	public Intake submit(SubmitIntakeRequest request, CurrentUser patient) {
		Intake intake = new Intake();
		intake.setPatientId(patient.id());
		intake.setPatientName(patient.name());
		intake.setPatientEmail(patient.email());

		intake.setComplaintArea(request.complaintArea());
		intake.setComplaintAreaOther(request.complaintArea() == ComplaintArea.OTHER ? clean(request.complaintAreaOther()) : null);
		intake.setDuration(request.duration());
		intake.setSeverity(request.severity());
		intake.setSymptoms(request.symptoms());
		intake.setSymptomsOther(clean(request.symptomsOther()));
		intake.setTakesMedication(request.takesMedication());
		intake.setCurrentMedication(request.takesMedication() ? clean(request.currentMedication()) : null);
		intake.setTriedTreatment(request.triedTreatment());
		intake.setTreatmentTried(request.triedTreatment() ? clean(request.treatmentTried()) : null);
		intake.setDescription(clean(request.description()));

		intake.setStatus(Intake.Status.SUBMITTED);
		intake.setSubmittedAt(Instant.now());
		intake.setSeverityScore(severityCalculator.calculate(intake.getSeverity(), intake.getDuration(), intake.getSymptoms()));
		queueService.placeByScore(intake);
		return intakeRepository.save(intake);
	}

	/** Practice staff see every question; a patient only sees their own. Oldest first. */
	@Transactional(readOnly = true)
	public List<Intake> findVisibleTo(CurrentUser user) {
		return user.isStaff()
				? intakeRepository.findAllByOrderBySubmittedAtAsc()
				: intakeRepository.findAllByPatientIdOrderBySubmittedAtAsc(user.id());
	}

	/** Someone else's question answers 404 for a patient, so its existence is not revealed. */
	@Transactional(readOnly = true)
	public Intake findById(UUID id, CurrentUser user) {
		Optional<Intake> intake = user.isStaff()
				? intakeRepository.findById(id)
				: intakeRepository.findByIdAndPatientId(id, user.id());
		return intake.orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Intake " + id + " not found"));
	}

	/** Trims free text; an empty answer is stored as no answer. */
	private static String clean(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}
}
