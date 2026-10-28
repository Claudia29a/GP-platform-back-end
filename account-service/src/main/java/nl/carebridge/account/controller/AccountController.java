package nl.carebridge.account.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import nl.carebridge.account.dto.AccountResponse;
import nl.carebridge.account.dto.LoginRequest;
import nl.carebridge.account.dto.RegisterRequest;
import nl.carebridge.account.dto.TokenResponse;
import nl.carebridge.account.service.AccountService;
import nl.carebridge.account.service.TokenService;

@RestController
public class AccountController {

	private final AccountService accountService;
	private final TokenService tokenService;

	public AccountController(AccountService accountService, TokenService tokenService) {
		this.accountService = accountService;
		this.tokenService = tokenService;
	}

	/** Sign up with name, email and password. */
	@PostMapping("/accounts")
	@ResponseStatus(HttpStatus.CREATED)
	public AccountResponse register(@Valid @RequestBody RegisterRequest request) {
		return AccountResponse.from(accountService.register(request));
	}

	/** Sign in: a correct password returns a signed token for the other services. */
	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return TokenResponse.bearer(tokenService.issue(accountService.authenticate(request)), tokenService.lifetimeSeconds());
	}
}
