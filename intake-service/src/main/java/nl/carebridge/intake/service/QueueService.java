package nl.carebridge.intake.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import nl.carebridge.intake.model.Intake;
import nl.carebridge.intake.repository.IntakeRepository;

/**
 * The practice's queue of open questions (SAD v2, process 2). New questions are placed by severity
 * score; the GP assistant or GP can then change the order by hand, and their order always wins.
 */
@Service
public class QueueService {

	private final IntakeRepository intakeRepository;

	public QueueService(IntakeRepository intakeRepository) {
		this.intakeRepository = intakeRepository;
	}

	@Transactional(readOnly = true)
	public List<Intake> currentQueue() {
		return intakeRepository.findAllByStatusOrderByQueuePositionAscSubmittedAtAsc(Intake.Status.SUBMITTED);
	}

	/**
	 * Gives a new question its place: right after the last question with an equal or higher score.
	 * In a queue sorted by score that is simply its sorted place; after manual reordering it never
	 * jumps above a question staff moved up, because their order wins (SAD v2, section 2.4).
	 * Equal scores keep arrival order. Runs inside the caller's transaction; the queue is renumbered.
	 */
	@Transactional
	public void placeByScore(Intake newIntake) {
		List<Intake> queue = new ArrayList<>(currentQueue());
		int index = 0;
		for (int i = 0; i < queue.size(); i++) {
			if (queue.get(i).getSeverityScore() >= newIntake.getSeverityScore()) {
				index = i + 1;
			}
		}
		queue.add(index, newIntake);
		renumber(queue);
	}

	/**
	 * Saves the order a staff member dragged the queue into. The list must hold exactly the open
	 * questions; if a question arrived or left in the meantime, the change is refused with 409 so the
	 * screen can reload instead of silently dropping or misplacing a question.
	 */
	@Transactional
	public List<Intake> reorder(List<UUID> intakeIds) {
		List<Intake> queue = currentQueue();
		Set<UUID> requested = new HashSet<>(intakeIds);
		Set<UUID> open = new HashSet<>(queue.stream().map(Intake::getId).toList());
		if (requested.size() != intakeIds.size() || !requested.equals(open)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "The queue has changed; reload it and try again");
		}

		List<Intake> ordered = intakeIds.stream()
				.map(id -> queue.stream().filter(intake -> intake.getId().equals(id)).findFirst().orElseThrow())
				.toList();
		renumber(ordered);
		return ordered;
	}

	private static void renumber(List<Intake> queue) {
		for (int i = 0; i < queue.size(); i++) {
			queue.get(i).setQueuePosition(i + 1);
		}
	}
}
