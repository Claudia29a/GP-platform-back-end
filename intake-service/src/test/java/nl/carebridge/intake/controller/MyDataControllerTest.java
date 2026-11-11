package nl.carebridge.intake.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import nl.carebridge.intake.dto.AccessLogResponse;
import nl.carebridge.intake.dto.MyDataResponse;
import nl.carebridge.intake.security.CurrentUser;
import nl.carebridge.intake.security.CurrentUserResolver;
import nl.carebridge.intake.security.Role;
import nl.carebridge.intake.security.SecurityConfig;
import nl.carebridge.intake.service.AccessLogService;
import nl.carebridge.intake.service.IntakeService;
import nl.carebridge.intake.service.MyDataService;

@WebMvcTest({MyDataController.class, IntakeController.class})
@Import({SecurityConfig.class, CurrentUserResolver.class})
@TestPropertySource(properties = {
		"carebridge.security.gp-emails=gp@example.com",
		"carebridge.security.assistant-emails=assistant@example.com"})
@Tag("api")
class MyDataControllerTest {

	private static final CurrentUser PATIENT = new CurrentUser("google-anna", "Anna de Vries", "anna@example.com", Role.PATIENT);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CurrentUserResolver currentUserResolver;

	@MockitoBean
	private MyDataService myDataService;

	@MockitoBean
	private AccessLogService accessLogService;

	@MockitoBean
	private IntakeService intakeService;

	@Test
	void aPatientGetsTheirOwnDataFromTheirToken() throws Exception {
		when(myDataService.export(PATIENT)).thenReturn(new MyDataResponse(
				new MyDataResponse.UserData("google-anna", "Anna de Vries", "anna@example.com"), null, List.of()));

		mockMvc.perform(get("/me/data").with(user("google-anna", "Anna de Vries", "anna@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.email").value("anna@example.com"));
	}

	@Test
	void aPatientCanDeleteTheirData() throws Exception {
		mockMvc.perform(delete("/me/data").with(user("google-anna", "Anna de Vries", "anna@example.com")))
				.andExpect(status().isNoContent());

		verify(myDataService).delete(PATIENT);
	}

	@Test
	void staffHaveNoPatientDataToDelete() throws Exception {
		mockMvc.perform(delete("/me/data").with(user("google-gp", "Dr. Jansen", "gp@example.com")))
				.andExpect(status().isForbidden());

		verify(myDataService, never()).delete(any());
	}

	@Test
	void onlyTheGpCanReadWhoOpenedAQuestion() throws Exception {
		UUID id = UUID.randomUUID();
		when(accessLogService.entriesFor(id))
				.thenReturn(List.of(new AccessLogResponse("Sam Bakker", Role.GP_ASSISTANT, Instant.parse("2026-11-11T10:00:00Z"))));

		mockMvc.perform(get("/intakes/" + id + "/access-log").with(user("google-gp", "Dr. Jansen", "gp@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].staffName").value("Sam Bakker"))
				.andExpect(jsonPath("$[0].role").value("GP_ASSISTANT"));
		mockMvc.perform(get("/intakes/" + id + "/access-log").with(user("google-sam", "Sam", "assistant@example.com")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/intakes/" + id + "/access-log").with(user("google-anna", "Anna", "anna@example.com")))
				.andExpect(status().isForbidden());

		verify(accessLogService, never()).record(eq(id), any());
	}

	private JwtRequestPostProcessor user(String id, String name, String email) {
		return jwt().jwt(token -> token.subject(id).claim("name", name).claim("email", email).claim("email_verified", true))
				.authorities(currentUserResolver::authorities);
	}
}
