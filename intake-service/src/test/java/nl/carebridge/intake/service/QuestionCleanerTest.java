package nl.carebridge.intake.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import nl.carebridge.intake.repository.IntakeRepository;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class QuestionCleanerTest {

	private static final Instant NOW = Instant.parse("2026-11-17T10:00:00Z");
	private static final Duration SIX_MONTHS = Duration.ofDays(183);

	@Mock
	private IntakeRepository repository;

	@Mock
	private QueueService queueService;

	private QuestionCleaner cleaner() {
		return new QuestionCleaner(repository, queueService, SIX_MONTHS, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void deletesQuestionsOlderThanTheRetentionPeriodThenClosesTheQueue() {
		when(repository.deleteSubmittedBefore(NOW.minus(SIX_MONTHS))).thenReturn(2);

		assertThat(cleaner().cleanUp()).isEqualTo(2);

		var order = inOrder(repository, queueService);
		order.verify(repository).deleteSubmittedBefore(NOW.minus(SIX_MONTHS));
		order.verify(queueService).closeGaps();
	}

	@Test
	void leavesTheQueueAloneWhenNothingWasOldEnough() {
		when(repository.deleteSubmittedBefore(NOW.minus(SIX_MONTHS))).thenReturn(0);

		assertThat(cleaner().cleanUp()).isZero();

		verify(queueService, never()).closeGaps();
	}
}
