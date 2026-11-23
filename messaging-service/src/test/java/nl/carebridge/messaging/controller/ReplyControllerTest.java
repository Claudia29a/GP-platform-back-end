package nl.carebridge.messaging.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import nl.carebridge.messaging.model.Reply;
import nl.carebridge.messaging.security.CurrentUser;
import nl.carebridge.messaging.security.CurrentUserResolver;
import nl.carebridge.messaging.security.Role;
import nl.carebridge.messaging.security.SecurityConfig;
import nl.carebridge.messaging.service.AppointmentLink;
import nl.carebridge.messaging.service.ReplyService;

@WebMvcTest(ReplyController.class)
@Import({SecurityConfig.class, CurrentUserResolver.class, AppointmentLink.class})
@TestPropertySource(properties = {
		"carebridge.security.gp-emails=gp@example.com",
		"carebridge.security.assistant-emails=assistant@example.com"})
@Tag("api")
class ReplyControllerTest {

	private static final UUID QUESTION = UUID.fromString("3f1c2a4e-0000-4000-8000-000000000001");
	private static final CurrentUser GP = new CurrentUser("google-gp", "Dr. Jansen", "gp@example.com", Role.GP);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CurrentUserResolver currentUserResolver;

	@MockitoBean
	private ReplyService replyService;

	@Test
	void theGpRepliesAndGetsTheReplyBack() throws Exception {
		when(replyService.reply(eq(QUESTION), eq("Use the cream."), eq(false), eq(GP), any()))
				.thenReturn(new Reply(QUESTION, "hash", "Dr. Jansen", "Use the cream.", Instant.parse("2026-11-17T10:00:00Z"), Instant.parse("2026-11-01T09:00:00Z")));

		mockMvc.perform(post("/questions/" + QUESTION + "/replies").with(user("google-gp", "Dr. Jansen", "gp@example.com"))
						.contentType(MediaType.APPLICATION_JSON).content("{\"text\": \"Use the cream.\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.text").value("Use the cream."))
				.andExpect(jsonPath("$.authorName").value("Dr. Jansen"))
				.andExpect(jsonPath("$.authorHash").doesNotExist())
				.andExpect(jsonPath("$.appointment").value(false))
				.andExpect(jsonPath("$.appointmentUrl").doesNotExist());
	}

	/** User story nr 9 AC1 TAC1 TAC3: the GP asks for an appointment; the link always comes from the settings, never from the request. */
	@Test
	void theGpInvitesThePatientAndTheLinkComesFromTheSettings() throws Exception {
		when(replyService.reply(eq(QUESTION), eq("Please come in."), eq(true), eq(GP), any()))
				.thenReturn(new Reply(QUESTION, "hash", "Dr. Jansen", "Please come in.", true, Instant.parse("2026-11-17T10:00:00Z"), Instant.parse("2026-11-01T09:00:00Z")));

		mockMvc.perform(post("/questions/" + QUESTION + "/replies").with(user("google-gp", "Dr. Jansen", "gp@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"text\": \"Please come in.\", \"appointment\": true, \"appointmentUrl\": \"https://evil.example\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.appointment").value(true))
				.andExpect(jsonPath("$.appointmentUrl").value("https://carebridge.test/appointment"));
	}

	/** User story nr 9 AC5: only the GP can send an invitation, just like a reply. */
	@Test
	void onlyTheGpMayInviteThePatient() throws Exception {
		mockMvc.perform(post("/questions/" + QUESTION + "/replies").with(user("google-sam", "Sam", "assistant@example.com"))
						.contentType(MediaType.APPLICATION_JSON).content("{\"text\": \"Come in\", \"appointment\": true}"))
				.andExpect(status().isForbidden());
		verify(replyService, never()).reply(any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any(), any());
	}

	@Test
	void onlyTheGpMayReply() throws Exception {
		for (JwtRequestPostProcessor someone : List.of(user("google-sam", "Sam", "assistant@example.com"), user("google-anna", "Anna", "anna@example.com"))) {
			mockMvc.perform(post("/questions/" + QUESTION + "/replies").with(someone)
							.contentType(MediaType.APPLICATION_JSON).content("{\"text\": \"Hello\"}"))
					.andExpect(status().isForbidden());
		}
		verify(replyService, never()).reply(any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any(), any());
	}

	@Test
	void anEmptyOrTooLongReplyIsRefused() throws Exception {
		mockMvc.perform(post("/questions/" + QUESTION + "/replies").with(user("google-gp", "Dr. Jansen", "gp@example.com"))
						.contentType(MediaType.APPLICATION_JSON).content("{\"text\": \"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.text").exists());
		mockMvc.perform(post("/questions/" + QUESTION + "/replies").with(user("google-gp", "Dr. Jansen", "gp@example.com"))
						.contentType(MediaType.APPLICATION_JSON).content("{\"text\": \"" + "x".repeat(2001) + "\"}"))
				.andExpect(status().isBadRequest());
		verify(replyService, never()).reply(any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any(), any());
	}

	@Test
	void anyoneWhoMaySeeTheQuestionReadsItsReplies() throws Exception {
		when(replyService.replies(eq(QUESTION), any(), eq("Bearer token")))
				.thenReturn(List.of(new Reply(QUESTION, "hash", "Dr. Jansen", "Use the cream.", Instant.parse("2026-11-17T10:00:00Z"), Instant.parse("2026-11-01T09:00:00Z"))));

		mockMvc.perform(get("/questions/" + QUESTION + "/replies").with(user("google-anna", "Anna", "anna@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].text").value("Use the cream."));
	}

	@Test
	void withoutSignInNothingIsShown() throws Exception {
		mockMvc.perform(get("/questions/" + QUESTION + "/replies")).andExpect(status().isUnauthorized());
	}

	private JwtRequestPostProcessor user(String id, String name, String email) {
		return jwt().jwt(token -> token.subject(id).claim("name", name).claim("email", email).claim("email_verified", true))
				.authorities(currentUserResolver::authorities);
	}
}
