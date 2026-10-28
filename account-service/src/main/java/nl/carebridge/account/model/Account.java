package nl.carebridge.account.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Someone who signs in with email and password (ADR-010). The password is only stored as a BCrypt hash. */
@Entity
@Table(name = "account")
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** Stored in lower case, so the same address cannot be registered twice with different capitals. */
	@Column(nullable = false, length = 320)
	private String email;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 100)
	private String passwordHash;

	/** False until the person proves the address is theirs; staff roles need a verified address. */
	@Column(nullable = false)
	private boolean emailVerified;

	@Column(nullable = false)
	private int failedLoginAttempts;

	/** Set after too many wrong passwords; no sign-in is accepted before this moment. */
	private Instant lockedUntil;

	@Column(nullable = false)
	private Instant createdAt;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public boolean isEmailVerified() {
		return emailVerified;
	}

	public void setEmailVerified(boolean emailVerified) {
		this.emailVerified = emailVerified;
	}

	public int getFailedLoginAttempts() {
		return failedLoginAttempts;
	}

	public void setFailedLoginAttempts(int failedLoginAttempts) {
		this.failedLoginAttempts = failedLoginAttempts;
	}

	public Instant getLockedUntil() {
		return lockedUntil;
	}

	public void setLockedUntil(Instant lockedUntil) {
		this.lockedUntil = lockedUntil;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
