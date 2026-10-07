package nl.carebridge.intake.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import nl.carebridge.intake.model.Intake;
import nl.carebridge.intake.repository.IntakeRepository;

@Service
public class IntakeService {

	static final int MAX_DESCRIPTION_LENGTH = 1000;

	private final IntakeRepository intakeRepository;

	public IntakeService(IntakeRepository intakeRepository) {
		this.intakeRepository = intakeRepository;
	}

	@Transactional
	public Intake submit(String description) {
		if (description == null || description.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description is required");
		}
		String trimmed = description.strip();
		if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Description must be at most " + MAX_DESCRIPTION_LENGTH + " characters");
		}

		Intake intake = new Intake();
		intake.setDescription(trimmed);
		intake.setStatus(Intake.Status.SUBMITTED);
		intake.setSubmittedAt(Instant.now());
		return intakeRepository.save(intake);
	}

	@Transactional(readOnly = true)
	public List<Intake> findAll() {
		return intakeRepository.findAllByOrderBySubmittedAtAsc();
	}

	@Transactional(readOnly = true)
	public Intake findById(UUID id) {
		return intakeRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Intake " + id + " not found"));
	}
}
