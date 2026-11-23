package nl.carebridge.messaging.appointment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
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

import nl.carebridge.messaging.config.AppConfig;
import nl.carebridge.messaging.security.CurrentUserResolver;
import nl.carebridge.messaging.security.SecurityConfig;

/** User story nr 10 AC2 AC4 AC6 AC7 TAC5: who may call which booking endpoint. */
@WebMvcTest(AppointmentController.class)
@Import({SecurityConfig.class, CurrentUserResolver.class, AppConfig.class, AppointmentConfig.class})
@TestPropertySource(properties = {
		"carebridge.security.gp-emails=gp@example.com",
		"carebridge.security.assistant-emails=assistant@example.com"})
@Tag("api")
class AppointmentControllerTest {

	private static final UUID QUESTION = UUID.fromString("3f1c2a4e-0000-4000-8000-000000000001");
	private static final UUID REPLY = UUID.fromString("3f1c2a4e-0000-4000-8000-000000000002");
	private static final Instant TUESDAY_TEN = Instant.parse("2026-12-01T09:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CurrentUserResolver currentUserResolver;

	@MockitoBean
	private AppointmentService service;

	private final JwtRequestPostProcessor anna = user("account-anna", "Anna", "anna@example.com");
	private final JwtRequestPostProcessor assistant = user("google-sam", "Sam", "assistant@example.com");

	private static Appointment annasAppointment() {
		return new Appointment(REPLY, QUESTION, "hash", "Anna de Vries", TUESDAY_TEN, Instant.parse("2026-11-30T14:00:00Z"));
	}

	@Test
	void aPatientSeesTheFreeTimesWithTheDayAndTimeOfThePractice() throws Exception {
		when(service.free()).thenReturn(List.of(TUESDAY_TEN));

		mockMvc.perform(get("/appointments/free").with(anna))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].start").value("2026-12-01T09:00:00Z"))
				.andExpect(jsonPath("$[0].day").value("Tuesday 1 December 2026"))
				.andExpect(jsonPath("$[0].time").value("10:00"));
	}

	@Test
	void aPatientBooksATimeForTheirInvitation() throws Exception {
		when(service.book(eq(REPLY), eq(TUESDAY_TEN), any(), eq("Bearer token"))).thenReturn(annasAppointment());

		mockMvc.perform(post("/replies/" + REPLY + "/appointment").with(anna)
						.contentType(MediaType.APPLICATION_JSON).content("{\"start\": \"2026-12-01T09:00:00Z\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.questionId").value(QUESTION.toString()))
				.andExpect(jsonPath("$.day").value("Tuesday 1 December 2026"))
				.andExpect(jsonPath("$.time").value("10:00"));
	}

	@Test
	void aBookingWithoutATimeIsRefused() throws Exception {
		mockMvc.perform(post("/replies/" + REPLY + "/appointment").with(anna).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
		verify(service, never()).book(any(), any(), any(), any());
	}

	@Test
	void aTimeJustTakenGivesConflictWithAClearMessage() throws Exception {
		when(service.book(any(), any(), any(), any()))
				.thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "This time was just taken. Please choose another."));

		mockMvc.perform(post("/replies/" + REPLY + "/appointment").with(anna)
						.contentType(MediaType.APPLICATION_JSON).content("{\"start\": \"2026-12-01T09:00:00Z\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("This time was just taken. Please choose another."));
	}

	@Test
	void staffCannotBookOrCancelForAPatient() throws Exception {
		mockMvc.perform(post("/replies/" + REPLY + "/appointment").with(assistant)
						.contentType(MediaType.APPLICATION_JSON).content("{\"start\": \"2026-12-01T09:00:00Z\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(delete("/appointments/" + UUID.randomUUID()).with(assistant)).andExpect(status().isForbidden());
		verify(service, never()).book(any(), any(), any(), any());
		verify(service, never()).cancel(any(), any(), any());
	}

	@Test
	void aPatientCancelsTheirAppointment() throws Exception {
		UUID id = UUID.randomUUID();

		mockMvc.perform(delete("/appointments/" + id).with(anna)).andExpect(status().isNoContent());

		verify(service).cancel(eq(id), any(), eq("Bearer token"));
	}

	@Test
	void cancellingSomeoneElsesAppointmentIsNotFound() throws Exception {
		UUID id = UUID.randomUUID();
		doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)).when(service).cancel(eq(id), any(), any());

		mockMvc.perform(delete("/appointments/" + id).with(anna)).andExpect(status().isNotFound());
	}

	@Test
	void staffSeeTheAppointmentsOfADay() throws Exception {
		when(service.onDay(LocalDate.of(2026, 12, 1))).thenReturn(List.of(annasAppointment()));

		for (JwtRequestPostProcessor staff : List.of(assistant, user("google-gp", "Dr. Jansen", "gp@example.com"))) {
			mockMvc.perform(get("/appointments").param("day", "2026-12-01").with(staff))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$[0].time").value("10:00"))
					.andExpect(jsonPath("$[0].patientName").value("Anna de Vries"))
					.andExpect(jsonPath("$[0].questionId").value(QUESTION.toString()));
		}
	}

	@Test
	void patientsCannotSeeThePracticesDayList() throws Exception {
		mockMvc.perform(get("/appointments").param("day", "2026-12-01").with(anna)).andExpect(status().isForbidden());
		verify(service, never()).onDay(any());
	}

	@Test
	void staffDoNotNeedTheFreeTimesAndAnUnreadableDayIsRefused() throws Exception {
		mockMvc.perform(get("/appointments/free").with(assistant)).andExpect(status().isForbidden());
		mockMvc.perform(get("/appointments").param("day", "1 December").with(assistant)).andExpect(status().isBadRequest());
	}

	@Test
	void withoutSignInNothingIsShown() throws Exception {
		mockMvc.perform(get("/appointments/free")).andExpect(status().isUnauthorized());
	}

	private JwtRequestPostProcessor user(String id, String name, String email) {
		return jwt().jwt(token -> token.tokenValue("token").subject(id).claim("name", name).claim("email", email).claim("email_verified", true))
				.authorities(token -> currentUserResolverAuthorities(token));
	}

	private java.util.Collection<org.springframework.security.core.GrantedAuthority> currentUserResolverAuthorities(org.springframework.security.oauth2.jwt.Jwt token) {
		return currentUserResolver.authorities(token);
	}
}
