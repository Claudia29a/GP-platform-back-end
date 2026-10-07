package nl.carebridge.intake.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nl.carebridge.intake.model.Intake;
import nl.carebridge.intake.service.IntakeService;

@RestController
@RequestMapping("/intakes")
public class IntakeController {

	private final IntakeService intakeService;

	public IntakeController(IntakeService intakeService) {
		this.intakeService = intakeService;
	}

	/** Patient submits a short description of their complaint. */
	@PostMapping
	public ResponseEntity<Intake> submit(@RequestBody Intake request) {
		Intake intake = intakeService.submit(request.getDescription());
		return ResponseEntity.created(URI.create("/intakes/" + intake.getId())).body(intake);
	}

	/** The practice reviews all submitted complaints, oldest first. */
	@GetMapping
	public List<Intake> findAll() {
		return intakeService.findAll();
	}

	@GetMapping("/{id}")
	public Intake findById(@PathVariable UUID id) {
		return intakeService.findById(id);
	}
}
