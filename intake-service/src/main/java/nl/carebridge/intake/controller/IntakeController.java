package nl.carebridge.intake.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import nl.carebridge.intake.dto.SubmitIntakeRequest;
import nl.carebridge.intake.model.Intake;
import nl.carebridge.intake.security.CurrentUserResolver;
import nl.carebridge.intake.service.IntakeService;

@RestController
@RequestMapping("/intakes")
public class IntakeController {

	private final IntakeService intakeService;
	private final CurrentUserResolver currentUserResolver;

	public IntakeController(IntakeService intakeService, CurrentUserResolver currentUserResolver) {
		this.intakeService = intakeService;
		this.currentUserResolver = currentUserResolver;
	}

	/**
	 * A patient asks the practice a question; only patients are allowed (see SecurityConfig).
	 * Missing or invalid answers are rejected with 400 before anything is stored.
	 */
	@PostMapping
	public ResponseEntity<Intake> submit(@AuthenticationPrincipal Jwt token,
			@Valid @RequestBody SubmitIntakeRequest request) {
		Intake intake = intakeService.submit(request, currentUserResolver.resolve(token));
		return ResponseEntity.created(URI.create("/intakes/" + intake.getId())).body(intake);
	}

	/** Staff get every question, a patient only their own, oldest first. */
	@GetMapping
	public List<Intake> findAll(@AuthenticationPrincipal Jwt token) {
		return intakeService.findVisibleTo(currentUserResolver.resolve(token));
	}

	@GetMapping("/{id}")
	public Intake findById(@AuthenticationPrincipal Jwt token, @PathVariable UUID id) {
		return intakeService.findById(id, currentUserResolver.resolve(token));
	}
}
