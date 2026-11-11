package nl.carebridge.intake.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import nl.carebridge.intake.TestIntakes;
import nl.carebridge.intake.dto.AccessLogResponse;
import nl.carebridge.intake.encryption.DataProtection;
import nl.carebridge.intake.model.AccessLogEntry;
import nl.carebridge.intake.repository.AccessLogRepository;
import nl.carebridge.intake.security.CurrentUser;
import nl.carebridge.intake.security.Role;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AccessLogServiceTest {

	private static final CurrentUser ASSISTANT = new CurrentUser("google-sam", "Sam Bakker", "sam@example.com", Role.GP_ASSISTANT);

	@Mock
	private AccessLogRepository repository;

	private final DataProtection protection = new DataProtection(TestIntakes.DATA_KEY);

	@Test
	void recordsWhoOpenedWhichQuestionAndWhen() {
		UUID question = UUID.randomUUID();
		Instant before = Instant.now();

		new AccessLogService(repository, protection).record(question, ASSISTANT);

		ArgumentCaptor<AccessLogEntry> saved = ArgumentCaptor.forClass(AccessLogEntry.class);
		verify(repository).save(saved.capture());
		assertThat(saved.getValue().getIntakeId()).isEqualTo(question);
		assertThat(saved.getValue().getStaffHash()).isEqualTo(protection.hash("google-sam"));
		assertThat(saved.getValue().getStaffName()).isEqualTo("Sam Bakker");
		assertThat(saved.getValue().getStaffRole()).isEqualTo(Role.GP_ASSISTANT);
		assertThat(saved.getValue().getViewedAt()).isAfterOrEqualTo(before);
	}

	@Test
	void listsTheAccessesPerQuestionNewestFirst() {
		UUID first = UUID.randomUUID();
		UUID second = UUID.randomUUID();
		AccessLogEntry older = new AccessLogEntry(first, "hash", "Sam Bakker", Role.GP_ASSISTANT, Instant.parse("2026-11-10T09:00:00Z"));
		AccessLogEntry newer = new AccessLogEntry(first, "hash", "Dr. Jansen", Role.GP, Instant.parse("2026-11-11T09:00:00Z"));
		when(repository.findAllByIntakeIdInOrderByViewedAtDesc(List.of(first, second))).thenReturn(List.of(newer, older));

		Map<UUID, List<AccessLogResponse>> log = new AccessLogService(repository, protection).entriesFor(List.of(first, second));

		assertThat(log.get(first)).extracting(AccessLogResponse::staffName).containsExactly("Dr. Jansen", "Sam Bakker");
		assertThat(log.get(second)).isEmpty();
	}
}
