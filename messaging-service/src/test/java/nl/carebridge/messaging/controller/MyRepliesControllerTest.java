package nl.carebridge.messaging.controller;

import static org.mockito.ArgumentMatchers.any;
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

import nl.carebridge.messaging.model.Reply;
import nl.carebridge.messaging.security.CurrentUserResolver;
import nl.carebridge.messaging.security.SecurityConfig;
import nl.carebridge.messaging.appointment.AppointmentConfig;
import nl.carebridge.messaging.appointment.AppointmentRepository;
import nl.carebridge.messaging.config.AppConfig;
import nl.carebridge.messaging.service.AppointmentLink;
import nl.carebridge.messaging.service.ReplyViews;
import nl.carebridge.messaging.service.ReplyService;

@WebMvcTest(MyRepliesController.class)
@Import({SecurityConfig.class, CurrentUserResolver.class, AppointmentLink.class, ReplyViews.class, AppConfig.class, AppointmentConfig.class})
@TestPropertySource(properties = "carebridge.security.gp-emails=gp@example.com")
@Tag("api")
class MyRepliesControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CurrentUserResolver currentUserResolver;

	@MockitoBean
	private ReplyService replyService;

	@MockitoBean
	private AppointmentRepository appointmentRepository;

	@Test
	void aPatientExportsTheRepliesToTheirQuestions() throws Exception {
		UUID question = UUID.randomUUID();
		when(replyService.myReplies("Bearer token")).thenReturn(List.of(
				new Reply(question, "hash", "Dr. Jansen", "Use the cream.", true, Instant.parse("2026-11-17T10:00:00Z"), Instant.parse("2026-11-01T09:00:00Z"))));

		mockMvc.perform(get("/me/replies").with(user("google-anna", "anna@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].text").value("Use the cream."))
				.andExpect(jsonPath("$[0].questionId").value(question.toString()))
				// User story nr 9 TAC1: My data shows that an appointment was asked.
				.andExpect(jsonPath("$[0].appointment").value(true));
	}

	@Test
	void aPatientDeletesTheRepliesToTheirQuestions() throws Exception {
		mockMvc.perform(delete("/me/replies").with(user("google-anna", "anna@example.com"))).andExpect(status().isNoContent());

		verify(replyService).deleteMyReplies("Bearer token");
	}

	@Test
	void staffHaveNoRepliesOfTheirOwnToDelete() throws Exception {
		mockMvc.perform(delete("/me/replies").with(user("google-gp", "gp@example.com"))).andExpect(status().isForbidden());

		verify(replyService, never()).deleteMyReplies(any());
	}

	private JwtRequestPostProcessor user(String id, String email) {
		return jwt().jwt(token -> token.subject(id).claim("email", email).claim("email_verified", true))
				.authorities(currentUserResolver::authorities);
	}
}
