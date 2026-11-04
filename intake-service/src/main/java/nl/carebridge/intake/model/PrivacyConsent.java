package nl.carebridge.intake.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A patient's permission to use their data, with the version of the privacy text they saw. */
@Entity
@Table(name = "privacy_consent")
public class PrivacyConsent {

	/** A hash of the patient's ID, like the owner of a question. */
	@Id
	@Column(length = 64)
	private String userHash;

	@Column(nullable = false, length = 20)
	private String version;

	@Column(nullable = false)
	private Instant acceptedAt;

	protected PrivacyConsent() {
	}

	public PrivacyConsent(String userHash, String version, Instant acceptedAt) {
		this.userHash = userHash;
		this.version = version;
		this.acceptedAt = acceptedAt;
	}

	public String getUserHash() {
		return userHash;
	}

	public String getVersion() {
		return version;
	}

	public Instant getAcceptedAt() {
		return acceptedAt;
	}
}
