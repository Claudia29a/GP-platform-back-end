package nl.carebridge.messaging.model;

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
import nl.carebridge.messaging.encryption.EncryptedYesNo;

/** The GP's reply to a patient's question. */
@Entity
@Table(name = "reply")
public class Reply {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** The question in the Intake Service. */
	@Column(nullable = false)
	private UUID intakeId;

	/** A hash of the GP's account ID. */
	@Column(nullable = false, length = 64)
	private String authorHash;

	@Convert(converter = EncryptedText.class)
	@Column(nullable = false)
	private String authorName;

	@Convert(converter = EncryptedText.class)
	@Column(nullable = false)
	private String text;

	/** User story nr 9: the GP asked the patient to make an appointment. */
	@Convert(converter = EncryptedYesNo.class)
	private Boolean appointment = false;

	@Column(nullable = false)
	private Instant createdAt;

	/** When the question was sent: the reply is deleted together with it after 6 months. */
	@Column(nullable = false)
	private Instant questionSubmittedAt;

	protected Reply() {
	}

	public Reply(UUID intakeId, String authorHash, String authorName, String text, Instant createdAt, Instant questionSubmittedAt) {
		this(intakeId, authorHash, authorName, text, false, createdAt, questionSubmittedAt);
	}

	public Reply(UUID intakeId, String authorHash, String authorName, String text, boolean appointment, Instant createdAt,
			Instant questionSubmittedAt) {
		this.intakeId = intakeId;
		this.authorHash = authorHash;
		this.authorName = authorName;
		this.text = text;
		this.appointment = appointment;
		this.createdAt = createdAt;
		this.questionSubmittedAt = questionSubmittedAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getIntakeId() {
		return intakeId;
	}

	public String getAuthorHash() {
		return authorHash;
	}

	public String getAuthorName() {
		return authorName;
	}

	public String getText() {
		return text;
	}

	public boolean isAppointment() {
		return Boolean.TRUE.equals(appointment);
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getQuestionSubmittedAt() {
		return questionSubmittedAt;
	}
}
