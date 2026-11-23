package nl.carebridge.messaging.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import nl.carebridge.messaging.dto.ReplyRequest;
import nl.carebridge.messaging.dto.ReplyResponse;
import nl.carebridge.messaging.security.CurrentUserResolver;
import nl.carebridge.messaging.service.ReplyViews;
import nl.carebridge.messaging.service.ReplyService;

@RestController
@RequestMapping("/questions/{questionId}/replies")
public class ReplyController {

	private final ReplyService replyService;
	private final CurrentUserResolver currentUserResolver;
	private final ReplyViews views;

	public ReplyController(ReplyService replyService, CurrentUserResolver currentUserResolver, ReplyViews views) {
		this.replyService = replyService;
		this.currentUserResolver = currentUserResolver;
		this.views = views;
	}

	/** Only the GP may reply (SecurityConfig). The booking link never comes from the request (User story nr 9 TAC3). */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ReplyResponse reply(@PathVariable UUID questionId, @Valid @RequestBody ReplyRequest request,
			@AuthenticationPrincipal Jwt token) {
		return views.of(replyService.reply(questionId, request.text(), request.asksForAppointment(),
				currentUserResolver.resolve(token), bearer(token)));
	}

	/** Everyone who may see the question, as the Intake Service decides, may read its replies. */
	@GetMapping
	public List<ReplyResponse> replies(@PathVariable UUID questionId, @AuthenticationPrincipal Jwt token) {
		return views.of(replyService.replies(questionId, currentUserResolver.resolve(token), bearer(token)));
	}

	/** The user's own, already checked sign-in, passed on to the Intake Service. */
	private static String bearer(Jwt token) {
		return "Bearer " + token.getTokenValue();
	}
}
