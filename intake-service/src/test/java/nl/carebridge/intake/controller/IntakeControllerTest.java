package nl.carebridge.intake.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

import nl.carebridge.intake.TestIntakes;
import nl.carebridge.intake.model.ComplaintArea;
import nl.carebridge.intake.model.ComplaintDuration;
import nl.carebridge.intake.model.Intake;
import nl.carebridge.intake.security.CurrentUser;
import nl.carebridge.intake.security.CurrentUserResolver;
import nl.carebridge.intake.security.Role;
import nl.carebridge.intake.security.SecurityConfig;
import nl.carebridge.intake.service.IntakeService;

@WebMvcTest({IntakeController.class, UserController.class})
@Import({SecurityConfig.class, CurrentUserResolver.class})
@TestPropertySource(properties = {
		"carebridge.security.gp-emails=gp@example.com",
		"carebridge.security.assistant-emails=assistant@example.com"})
class IntakeControllerTest {

	private static final CurrentUser PATIENT = new CurrentUser("google-anna", "Anna de Vries", "anna@example.com", Role.PATIENT);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CurrentUserResolver currentUserResolver;

	@MockitoBean
	private IntakeService intakeService;

	@Test
	void requestsWithoutTokenAreUnauthorized() throws Exception {
		mockMvc.perform(get("/intakes")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/me")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/intakes").contentType(MediaType.APPLICATION_JSON).content("{\"description\": \"Hi\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void patientSubmitsQuestionAsThemselves() throws Exception {
		Intake intake = sampleIntake("Itchy rash on my left arm");
		when(intakeService.submit(TestIntakes.validRequest(), PATIENT)).thenReturn(intake);

		mockMvc.perform(post("/intakes").with(signedIn("google-anna", "Anna de Vries", "anna@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(TestIntakes.VALID_JSON))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/intakes/" + intake.getId()))
				.andExpect(jsonPath("$.description").value("Itchy rash on my left arm"))
				.andExpect(jsonPath("$.complaintArea").value("SKIN"))
				.andExpect(jsonPath("$.patientName").value("Anna de Vries"))
				.andExpect(jsonPath("$.severityScore").doesNotExist())
				.andExpect(jsonPath("$.queuePosition").doesNotExist());
	}

	@Test
	void staffCannotSubmitQuestions() throws Exception {
		mockMvc.perform(post("/intakes").with(signedIn("google-gp", "Dr. Jansen", "gp@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(TestIntakes.VALID_JSON))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/intakes").with(signedIn("google-sam", "Sam", "assistant@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(TestIntakes.VALID_JSON))
				.andExpect(status().isForbidden());

		verify(intakeService, never()).submit(any(), any());
	}

	@Test
	void incompleteAnswersAreRejectedBeforeAnythingIsStored() throws Exception {
		mockMvc.perform(post("/intakes").with(signedIn("google-anna", "Anna de Vries", "anna@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(TestIntakes.VALID_JSON.replace("\"severity\": 4", "\"severity\": 11")))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/intakes").with(signedIn("google-anna", "Anna de Vries", "anna@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(TestIntakes.VALID_JSON.replace("\"triedTreatment\": true,\n  \"treatmentTried\": \"Moisturising cream\",", "\"triedTreatment\": true,")))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/intakes").with(signedIn("google-anna", "Anna de Vries", "anna@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(TestIntakes.VALID_JSON.replace("\"SKIN\"", "\"ELBOW\"")))
				.andExpect(status().isBadRequest());

		verify(intakeService, never()).submit(any(), any());
	}

	@Test
	void listShowsWhatTheSignedInUserMaySee() throws Exception {
		when(intakeService.findVisibleTo(PATIENT)).thenReturn(List.of(sampleIntake("Mine")));

		mockMvc.perform(get("/intakes").with(signedIn("google-anna", "Anna de Vries", "anna@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].description").value("Mine"));
	}

	@Test
	void unknownQuestionReturnsNotFound() throws Exception {
		UUID id = UUID.randomUUID();
		when(intakeService.findById(eq(id), any()))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Intake " + id + " not found"));

		mockMvc.perform(get("/intakes/" + id).with(signedIn("google-anna", "Anna de Vries", "anna@example.com")))
				.andExpect(status().isNotFound());
	}

	@Test
	void meReturnsNameAndRole() throws Exception {
		mockMvc.perform(get("/me").with(signedIn("google-gp", "Dr. Jansen", "GP@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Dr. Jansen"))
				.andExpect(jsonPath("$.role").value("GP"));
		mockMvc.perform(get("/me").with(signedIn("google-sam", "Sam", "assistant@example.com")))
				.andExpect(jsonPath("$.role").value("GP_ASSISTANT"));
		mockMvc.perform(get("/me").with(signedIn("google-anna", "Anna de Vries", "anna@example.com")))
				.andExpect(jsonPath("$.role").value("PATIENT"));
	}

	/** A signed-in user whose role is worked out exactly as in production. */
	private JwtRequestPostProcessor signedIn(String id, String name, String email) {
		return jwt().jwt(token -> token.subject(id).claim("name", name).claim("email", email))
				.authorities(currentUserResolver::authorities);
	}

	private static Intake sampleIntake(String description) {
		Intake intake = new Intake();
		intake.setId(UUID.randomUUID());
		intake.setPatientId("google-anna");
		intake.setPatientName("Anna de Vries");
		intake.setPatientEmail("anna@example.com");
		intake.setComplaintArea(ComplaintArea.SKIN);
		intake.setDuration(ComplaintDuration.ONE_TO_FOUR_WEEKS);
		intake.setSeverity(4);
		intake.setSeverityScore(47);
		intake.setQueuePosition(1);
		intake.setDescription(description);
		intake.setStatus(Intake.Status.SUBMITTED);
		intake.setSubmittedAt(Instant.parse("2026-10-14T09:00:00Z"));
		return intake;
	}
}
