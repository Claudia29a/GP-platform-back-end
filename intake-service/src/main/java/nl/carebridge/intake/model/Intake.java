package nl.carebridge.intake.model;

import java.time.Instant;
import java.util.UUID;

public class Intake {

	public enum Status {
		SUBMITTED
	}

	private UUID id;
	private String description;
	private Status status;
	private Instant submittedAt;

	public Intake() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Instant getSubmittedAt() {
		return submittedAt;
	}

	public void setSubmittedAt(Instant submittedAt) {
		this.submittedAt = submittedAt;
	}
}
