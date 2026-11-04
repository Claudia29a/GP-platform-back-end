package nl.carebridge.intake.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import nl.carebridge.intake.security.CurrentUser;
import nl.carebridge.intake.security.CurrentUserResolver;
import nl.carebridge.intake.security.Role;
import nl.carebridge.intake.security.SecurityConfig;
import nl.carebridge.intake.service.PrivacyConsentService;

@WebMvcTest({PrivacyConsentController.class, UserController.class})
@Import({SecurityConfig.class, CurrentUserResolver.class})
@TestPropertySource(properties = "carebridge.security.gp-emails=gp@example.com")
@Tag("api")
class PrivacyConsentControllerTest {

	private static final CurrentUser PATIENT = new CurrentUser("google-anna", "Anna", "anna@example.com", Role.PATIENT);

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PrivacyConsentService privacyConsentService;

	@Test
	void needsAToken() throws Exception {
		mockMvc.perform(post("/privacy-consent")).andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/privacy-consent")).andExpect(status().isUnauthorized());
	}

	@Test
	void aPatientCanAllowAndWithdraw() throws Exception {
		mockMvc.perform(post("/privacy-consent").with(patient())).andExpect(status().isNoContent());
		verify(privacyConsentService).accept(PATIENT);

		mockMvc.perform(delete("/privacy-consent").with(patient())).andExpect(status().isNoContent());
		verify(privacyConsentService).withdraw(PATIENT);
	}

	@Test
	void staffHaveNothingToAllow() throws Exception {
		mockMvc.perform(post("/privacy-consent").with(jwt()
						.jwt(token -> token.subject("google-gp").claim("email", "gp@example.com").claim("name", "Dr. Jansen"))
						.authorities(new SimpleGrantedAuthority("ROLE_GP"))))
				.andExpect(status().isForbidden());
		verify(privacyConsentService, never()).accept(any());
	}

	@Test
	void theFrontEndLearnsWhetherToAsk() throws Exception {
		when(privacyConsentService.isNeeded(PATIENT)).thenReturn(true);

		mockMvc.perform(get("/me").with(patient()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("PATIENT"))
				.andExpect(jsonPath("$.privacyConsentNeeded").value(true));
	}

	private static JwtRequestPostProcessor patient() {
		return jwt().jwt(token -> token.subject("google-anna").claim("email", "anna@example.com").claim("name", "Anna"))
				.authorities(new SimpleGrantedAuthority("ROLE_PATIENT"));
	}
}
