package nl.carebridge.intake.controller;

import static org.mockito.Mockito.when;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import nl.carebridge.intake.model.Intake;
import nl.carebridge.intake.service.IntakeService;

@WebMvcTest(IntakeController.class)
class IntakeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IntakeService intakeService;

	@Test
	void submitReturnsCreatedIntakeWithLocation() throws Exception {
		Intake intake = sampleIntake("Back pain since Monday");
		when(intakeService.submit("Back pain since Monday")).thenReturn(intake);

		mockMvc.perform(post("/intakes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"description\": \"Back pain since Monday\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/intakes/" + intake.getId()))
				.andExpect(jsonPath("$.id").value(intake.getId().toString()))
				.andExpect(jsonPath("$.description").value("Back pain since Monday"))
				.andExpect(jsonPath("$.status").value("SUBMITTED"));
	}

	@Test
	void submitWithInvalidDescriptionReturnsBadRequest() throws Exception {
		when(intakeService.submit("  "))
				.thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description is required"));

		mockMvc.perform(post("/intakes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"description\": \"  \"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void findAllReturnsIntakes() throws Exception {
		when(intakeService.findAll()).thenReturn(List.of(sampleIntake("First"), sampleIntake("Second")));

		mockMvc.perform(get("/intakes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].description").value("First"));
	}

	@Test
	void findByIdOfUnknownIntakeReturnsNotFound() throws Exception {
		UUID id = UUID.randomUUID();
		when(intakeService.findById(id))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Intake " + id + " not found"));

		mockMvc.perform(get("/intakes/" + id))
				.andExpect(status().isNotFound());
	}

	private static Intake sampleIntake(String description) {
		Intake intake = new Intake();
		intake.setId(UUID.randomUUID());
		intake.setDescription(description);
		intake.setStatus(Intake.Status.SUBMITTED);
		intake.setSubmittedAt(Instant.parse("2026-10-07T09:00:00Z"));
		return intake;
	}
}
