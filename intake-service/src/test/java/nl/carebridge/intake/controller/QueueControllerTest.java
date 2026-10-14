package nl.carebridge.intake.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import nl.carebridge.intake.model.ComplaintArea;
import nl.carebridge.intake.model.ComplaintDuration;
import nl.carebridge.intake.model.Intake;
import nl.carebridge.intake.security.CurrentUserResolver;
import nl.carebridge.intake.security.SecurityConfig;
import nl.carebridge.intake.service.QueueService;

@WebMvcTest(QueueController.class)
@Import({SecurityConfig.class, CurrentUserResolver.class})
@TestPropertySource(properties = {
		"carebridge.security.gp-emails=gp@example.com",
		"carebridge.security.assistant-emails=assistant@example.com"})
class QueueControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CurrentUserResolver currentUserResolver;

	@MockitoBean
	private QueueService queueService;

	@Test
	void assistantAndGpSeeTheQueueWithScores() throws Exception {
		Intake intake = queued(1, 72);
		when(queueService.currentQueue()).thenReturn(List.of(intake));

		for (String email : List.of("assistant@example.com", "gp@example.com")) {
			mockMvc.perform(get("/queue").with(signedIn(email)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$[0].id").value(intake.getId().toString()))
					.andExpect(jsonPath("$[0].position").value(1))
					.andExpect(jsonPath("$[0].severityScore").value(72))
					.andExpect(jsonPath("$[0].patientName").value("Anna de Vries"));
		}
	}

	@Test
	void patientsCannotSeeOrChangeTheQueue() throws Exception {
		mockMvc.perform(get("/queue").with(signedIn("anna@example.com"))).andExpect(status().isForbidden());
		mockMvc.perform(put("/queue/order").with(signedIn("anna@example.com"))
						.contentType(MediaType.APPLICATION_JSON).content("{\"intakeIds\": []}"))
				.andExpect(status().isForbidden());

		verify(queueService, never()).currentQueue();
		verify(queueService, never()).reorder(any());
	}

	@Test
	void queueNeedsSignIn() throws Exception {
		mockMvc.perform(get("/queue")).andExpect(status().isUnauthorized());
	}

	@Test
	void staffSaveANewOrder() throws Exception {
		Intake first = queued(1, 30);
		Intake second = queued(2, 90);
		when(queueService.reorder(List.of(first.getId(), second.getId()))).thenReturn(List.of(first, second));

		mockMvc.perform(put("/queue/order").with(signedIn("assistant@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"intakeIds\": [\"" + first.getId() + "\", \"" + second.getId() + "\"]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(first.getId().toString()))
				.andExpect(jsonPath("$[1].position").value(2));
	}

	@Test
	void outdatedOrderAnswersConflict() throws Exception {
		when(queueService.reorder(any()))
				.thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "The queue has changed"));

		mockMvc.perform(put("/queue/order").with(signedIn("gp@example.com"))
						.contentType(MediaType.APPLICATION_JSON).content("{\"intakeIds\": []}"))
				.andExpect(status().isConflict());
	}

	@Test
	void orderWithoutIdsIsABadRequest() throws Exception {
		mockMvc.perform(put("/queue/order").with(signedIn("gp@example.com"))
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}

	private JwtRequestPostProcessor signedIn(String email) {
		return jwt().jwt(token -> token.subject("google-" + email).claim("name", email).claim("email", email))
				.authorities(currentUserResolver::authorities);
	}

	private static Intake queued(int position, int score) {
		Intake intake = new Intake();
		intake.setId(UUID.randomUUID());
		intake.setPatientName("Anna de Vries");
		intake.setComplaintArea(ComplaintArea.SKIN);
		intake.setDuration(ComplaintDuration.ONE_TO_FOUR_WEEKS);
		intake.setSeverity(4);
		intake.setSeverityScore(score);
		intake.setQueuePosition(position);
		intake.setDescription("Itchy rash");
		intake.setStatus(Intake.Status.SUBMITTED);
		intake.setSubmittedAt(Instant.parse("2026-10-14T09:00:00Z"));
		return intake;
	}
}
