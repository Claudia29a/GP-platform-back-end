package nl.carebridge.messaging.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import nl.carebridge.messaging.dto.ReplyResponse;
import nl.carebridge.messaging.service.ReplyService;

/** The patient's own replies, for My data: to take them along (GDPR Art. 15, 20) or delete them (Art. 17). */
@RestController
@RequestMapping("/me/replies")
public class MyRepliesController {

	private final ReplyService replyService;

	public MyRepliesController(ReplyService replyService) {
		this.replyService = replyService;
	}

	@GetMapping
	public List<ReplyResponse> export(@AuthenticationPrincipal Jwt token) {
		return replyService.myReplies("Bearer " + token.getTokenValue()).stream().map(ReplyResponse::from).toList();
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Jwt token) {
		replyService.deleteMyReplies("Bearer " + token.getTokenValue());
	}
}
