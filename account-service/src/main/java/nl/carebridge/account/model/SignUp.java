package nl.carebridge.account.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One sign-up from a visitor, kept for an hour to limit how many accounts one address can create. */
@Entity
@Table(name = "sign_up")
public class SignUp {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** A hash of the visitor's IP address, never the address itself. */
	@Column(nullable = false, length = 64)
	private String ipHash;

	@Column(nullable = false)
	private Instant createdAt;

	protected SignUp() {
	}

	public SignUp(String ipHash, Instant createdAt) {
		this.ipHash = ipHash;
		this.createdAt = createdAt;
	}

	public String getIpHash() {
		return ipHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
