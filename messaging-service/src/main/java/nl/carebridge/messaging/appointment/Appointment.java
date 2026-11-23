package nl.carebridge.messaging.appointment;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import nl.carebridge.messaging.encryption.EncryptedText;

/**
 * User story nr 10: a patient's booked appointment, for the GP's invitation (a reply). The database allows one
 * appointment per start time and per invitation, and deletes it together with the reply.
 */
@Entity
@Table(name = "appointment")
public class Appointment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true)
	private UUID replyId;

	@Column(nullable = false)
	private UUID intakeId;

	/** A hash of the patient's account ID: only they may cancel. */
	@Column(nullable = false, length = 64)
	private String patientHash;

	/** For the practice's list of the day. */
	@Convert(converter = EncryptedText.class)
	@Column(nullable = false)
	private String patientName;

	@Column(nullable = false, unique = true)
	private Instant startsAt;

	@Column(nullable = false)
	private Instant createdAt;

	protected Appointment() {
	}

	public Appointment(UUID replyId, UUID intakeId, String patientHash, String patientName, Instant startsAt, Instant createdAt) {
		this.replyId = replyId;
		this.intakeId = intakeId;
		this.patientHash = patientHash;
		this.patientName = patientName;
		this.startsAt = startsAt;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getReplyId() {
		return replyId;
	}

	public UUID getIntakeId() {
		return intakeId;
	}

	public String getPatientHash() {
		return patientHash;
	}

	public String getPatientName() {
		return patientName;
	}

	public Instant getStartsAt() {
		return startsAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
