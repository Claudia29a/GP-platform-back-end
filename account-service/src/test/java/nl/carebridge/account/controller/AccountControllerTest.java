package nl.carebridge.account.controller;

import static nl.carebridge.account.TestAccounts.account;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import nl.carebridge.account.service.TokenService;

@WebMvcTest({AccountController.class, JwksController.class})
class AccountControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AccountService accountService;

	@MockitoBean
	private TokenService tokenService;

	@MockitoBean
	private SigningKey signingKey;

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
}
