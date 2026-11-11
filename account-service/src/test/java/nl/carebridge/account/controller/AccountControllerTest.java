package nl.carebridge.account.controller;

import static nl.carebridge.account.TestAccounts.account;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import nl.carebridge.account.dto.LoginRequest;
import nl.carebridge.account.service.AccountService;
import nl.carebridge.account.service.SigningKey;
import nl.carebridge.account.service.OwnTokens;
import nl.carebridge.account.service.SignUpLimiter;
import nl.carebridge.account.service.TokenService;
import nl.carebridge.account.service.TooManyRequestsException;
import nl.carebridge.account.service.VerificationService;

@WebMvcTest({AccountController.class, JwksController.class})
@Tag("api")
class AccountControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AccountService accountService;

	@MockitoBean
	private TokenService tokenService;

	@MockitoBean
	private SigningKey signingKey;

	@MockitoBean
	private VerificationService verificationService;

	@MockitoBean
	private SignUpLimiter signUpLimiter;

	@MockitoBean
	private OwnTokens ownTokens;

	@Test
	void signUpReturnsTheAccountWithoutPasswordData() throws Exception {
		when(accountService.register(any())).thenReturn(account("anna@example.com", "$2a$10$hash"));

		mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"Anna de Vries\", \"email\": \"anna@example.com\", \"password\": \"Secret12\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("anna@example.com"))
				.andExpect(jsonPath("$.name").value("Anna de Vries"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void signUpWithAWeakPasswordIsRejectedBeforeAnythingIsStored() throws Exception {
		mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"Anna\", \"email\": \"anna@example.com\", \"password\": \"secret\"}"))
				.andExpect(status().isBadRequest());

		verify(accountService, never()).register(any());
	}

	@Test
	void signInReturnsABearerToken() throws Exception {
		when(accountService.authenticate(new LoginRequest("anna@example.com", "Secret12")))
				.thenReturn(account("anna@example.com", "$2a$10$hash"));
		when(tokenService.issue(any())).thenReturn("signed.jwt.token");
		when(tokenService.lifetimeSeconds()).thenReturn(3600L);

		mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"anna@example.com\", \"password\": \"Secret12\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("signed.jwt.token"))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(3600));
	}

	@Test
	void wrongPasswordIsUnauthorized() throws Exception {
		when(accountService.authenticate(any()))
				.thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect"));

		mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"anna@example.com\", \"password\": \"Wrong123\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void publishesThePublicKeys() throws Exception {
		when(signingKey.publicKeys()).thenReturn(new SigningKey(
				new nl.carebridge.account.config.TokenProperties("iss", "aud", java.time.Duration.ofHours(1), "")).publicKeys());

		mockMvc.perform(get("/.well-known/jwks.json"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.keys[0].kty").value("RSA"))
				.andExpect(jsonPath("$.keys[0].d").doesNotExist());
	}

	@Test
	void signUpChecksTheLimitForTheVisitorThenSendsACode() throws Exception {
		var created = account("anna@example.com", "$2a$10$hash");
		when(accountService.register(any())).thenReturn(created);

		mockMvc.perform(post("/accounts").header("X-Forwarded-For", "81.204.10.20, 172.18.0.5")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"Anna\", \"email\": \"anna@example.com\", \"password\": \"Secret12\"}"))
				.andExpect(status().isCreated());

		var order = inOrder(signUpLimiter, accountService, verificationService);
		order.verify(signUpLimiter).check(Optional.of("81.204.10.20"));
		order.verify(accountService).register(any());
		order.verify(signUpLimiter).record(Optional.of("81.204.10.20"));
		order.verify(verificationService).sendNewCode(created);
	}

	@Test
	void tooManySignUpsAreRefusedWithTheWaitingTime() throws Exception {
		org.mockito.Mockito.doThrow(new TooManyRequestsException("Too many accounts", Duration.ofMinutes(15)))
				.when(signUpLimiter).check(any());

		mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"Anna\", \"email\": \"anna@example.com\", \"password\": \"Secret12\"}"))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string("Retry-After", "900"));
		verify(accountService, never()).register(any());
	}

	@Test
	void theRightCodeReturnsANewTokenThatSaysTheEmailIsVerified() throws Exception {
		var verified = account("anna@example.com", "$2a$10$hash");
		verified.setEmailVerified(true);
		when(verificationService.verify("anna@example.com", "482913")).thenReturn(verified);
		when(tokenService.issue(verified)).thenReturn("verified.jwt.token");
		when(tokenService.lifetimeSeconds()).thenReturn(3600L);

		mockMvc.perform(post("/accounts/verify").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"anna@example.com\", \"code\": \"482913\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("verified.jwt.token"));
	}

	@Test
	void aCodeMustBeSixDigits() throws Exception {
		mockMvc.perform(post("/accounts/verify").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"anna@example.com\", \"code\": \"48a9\"}"))
				.andExpect(status().isBadRequest());
		verify(verificationService, never()).verify(any(), any());
	}

	@Test
	void askingForANewCodeIsAccepted() throws Exception {
		mockMvc.perform(post("/accounts/verify/resend").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"anna@example.com\"}"))
				.andExpect(status().isAccepted());
		verify(verificationService).resend("anna@example.com");
	}

	@Test
	void signingInToAnUnverifiedAccountSendsACodeWhenNeeded() throws Exception {
		var unverified = account("anna@example.com", "$2a$10$hash");
		when(accountService.authenticate(any())).thenReturn(unverified);
		when(tokenService.issue(any())).thenReturn("signed.jwt.token");

		mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"anna@example.com\", \"password\": \"Secret12\"}"))
				.andExpect(status().isOk());
		verify(verificationService).sendIfNoValidCode(eq(unverified));
	}

	@Test
	void signingInToAVerifiedAccountSendsNothing() throws Exception {
		var verified = account("anna@example.com", "$2a$10$hash");
		verified.setEmailVerified(true);
		when(accountService.authenticate(any())).thenReturn(verified);
		when(tokenService.issue(any())).thenReturn("signed.jwt.token");

		mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"anna@example.com\", \"password\": \"Secret12\"}"))
				.andExpect(status().isOk());
		verify(verificationService, never()).sendIfNoValidCode(any());
	}

	@Test
	void anAccountReadsItsOwnDataWithItsOwnToken() throws Exception {
		var anna = account("anna@example.com", "$2a$10$hash");
		when(ownTokens.accountId("Bearer own.token")).thenReturn(anna.getId());
		when(accountService.find(anna.getId())).thenReturn(anna);

		mockMvc.perform(get("/accounts/me").header("Authorization", "Bearer own.token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("anna@example.com"))
				.andExpect(jsonPath("$.emailVerified").value(false))
				.andExpect(jsonPath("$.createdAt").exists())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void anAccountDeletesItselfWithItsOwnToken() throws Exception {
		var anna = account("anna@example.com", "$2a$10$hash");
		when(ownTokens.accountId("Bearer own.token")).thenReturn(anna.getId());

		mockMvc.perform(delete("/accounts/me").header("Authorization", "Bearer own.token"))
				.andExpect(status().isNoContent());
		verify(accountService).delete(anna.getId());
	}

	@Test
	void withoutAValidTokenNothingIsReadOrDeleted() throws Exception {
		when(ownTokens.accountId(any())).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again"));

		mockMvc.perform(get("/accounts/me")).andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/accounts/me").header("Authorization", "Bearer forged")).andExpect(status().isUnauthorized());
		verify(accountService, never()).delete(any());
	}
}
