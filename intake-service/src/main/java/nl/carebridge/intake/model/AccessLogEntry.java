package nl.carebridge.intake.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import nl.carebridge.intake.encryption.EncryptedConverters;
import nl.carebridge.intake.security.Role;

/** One time a staff member opened a question. */
@Entity
@Table(name = "access_log")
public class AccessLogEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UUID intakeId;

	/** A hash of the staff member's account ID. */
	@Column(nullable = false, length = 64)
	private String staffHash;

	@Convert(converter = EncryptedConverters.Text.class)
	@Column(nullable = false)
	private String staffName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role staffRole;

	@Column(nullable = false)
	private Instant viewedAt;

	protected AccessLogEntry() {
	}

	public AccessLogEntry(UUID intakeId, String staffHash, String staffName, Role staffRole, Instant viewedAt) {
		this.intakeId = intakeId;
		this.staffHash = staffHash;
		this.staffName = staffName;
		this.staffRole = staffRole;
		this.viewedAt = viewedAt;
	}

	public UUID getIntakeId() {
		return intakeId;
	}

	public String getStaffHash() {
		return staffHash;
	}

	public String getStaffName() {
		return staffName;
	}

	public Role getStaffRole() {
		return staffRole;
	}

	public Instant getViewedAt() {
		return viewedAt;
	}
}
