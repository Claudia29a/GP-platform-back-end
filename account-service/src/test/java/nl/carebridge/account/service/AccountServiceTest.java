package nl.carebridge.account.service;

import static nl.carebridge.account.TestAccounts.account;
import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import nl.carebridge.account.config.LoginProperties;
import nl.carebridge.account.dto.LoginRequest;
import nl.carebridge.account.dto.RegisterRequest;
import nl.carebridge.account.model.Account;
import nl.carebridge.account.repository.AccountRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-28T10:00:00Z");

	@Mock
	private AccountRepository accountRepository;

	/** Low cost factor keeps the tests fast; production uses the default. */
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
	private AccountService accountService;

	@BeforeEach
	void createService() {
		accountService = new AccountService(accountRepository, passwordEncoder,
				new LoginProperties(3, Duration.ofMinutes(15)), Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void registerStoresAHashNeverThePassword() {
		when(accountRepository.saveAndFlush(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Account account = accountService.register(new RegisterRequest("  Anna de Vries ", " Anna@Example.COM ", "Secret12"));

		assertThat(account.getEmail()).isEqualTo("anna@example.com");
		assertThat(account.getName()).isEqualTo("Anna de Vries");
		assertThat(account.getPasswordHash()).isNotEqualTo("Secret12").startsWith("$2");
		assertThat(passwordEncoder.matches("Secret12", account.getPasswordHash())).isTrue();
		assertThat(account.isEmailVerified()).isFalse();
		assertThat(account.getCreatedAt()).isEqualTo(NOW);
	}

	@Test
	void registerRefusesAnEmailThatAlreadyHasAnAccount() {
		when(accountRepository.existsByEmail("anna@example.com")).thenReturn(true);

		assertStatus(() -> accountService.register(new RegisterRequest("Anna", "ANNA@example.com", "Secret12")), HttpStatus.CONFLICT);
		verify(accountRepository, never()).saveAndFlush(any());
	}

	@Test
	void registerRefusesWhenTheSameEmailWasRegisteredAtTheSameMoment() {
		when(accountRepository.saveAndFlush(any(Account.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

		assertStatus(() -> accountService.register(new RegisterRequest("Anna", "anna@example.com", "Secret12")), HttpStatus.CONFLICT);
	}

	@Test
	void correctPasswordSignsInAndClearsEarlierFailures() {
		Account account = stored();
		account.setFailedLoginAttempts(2);

		assertThat(accountService.authenticate(new LoginRequest("ANNA@example.com ", "Secret12"))).isSameAs(account);
		assertThat(account.getFailedLoginAttempts()).isZero();
	}

	@Test
	void wrongPasswordAndUnknownEmailGiveTheSameAnswer() {
		stored();
		when(accountRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		ResponseStatusException wrongPassword = rejected(new LoginRequest("anna@example.com", "Wrong123"));
		ResponseStatusException unknownEmail = rejected(new LoginRequest("nobody@example.com", "Wrong123"));

		assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED).isEqualTo(unknownEmail.getStatusCode());
		assertThat(wrongPassword.getReason()).isEqualTo(unknownEmail.getReason()).isEqualTo(AccountService.WRONG_CREDENTIALS);
	}

	@Test
	void tooManyWrongPasswordsLockTheAccount() {
		Account account = stored();

		rejected(new LoginRequest("anna@example.com", "Wrong123"));
		rejected(new LoginRequest("anna@example.com", "Wrong123"));
		assertThat(account.getLockedUntil()).isNull();
		rejected(new LoginRequest("anna@example.com", "Wrong123"));

		assertThat(account.getLockedUntil()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
		assertStatus(() -> accountService.authenticate(new LoginRequest("anna@example.com", "Secret12")), HttpStatus.TOO_MANY_REQUESTS);
	}

	@Test
	void lockEndsAfterItsTime() {
		Account account = stored();
		account.setLockedUntil(NOW.minusSeconds(1));

		assertThat(accountService.authenticate(new LoginRequest("anna@example.com", "Secret12"))).isSameAs(account);
		assertThat(account.getLockedUntil()).isNull();
	}

	private Account stored() {
		Account account = account("anna@example.com", passwordEncoder.encode("Secret12"));
		when(accountRepository.findByEmail("anna@example.com")).thenReturn(Optional.of(account));
		return account;
	}

	private ResponseStatusException rejected(LoginRequest request) {
		try {
			accountService.authenticate(request);
		}
		catch (ResponseStatusException ex) {
			return ex;
		}
		throw new AssertionError("expected the sign-in to be rejected");
	}

	private static void assertStatus(Runnable action, HttpStatusCode status) {
		assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class,
				ex -> assertThat(ex.getStatusCode()).isEqualTo(status));
	}
}
