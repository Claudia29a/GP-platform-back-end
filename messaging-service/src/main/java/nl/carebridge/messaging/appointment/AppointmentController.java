package nl.carebridge.messaging.appointment;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import nl.carebridge.messaging.security.CurrentUserResolver;

/** User story nr 10 TAC5. Who may call what is in SecurityConfig: patients book, staff see the day list. */
@RestController
public class AppointmentController {

	private final AppointmentService service;
	private final SlotCalendar calendar;
	private final CurrentUserResolver currentUserResolver;

	public AppointmentController(AppointmentService service, SlotCalendar calendar, CurrentUserResolver currentUserResolver) {
		this.service = service;
		this.calendar = calendar;
		this.currentUserResolver = currentUserResolver;
	}

	@GetMapping("/appointments/free")
	public List<SlotResponse> free() {
		return service.free().stream().map(start -> SlotResponse.from(start, calendar)).toList();
	}

	@PostMapping("/replies/{replyId}/appointment")
	@ResponseStatus(HttpStatus.CREATED)
	public AppointmentResponse book(@PathVariable UUID replyId, @Valid @RequestBody BookRequest request, @AuthenticationPrincipal Jwt token) {
		return AppointmentResponse.from(service.book(replyId, request.start(), currentUserResolver.resolve(token), bearer(token)), calendar);
	}

	@DeleteMapping("/appointments/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancel(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
		service.cancel(id, currentUserResolver.resolve(token), bearer(token));
	}

	@GetMapping("/appointments")
	public List<AppointmentResponse> onDay(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day) {
		return service.onDay(day).stream().map(appointment -> AppointmentResponse.from(appointment, calendar)).toList();
	}

	/**
	 * The booking rules explain themselves to the patient ("This time was just taken…"). These reasons are written for
	 * patients and never contain personal data.
	 */
	@ExceptionHandler(ResponseStatusException.class)
	ResponseEntity<java.util.Map<String, String>> explain(ResponseStatusException ex) {
		return ResponseEntity.status(ex.getStatusCode()).body(java.util.Map.of("message", ex.getReason() == null ? "" : ex.getReason()));
	}

	/** The user's own, already checked sign-in, passed on to the Intake Service. */
	private static String bearer(Jwt token) {
		return "Bearer " + token.getTokenValue();
	}
}
