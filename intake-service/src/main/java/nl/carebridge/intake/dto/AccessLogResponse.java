package nl.carebridge.intake.dto;

import java.time.Instant;

import nl.carebridge.intake.model.AccessLogEntry;
import nl.carebridge.intake.security.Role;

/** Who opened a question, with their role, and when. */
public record AccessLogResponse(String staffName, Role role, Instant viewedAt) {

	public static AccessLogResponse from(AccessLogEntry entry) {
		return new AccessLogResponse(entry.getStaffName(), entry.getStaffRole(), entry.getViewedAt());
	}
}
