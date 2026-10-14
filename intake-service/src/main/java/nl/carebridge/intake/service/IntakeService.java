package nl.carebridge.intake.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import nl.carebridge.intake.model.Intake;
import nl.carebridge.intake.repository.IntakeRepository;
import nl.carebridge.intake.security.CurrentUser;

@Service
public class IntakeService {

	static final int MAX_DESCRIPTION_LENGTH = 1000;

	private final IntakeRepository intakeRepository;

	public IntakeService(IntakeRepository intakeRepository) {
		this.intakeRepository = intakeRepository;
	}

	@Transactional
	public Intake submit(String description, CurrentUser patient) {
		if (description == null || description.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description is required");
		}
		String trimmed = description.strip();
		if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Description must be at most " + MAX_DESCRIPTION_LENGTH + " characters");
		}

		Intake intake = new Intake();
		intake.setPatientId(patient.id());
		intake.setPatientName(patient.name());
		intake.setPatientEmail(patient.email());
		intake.setDescription(trimmed);
		intake.setStatus(Intake.Status.SUBMITTED);
		intake.setSubmittedAt(Instant.now());
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
}
