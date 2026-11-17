package nl.carebridge.messaging.service;

import static org.assertj.core.api.Assertions.assertThat;
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

import nl.carebridge.messaging.repository.ReplyRepository;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ReplyCleanerTest {

	private static final Instant NOW = Instant.parse("2026-11-17T10:00:00Z");

	@Mock
	private ReplyRepository repository;

	@Test
	void deletesRepliesTogetherWithTheirQuestionAfterTheRetentionPeriod() {
		when(repository.deleteQuestionSubmittedBefore(NOW.minus(Duration.ofDays(183)))).thenReturn(2);

		assertThat(new ReplyCleaner(repository, Duration.ofDays(183), Clock.fixed(NOW, ZoneOffset.UTC)).cleanUp()).isEqualTo(2);
	}
}
