package nl.carebridge.account.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import nl.carebridge.account.TestAccounts;
import nl.carebridge.account.config.VerificationProperties;
import nl.carebridge.account.encryption.DataProtection;
import nl.carebridge.account.model.SignUp;
import nl.carebridge.account.repository.SignUpRepository;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SignUpLimiterTest {

	private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");

	@Mock
	private SignUpRepository repository;

	private final DataProtection protection = new DataProtection(TestAccounts.DATA_KEY);
	private SignUpLimiter limiter;

	@BeforeEach
	void createLimiter() {
		limiter = new SignUpLimiter(repository, protection,
				new VerificationProperties(Duration.ofMinutes(15), 5, Duration.ofMinutes(1), 5, 5), Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void allowsFiveSignUpsAnHourFromOneAddress() {
		when(repository.countByIpHashAndCreatedAtAfter(protection.hash("81.204.10.20"), NOW.minus(Duration.ofHours(1)))).thenReturn(4L);

		assertThatCode(() -> limiter.check(Optional.of("81.204.10.20"))).doesNotThrowAnyException();
	}

	@Test
	void refusesTheSixthWithTheWaitingTime() {
		when(repository.countByIpHashAndCreatedAtAfter(protection.hash("81.204.10.20"), NOW.minus(Duration.ofHours(1)))).thenReturn(5L);
		when(repository.findOldestCreatedAt(protection.hash("81.204.10.20"), NOW.minus(Duration.ofHours(1))))
				.thenReturn(Optional.of(NOW.minus(Duration.ofMinutes(45))));

		assertThatThrownBy(() -> limiter.check(Optional.of("81.204.10.20"))).isInstanceOfSatisfying(TooManyRequestsException.class,
				ex -> org.assertj.core.api.Assertions.assertThat(ex.getRetryAfter()).isEqualTo(Duration.ofMinutes(15)));
	}

	@Test
	void storesOnlyAHashOfTheAddressAndForgetsOldOnes() {
		limiter.record(Optional.of("81.204.10.20"));

		ArgumentCaptor<SignUp> saved = ArgumentCaptor.forClass(SignUp.class);
		verify(repository).save(saved.capture());
		org.assertj.core.api.Assertions.assertThat(saved.getValue().getIpHash()).isEqualTo(protection.hash("81.204.10.20"));
		verify(repository).deleteByCreatedAtBefore(NOW.minus(Duration.ofHours(1)));
	}

	@Test
	void withoutAVisitorAddressThereIsNothingToLimit() {
		assertThatCode(() -> limiter.check(Optional.empty())).doesNotThrowAnyException();
		limiter.record(Optional.empty());

		verify(repository, never()).save(any());
	}
}
