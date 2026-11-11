package nl.carebridge.account.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import nl.carebridge.account.dto.AccountDataResponse;
import nl.carebridge.account.dto.AccountResponse;
import nl.carebridge.account.dto.LoginRequest;
import nl.carebridge.account.dto.RegisterRequest;
import nl.carebridge.account.dto.ResendRequest;
import nl.carebridge.account.dto.TokenResponse;
import nl.carebridge.account.dto.VerifyRequest;
import nl.carebridge.account.model.Account;
import nl.carebridge.account.service.AccountService;
import nl.carebridge.account.service.ClientAddress;
import nl.carebridge.account.service.OwnTokens;
import nl.carebridge.account.service.SignUpLimiter;
import nl.carebridge.account.service.TokenService;
import nl.carebridge.account.service.VerificationService;

@RestController
public class AccountController {

	private final AccountService accountService;
	private final TokenService tokenService;
	private final VerificationService verificationService;
	private final SignUpLimiter signUpLimiter;
	private final OwnTokens ownTokens;

	public AccountController(AccountService accountService, TokenService tokenService,
			VerificationService verificationService, SignUpLimiter signUpLimiter, OwnTokens ownTokens) {
		this.accountService = accountService;
		this.tokenService = tokenService;
		this.verificationService = verificationService;
		this.signUpLimiter = signUpLimiter;
		this.ownTokens = ownTokens;
	}

	/** Sign up with name, email and password; a code to confirm the address is emailed straight away. */
	@PostMapping("/accounts")
	@ResponseStatus(HttpStatus.CREATED)
	public AccountResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
		Optional<String> visitor = ClientAddress.of(http.getHeader("X-Forwarded-For"), http.getRemoteAddr());
		signUpLimiter.check(visitor);
		Account account = accountService.register(request);
		signUpLimiter.record(visitor);
		verificationService.sendNewCode(account);
		return AccountResponse.from(account);
	}

	/**
	 * Sign in: a correct password returns a signed token for the other services. An unverified account can
	 * sign in, but gets a code by email if it has no valid one, and its token says the address is unverified.
	 */
	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		Account account = accountService.authenticate(request);
		if (!account.isEmailVerified()) {
			verificationService.sendIfNoValidCode(account);
		}
		return token(account);
	}

	/** The right code verifies the address and returns a new token that says so. */
	@PostMapping("/accounts/verify")
	public TokenResponse verify(@Valid @RequestBody VerifyRequest request) {
		return token(verificationService.verify(request.email(), request.code()));
	}

	/** "Send a new code". The answer is the same for an unknown address, so nothing is revealed. */
	@PostMapping("/accounts/verify/resend")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void resend(@Valid @RequestBody ResendRequest request) {
		verificationService.resend(request.email());
	}

	/** "My data": the signed-in user's own account, found from their own token. */
	@GetMapping("/accounts/me")
	public AccountDataResponse me(@RequestHeader(name = "Authorization", required = false) String authorization) {
		return AccountDataResponse.from(accountService.find(ownTokens.accountId(authorization)));
	}

	/** "Delete my account": only with the user's own token. */
	@DeleteMapping("/accounts/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@RequestHeader(name = "Authorization", required = false) String authorization) {
		accountService.delete(ownTokens.accountId(authorization));
	}

	private TokenResponse token(Account account) {
		return TokenResponse.bearer(tokenService.issue(account), tokenService.lifetimeSeconds());
	}
}
