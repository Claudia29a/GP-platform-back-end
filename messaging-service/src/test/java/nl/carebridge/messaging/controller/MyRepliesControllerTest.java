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
import nl.carebridge.messaging.service.ReplyService;

@WebMvcTest(MyRepliesController.class)
@Import({SecurityConfig.class, CurrentUserResolver.class})
@TestPropertySource(properties = "carebridge.security.gp-emails=gp@example.com")
@Tag("api")
class MyRepliesControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CurrentUserResolver currentUserResolver;

	@MockitoBean
	private ReplyService replyService;

	@Test
	void aPatientExportsTheRepliesToTheirQuestions() throws Exception {
		UUID question = UUID.randomUUID();
		when(replyService.myReplies("Bearer token")).thenReturn(List.of(
				new Reply(question, "hash", "Dr. Jansen", "Use the cream.", Instant.parse("2026-11-17T10:00:00Z"), Instant.parse("2026-11-01T09:00:00Z"))));

		mockMvc.perform(get("/me/replies").with(user("google-anna", "anna@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].text").value("Use the cream."))
				.andExpect(jsonPath("$[0].questionId").value(question.toString()));
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
