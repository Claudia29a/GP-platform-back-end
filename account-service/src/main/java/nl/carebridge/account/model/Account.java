package nl.carebridge.account.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import nl.carebridge.account.encryption.EncryptedText;

/** Someone who signs in with email and password (ADR-010). The password is only stored as a BCrypt hash. */
@Entity
@Table(name = "account")
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** Stored in lower case and encrypted. */
	@Convert(converter = EncryptedText.class)
	@Column(nullable = false)
	private String email;

	/** Finds the account at sign-in, because encrypted text cannot be searched. Unique, so an address is registered once. */
	@Column(nullable = false, length = 64)
	private String emailHash;

	@Convert(converter = EncryptedText.class)
	@Column(nullable = false)
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

	/** A hash of the emailed code, never the code itself. */
	@Column(length = 64)
	private String verificationCodeHash;

	private Instant verificationExpiresAt;

	/** Wrong codes typed for the current code. */
	@Column(nullable = false)
	private int verificationAttempts;

	private Instant lastCodeSentAt;

	/** Start of the hour in which codesSentCount codes were sent. */
	private Instant codesSentSince;

	@Column(nullable = false)
	private int codesSentCount;

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

	public String getEmailHash() {
		return emailHash;
	}

	public void setEmailHash(String emailHash) {
		this.emailHash = emailHash;
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

	public String getVerificationCodeHash() {
		return verificationCodeHash;
	}

	public void setVerificationCodeHash(String verificationCodeHash) {
		this.verificationCodeHash = verificationCodeHash;
	}

	public Instant getVerificationExpiresAt() {
		return verificationExpiresAt;
	}

	public void setVerificationExpiresAt(Instant verificationExpiresAt) {
		this.verificationExpiresAt = verificationExpiresAt;
	}

	public int getVerificationAttempts() {
		return verificationAttempts;
	}

	public void setVerificationAttempts(int verificationAttempts) {
		this.verificationAttempts = verificationAttempts;
	}

	public Instant getLastCodeSentAt() {
		return lastCodeSentAt;
	}

	public void setLastCodeSentAt(Instant lastCodeSentAt) {
		this.lastCodeSentAt = lastCodeSentAt;
	}

	public Instant getCodesSentSince() {
		return codesSentSince;
	}

	public void setCodesSentSince(Instant codesSentSince) {
		this.codesSentSince = codesSentSince;
	}

	public int getCodesSentCount() {
		return codesSentCount;
	}

	public void setCodesSentCount(int codesSentCount) {
		this.codesSentCount = codesSentCount;
	}
}
