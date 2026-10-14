package nl.carebridge.intake.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import nl.carebridge.intake.model.Intake;
import nl.carebridge.intake.repository.IntakeRepository;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

	@Mock
	private IntakeRepository intakeRepository;

	@InjectMocks
	private QueueService queueService;

	@Test
	void newQuestionGoesAfterTheLastEqualOrHigherScore() {
		Intake a = queued(1, 80);
		Intake b = queued(2, 50);
		Intake c = queued(3, 20);
		givenQueue(a, b, c);
		Intake added = withScore(60);

		queueService.placeByScore(added);

		assertPositions(a, 1, added, 2, b, 3, c, 4);
	}

	@Test
	void equalScoresKeepArrivalOrder() {
		Intake a = queued(1, 50);
		givenQueue(a);
		Intake added = withScore(50);

		queueService.placeByScore(added);

		assertPositions(a, 1, added, 2);
	}

	@Test
	void lowestScoreGoesLastAndFirstQuestionGoesFirst() {
		Intake a = queued(1, 40);
		givenQueue(a);
		Intake lowest = withScore(10);
		queueService.placeByScore(lowest);
		assertPositions(a, 1, lowest, 2);

		givenQueue();
		Intake only = withScore(30);
		queueService.placeByScore(only);
		assertThat(only.getQueuePosition()).isEqualTo(1);
	}

	@Test
	void newQuestionIsPlacedByScoreEvenAfterManualReordering() {
		// Staff moved the low-scoring question to the top.
		Intake movedUp = queued(1, 20);
		Intake high = queued(2, 90);
		Intake middle = queued(3, 40);
		givenQueue(movedUp, high, middle);
		Intake added = withScore(60);

		queueService.placeByScore(added);

		assertPositions(movedUp, 1, high, 2, added, 3, middle, 4);
	}

	@Test
	void mostSevereNewQuestionGoesToTheTop() {
		Intake movedUp = queued(1, 20);
		Intake other = queued(2, 40);
		givenQueue(movedUp, other);
		Intake added = withScore(95);

		queueService.placeByScore(added);

		assertPositions(added, 1, movedUp, 2, other, 3);
	}

	@Test
	void reorderSavesTheGivenOrder() {
		Intake a = queued(1, 80);
		Intake b = queued(2, 50);
		Intake c = queued(3, 20);
		givenQueue(a, b, c);

		List<Intake> result = queueService.reorder(List.of(c.getId(), a.getId(), b.getId()));

		assertThat(result).containsExactly(c, a, b);
		assertPositions(c, 1, a, 2, b, 3);
	}

	@Test
	void reorderIsRefusedWhenAQuestionIsMissingExtraOrDuplicated() {
		Intake a = queued(1, 80);
		Intake b = queued(2, 50);
		givenQueue(a, b);

		assertConflict(List.of(a.getId()));
		assertConflict(List.of(a.getId(), b.getId(), UUID.randomUUID()));
		assertConflict(List.of(a.getId(), a.getId()));
		assertPositions(a, 1, b, 2);
	}

	private void assertConflict(List<UUID> ids) {
		assertThatThrownBy(() -> queueService.reorder(ids))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
	}

	private void givenQueue(Intake... intakes) {
		when(intakeRepository.findAllByStatusOrderByQueuePositionAscSubmittedAtAsc(Intake.Status.SUBMITTED))
				.thenReturn(new ArrayList<>(Arrays.asList(intakes)));
	}

	private static Intake queued(int position, int score) {
		Intake intake = withScore(score);
		intake.setId(UUID.randomUUID());
		intake.setQueuePosition(position);
		return intake;
	}

	private static Intake withScore(int score) {
		Intake intake = new Intake();
		intake.setSeverityScore(score);
		intake.setStatus(Intake.Status.SUBMITTED);
		return intake;
	}

	/** Pairs of (question, expected position). */
	private static void assertPositions(Object... pairs) {
		for (int i = 0; i < pairs.length; i += 2) {
			assertThat(((Intake) pairs[i]).getQueuePosition()).as("position %d", i / 2 + 1).isEqualTo(pairs[i + 1]);
		}
	}
}
