package nl.carebridge.intake.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import nl.carebridge.intake.dto.QueueItemResponse;
import nl.carebridge.intake.dto.ReorderQueueRequest;
import nl.carebridge.intake.service.QueueService;

/** The practice's queue; only the GP assistant and the GP may use it (see SecurityConfig). */
@RestController
@RequestMapping("/queue")
public class QueueController {

	private final QueueService queueService;

	public QueueController(QueueService queueService) {
		this.queueService = queueService;
	}

	@GetMapping
	public List<QueueItemResponse> queue() {
		return queueService.currentQueue().stream().map(QueueItemResponse::from).toList();
	}

	/** Saves a manual order and returns the queue as stored. */
	@PutMapping("/order")
	public List<QueueItemResponse> reorder(@Valid @RequestBody ReorderQueueRequest request) {
		return queueService.reorder(request.intakeIds()).stream().map(QueueItemResponse::from).toList();
	}
}
