package nl.carebridge.account.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import nl.carebridge.account.config.VerificationProperties;
import nl.carebridge.account.encryption.DataProtection;
import nl.carebridge.account.model.Account;
import nl.carebridge.account.repository.AccountRepository;

/**
 * Proves that an email address belongs to the person who signed up: a 6-digit code is emailed and has to be
 * typed in. Only a keyed hash of the code is stored. A code expires, works once and stops working after too
 * many wrong tries, and new codes are rate-limited so the feature cannot be used to flood someone's inbox.
 */
@Service
public class VerificationService {

	static final String WRONG_CODE = "This code is not correct or has expired. Please check the email or ask for a new code.";
	private static final Duration HOUR = Duration.ofHours(1);

	private final SecureRandom random = new SecureRandom();
	private final AccountRepository accountRepository;
	private final DataProtection protection;
	private final CodeMailer mailer;
	private final VerificationProperties properties;
	private final Clock clock;

	public VerificationService(AccountRepository accountRepository, DataProtection protection, CodeMailer mailer,
			VerificationProperties properties, Clock clock) {
		this.accountRepository = accountRepository;
		this.protection = protection;
		this.mailer = mailer;
		this.properties = properties;
		this.clock = clock;
	}

	/** Right after sign-up. */
	@Transactional
	public void sendNewCode(Account account) {
		send(account, Instant.now(clock));
	}

	/**
	 * At sign-in to an unverified account, for example one created before verification existed. Sends a code
	 * only when there is no valid one, and quietly respects the limits: signing in itself never fails here.
	 */
	@Transactional
	public void sendIfNoValidCode(Account account) {
		Instant now = Instant.now(clock);
		boolean hasValidCode = account.getVerificationCodeHash() != null && now.isBefore(account.getVerificationExpiresAt());
		if (!account.isEmailVerified() && !hasValidCode && waitBeforeSending(account, now).isEmpty()) {
			send(account, now);
		}
	}

	/** "Send a new code". Nothing is sent for an unknown or already verified address. */
	@Transactional
	public void resend(String email) {
		Optional<Account> found = accountRepository.findByEmailHash(protection.hash(normalize(email)));
		if (found.isEmpty() || found.get().isEmailVerified()) {
			return;
		}
		Instant now = Instant.now(clock);
		Optional<Duration> wait = waitBeforeSending(found.get(), now);
		if (wait.isPresent()) {
			throw new TooManyRequestsException("Please wait before asking for a new code.", wait.get());
		}
		send(found.get(), now);
	}

	/** Every failure gives the same answer, so it reveals nothing about the account. */
	@Transactional(noRollbackFor = ResponseStatusException.class)
	public Account verify(String email, String code) {
		Account account = accountRepository.findByEmailHash(protection.hash(normalize(email)))
				.orElseThrow(VerificationService::wrongCode);
		Instant now = Instant.now(clock);
		if (account.getVerificationCodeHash() == null || !now.isBefore(account.getVerificationExpiresAt())) {
			throw wrongCode();
		}
		if (!account.getVerificationCodeHash().equals(hash(account, code))) {
			account.setVerificationAttempts(account.getVerificationAttempts() + 1);
			if (account.getVerificationAttempts() >= properties.maxAttempts()) {
				// Too many guesses: this code is finished and a new one is needed.
				clearCode(account);
			}
			accountRepository.save(account);
			throw wrongCode();
		}
		account.setEmailVerified(true);
		clearCode(account);
		accountRepository.save(account);
		return account;
	}

	private void send(Account account, Instant now) {
		String code = "%06d".formatted(random.nextInt(1_000_000));
		account.setVerificationCodeHash(hash(account, code));
		account.setVerificationExpiresAt(now.plus(properties.codeLifetime()));
		account.setVerificationAttempts(0);
		account.setLastCodeSentAt(now);
		if (account.getCodesSentSince() == null || !now.isBefore(account.getCodesSentSince().plus(HOUR))) {
			account.setCodesSentSince(now);
			account.setCodesSentCount(0);
		}
		account.setCodesSentCount(account.getCodesSentCount() + 1);
		accountRepository.save(account);
		mailer.send(account.getEmail(), code, properties.codeLifetime());
	}

	private Optional<Duration> waitBeforeSending(Account account, Instant now) {
		if (account.getLastCodeSentAt() != null) {
			Instant allowed = account.getLastCodeSentAt().plus(properties.resendInterval());
			if (now.isBefore(allowed)) {
				return Optional.of(Duration.between(now, allowed));
			}
		}
		if (account.getCodesSentSince() != null && account.getCodesSentCount() >= properties.maxCodesPerHour()) {
			Instant allowed = account.getCodesSentSince().plus(HOUR);
			if (now.isBefore(allowed)) {
				return Optional.of(Duration.between(now, allowed));
			}
		}
		return Optional.empty();
	}

	/** Keyed with the data key and the account, so the stored hash is useless without both. */
	private String hash(Account account, String code) {
		return protection.hash(account.getId() + ":" + code);
	}

	private static void clearCode(Account account) {
		account.setVerificationCodeHash(null);
		account.setVerificationExpiresAt(null);
		account.setVerificationAttempts(0);
	}

	private static String normalize(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}

	private static ResponseStatusException wrongCode() {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, WRONG_CODE);
	}
}
