package nl.carebridge.account.service;

import static org.assertj.core.api.Assertions.assertThat;
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

import nl.carebridge.account.repository.AccountRepository;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class UnconfirmedAccountCleanerTest {

	private static final Instant NOW = Instant.parse("2026-11-17T10:00:00Z");

	@Mock
	private AccountRepository repository;

	@Test
	void deletesUnconfirmedAccountsOlderThanTheSetAge() {
		when(repository.deleteUnconfirmedCreatedBefore(NOW.minus(Duration.ofHours(24)))).thenReturn(3);

		int deleted = new UnconfirmedAccountCleaner(repository, Duration.ofHours(24), Clock.fixed(NOW, ZoneOffset.UTC)).cleanUp();

		assertThat(deleted).isEqualTo(3);
		verify(repository).deleteUnconfirmedCreatedBefore(NOW.minus(Duration.ofHours(24)));
	}
}
