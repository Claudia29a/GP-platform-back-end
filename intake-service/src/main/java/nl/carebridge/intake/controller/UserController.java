package nl.carebridge.intake.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import nl.carebridge.intake.dto.UserResponse;
import nl.carebridge.intake.security.CurrentUser;
import nl.carebridge.intake.security.CurrentUserResolver;
import nl.carebridge.intake.service.PrivacyConsentService;

@RestController
public class UserController {

	private final CurrentUserResolver currentUserResolver;
	private final PrivacyConsentService privacyConsentService;

	public UserController(CurrentUserResolver currentUserResolver, PrivacyConsentService privacyConsentService) {
		this.currentUserResolver = currentUserResolver;
		this.privacyConsentService = privacyConsentService;
	}

	/** Who is signed in and with which role, so the front-end knows which pages to show. */
	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal Jwt token) {
		CurrentUser user = currentUserResolver.resolve(token);
		return UserResponse.from(user, currentUserResolver.isEmailVerified(token), privacyConsentService.isNeeded(user));
	}
}
