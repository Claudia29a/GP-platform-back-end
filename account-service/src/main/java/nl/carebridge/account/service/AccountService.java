package nl.carebridge.account.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import nl.carebridge.account.config.LoginProperties;
import nl.carebridge.account.dto.LoginRequest;
import nl.carebridge.account.dto.RegisterRequest;
import nl.carebridge.account.model.Account;
import nl.carebridge.account.repository.AccountRepository;

@Service
public class AccountService {

	static final String WRONG_CREDENTIALS = "Email or password is incorrect";

	private final AccountRepository accountRepository;
	private final PasswordEncoder passwordEncoder;
	private final LoginProperties loginProperties;
	private final Clock clock;
	/** Compared against when the email is unknown, so a wrong email takes as long as a wrong password. */
	private final String unknownAccountHash;

	public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder,
			LoginProperties loginProperties, Clock clock) {
		this.accountRepository = accountRepository;
		this.passwordEncoder = passwordEncoder;
		this.loginProperties = loginProperties;
		this.clock = clock;
		this.unknownAccountHash = passwordEncoder.encode("no account has this password");
	}

	/** Creates an account; the address is not verified yet, so it never gets a staff role on its own. */
	@Transactional
	public Account register(RegisterRequest request) {
		String email = normalize(request.email());
		if (accountRepository.existsByEmail(email)) {
			throw emailTaken();
		}
		Account account = new Account();
		account.setEmail(email);
		account.setName(request.name().strip());
		account.setPasswordHash(passwordEncoder.encode(request.password()));
		account.setEmailVerified(false);
		account.setCreatedAt(Instant.now(clock));
		try {
			return accountRepository.saveAndFlush(account);
		}
		catch (DataIntegrityViolationException ex) {
			// Someone registered the same address at the same moment.
			throw emailTaken();
		}
	}

	/**
	 * Checks the password and returns the account. A wrong email and a wrong password give the same
	 * answer, so nobody can find out which addresses have an account by trying passwords. After too
	 * many wrong passwords the account is locked for a while, even for the right password.
	 */
	@Transactional(noRollbackFor = ResponseStatusException.class)
	public Account authenticate(LoginRequest request) {
		Optional<Account> found = accountRepository.findByEmail(normalize(request.email()));
		if (found.isEmpty()) {
			passwordEncoder.matches(request.password(), unknownAccountHash);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, WRONG_CREDENTIALS);
		}
		Account account = found.get();
		Instant now = Instant.now(clock);
		if (account.getLockedUntil() != null && now.isBefore(account.getLockedUntil())) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
					"Too many wrong passwords. Please try again in " + loginProperties.lockDuration().toMinutes() + " minutes.");
		}
		if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
			registerFailure(account, now);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, WRONG_CREDENTIALS);
		}
		account.setFailedLoginAttempts(0);
		account.setLockedUntil(null);
		return account;
	}

	private void registerFailure(Account account, Instant now) {
		int failures = account.getFailedLoginAttempts() + 1;
		if (failures >= loginProperties.maxFailedAttempts()) {
			account.setLockedUntil(now.plus(loginProperties.lockDuration()));
			failures = 0;
		}
		account.setFailedLoginAttempts(failures);
		accountRepository.save(account);
	}

	private static String normalize(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}

	private static ResponseStatusException emailTaken() {
		return new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email address already exists");
	}
}
