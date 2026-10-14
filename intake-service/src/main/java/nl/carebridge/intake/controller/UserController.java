package nl.carebridge.intake.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import nl.carebridge.intake.security.CurrentUser;
import nl.carebridge.intake.security.CurrentUserResolver;

@RestController
public class UserController {

	private final CurrentUserResolver currentUserResolver;

	public UserController(CurrentUserResolver currentUserResolver) {
		this.currentUserResolver = currentUserResolver;
	}

	/** Who is signed in and with which role, so the front-end knows which pages to show. */
	@GetMapping("/me")
	public CurrentUser me(@AuthenticationPrincipal Jwt token) {
		return currentUserResolver.resolve(token);
	}
}
