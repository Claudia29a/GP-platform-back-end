package nl.carebridge.intake.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import nl.carebridge.intake.security.CurrentUserResolver;
import nl.carebridge.intake.service.PrivacyConsentService;

@RestController
@RequestMapping("/privacy-consent")
public class PrivacyConsentController {

	private final PrivacyConsentService privacyConsentService;
	private final CurrentUserResolver currentUserResolver;

	public PrivacyConsentController(PrivacyConsentService privacyConsentService, CurrentUserResolver currentUserResolver) {
		this.privacyConsentService = privacyConsentService;
		this.currentUserResolver = currentUserResolver;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void allow(@AuthenticationPrincipal Jwt token) {
		privacyConsentService.accept(currentUserResolver.resolve(token));
	}

	/** Withdrawing has to be as easy as allowing (GDPR Article 7). */
	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void withdraw(@AuthenticationPrincipal Jwt token) {
		privacyConsentService.withdraw(currentUserResolver.resolve(token));
	}
}
