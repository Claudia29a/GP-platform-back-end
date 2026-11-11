package nl.carebridge.account.service;

import static nl.carebridge.account.TestAccounts.account;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import nl.carebridge.account.TestAccounts;
import nl.carebridge.account.config.VerificationProperties;
import nl.carebridge.account.encryption.DataProtection;
import nl.carebridge.account.model.Account;
import nl.carebridge.account.repository.AccountRepository;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class VerificationServiceTest {

	private static final String EMAIL = "anna@example.com";

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private CodeMailer mailer;

	private final DataProtection protection = new DataProtection(TestAccounts.DATA_KEY);
	private final MovableClock clock = new MovableClock(Instant.parse("2026-11-11T10:00:00Z"));
	private VerificationService service;
	private Account anna;

	@BeforeEach
	void createService() {
		service = new VerificationService(accountRepository, protection, mailer,
				new VerificationProperties(Duration.ofMinutes(15), 5, Duration.ofMinutes(1), 5, 5), clock);
		anna = account(EMAIL, "$2a$10$hash");
		anna.setEmailHash(protection.hash(EMAIL));
		lenient().when(accountRepository.findByEmailHash(protection.hash(EMAIL))).thenReturn(Optional.of(anna));
		lenient().when(accountRepository.findByEmailHash(protection.hash("nobody@example.com"))).thenReturn(Optional.empty());
		lenient().when(mailer.send(anyString(), anyString(), any())).thenReturn(true);
	}

	private String lastCodeSent() {
		ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
		verify(mailer, org.mockito.Mockito.atLeastOnce()).send(eq(EMAIL), code.capture(), eq(Duration.ofMinutes(15)));
		return code.getValue();
	}

	@Test
	void aNewAccountGetsASixDigitCodeByEmailAndOnlyAHashIsStored() {
		service.sendNewCode(anna);

		String code = lastCodeSent();
		assertThat(code).matches("\\d{6}");
		assertThat(anna.getVerificationCodeHash()).isNotNull().doesNotContain(code);
		assertThat(anna.getVerificationExpiresAt()).isEqualTo(clock.instant().plus(Duration.ofMinutes(15)));
	}

	@Test
	void theRightCodeVerifiesTheAccountAndWorksOnlyOnce() {
		service.sendNewCode(anna);
		String code = lastCodeSent();

		Account verified = service.verify(" Anna@Example.com ", code);

		assertThat(verified.isEmailVerified()).isTrue();
		assertThat(verified.getVerificationCodeHash()).isNull();
		assertThatThrownBy(() -> service.verify(EMAIL, code)).satisfies(VerificationServiceTest::isWrongCode);
	}

	@Test
	void aWrongCodeIsRefusedAndCounted() {
		service.sendNewCode(anna);

		assertThatThrownBy(() -> service.verify(EMAIL, "000000")).satisfies(VerificationServiceTest::isWrongCode);
		assertThat(anna.isEmailVerified()).isFalse();
		assertThat(anna.getVerificationAttempts()).isEqualTo(1);
	}

	@Test
	void afterFiveWrongCodesEvenTheRightCodeIsRefused() {
		service.sendNewCode(anna);
		String code = lastCodeSent();
		String wrong = code.equals("000000") ? "111111" : "000000";

		for (int i = 0; i < 5; i++) {
			assertThatThrownBy(() -> service.verify(EMAIL, wrong)).satisfies(VerificationServiceTest::isWrongCode);
		}

		assertThatThrownBy(() -> service.verify(EMAIL, code)).satisfies(VerificationServiceTest::isWrongCode);
		assertThat(anna.isEmailVerified()).isFalse();
	}

	@Test
	void anExpiredCodeIsRefused() {
		service.sendNewCode(anna);
		String code = lastCodeSent();

		clock.advance(Duration.ofMinutes(16));

		assertThatThrownBy(() -> service.verify(EMAIL, code)).satisfies(VerificationServiceTest::isWrongCode);
	}

	@Test
	void anUnknownAddressGetsTheSameAnswerAsAWrongCode() {
		assertThatThrownBy(() -> service.verify("nobody@example.com", "123456")).satisfies(VerificationServiceTest::isWrongCode);
	}

	@Test
	void aNewCodeReplacesTheOldOne() {
		service.sendNewCode(anna);
		String first = lastCodeSent();
		clock.advance(Duration.ofMinutes(2));
		service.resend(EMAIL);
		String second = lastCodeSent();

		if (!first.equals(second)) {
			assertThatThrownBy(() -> service.verify(EMAIL, first)).satisfies(VerificationServiceTest::isWrongCode);
		}
		assertThat(service.verify(EMAIL, second).isEmailVerified()).isTrue();
	}

	@Test
	void askingForANewCodeWithinAMinuteIsRefusedWithTheWaitingTime() {
		service.sendNewCode(anna);
		clock.advance(Duration.ofSeconds(20));

		assertThatThrownBy(() -> service.resend(EMAIL)).isInstanceOfSatisfying(TooManyRequestsException.class, ex -> {
			assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
			assertThat(ex.getRetryAfter()).isEqualTo(Duration.ofSeconds(40));
		});
	}

	@Test
	void atMostFiveCodesAreSentPerHour() {
		service.sendNewCode(anna);
		for (int i = 0; i < 4; i++) {
			clock.advance(Duration.ofMinutes(2));
			service.resend(EMAIL);
		}
		clock.advance(Duration.ofMinutes(2));

		assertThatThrownBy(() -> service.resend(EMAIL)).isInstanceOfSatisfying(TooManyRequestsException.class,
				ex -> assertThat(ex.getRetryAfter()).isEqualTo(Duration.ofMinutes(50)));

		clock.advance(Duration.ofMinutes(50));
		service.resend(EMAIL);
	}

	@Test
	void noCodeIsSentForAnUnknownOrAlreadyVerifiedAddress() {
		service.resend("nobody@example.com");
		anna.setEmailVerified(true);
		service.resend(EMAIL);

		verify(mailer, never()).send(anyString(), anyString(), any());
	}

	@Test
	void signingInSendsACodeOnlyWhenThereIsNoValidCode() {
		service.sendIfNoValidCode(anna);
		lastCodeSent();
		clearInvocations(mailer);

		clock.advance(Duration.ofMinutes(5));
		service.sendIfNoValidCode(anna);
		verify(mailer, never()).send(anyString(), anyString(), any());

		clock.advance(Duration.ofMinutes(11));
		service.sendIfNoValidCode(anna);
		lastCodeSent();
	}

	@Test
	void signingInNeverFailsBecauseOfTheSendingLimit() {
		service.sendNewCode(anna);
		clock.advance(Duration.ofMinutes(16));
		anna.setLastCodeSentAt(clock.instant());
		clearInvocations(mailer);

		service.sendIfNoValidCode(anna);

		verify(mailer, never()).send(anyString(), anyString(), any());
	}

	private static void isWrongCode(Throwable ex) {
		assertThat(ex).isInstanceOf(ResponseStatusException.class);
		assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(((ResponseStatusException) ex).getReason()).isEqualTo(VerificationService.WRONG_CODE);
	}

	/** A clock the test can move forward, to check expiry and waiting times. */
	static final class MovableClock extends Clock {

		private Instant now;

		MovableClock(Instant start) {
			this.now = start;
		}

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}
	}
}
