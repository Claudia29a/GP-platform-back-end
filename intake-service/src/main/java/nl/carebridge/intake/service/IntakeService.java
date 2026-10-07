package nl.carebridge.intake.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import nl.carebridge.intake.model.Intake;

@Service
public class IntakeService {

	static final int MAX_DESCRIPTION_LENGTH = 1000;

	// Kept in memory for now; replaced by a database later.
	private final Map<UUID, Intake> intakes = new ConcurrentHashMap<>();

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
		intake.setId(UUID.randomUUID());
		intake.setDescription(trimmed);
		intake.setStatus(Intake.Status.SUBMITTED);
		intake.setSubmittedAt(Instant.now());
		intakes.put(intake.getId(), intake);
		return intake;
	}

	public List<Intake> findAll() {
		List<Intake> all = new ArrayList<>(intakes.values());
		all.sort(Comparator.comparing(Intake::getSubmittedAt));
		return all;
	}

	public Intake findById(UUID id) {
		Intake intake = intakes.get(id);
		if (intake == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Intake " + id + " not found");
		}
		return intake;
	}
}
