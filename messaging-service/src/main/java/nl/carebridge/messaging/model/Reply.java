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

	@Column(nullable = false)
	private Instant createdAt;

	protected Reply() {
	}

	public Reply(UUID intakeId, String authorHash, String authorName, String text, Instant createdAt) {
		this.intakeId = intakeId;
		this.authorHash = authorHash;
		this.authorName = authorName;
		this.text = text;
		this.createdAt = createdAt;
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

	public Instant getCreatedAt() {
		return createdAt;
	}
}
