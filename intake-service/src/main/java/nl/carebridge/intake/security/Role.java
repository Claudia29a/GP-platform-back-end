package nl.carebridge.intake.security;

/**
 * Roles from SAD v2, section 2.2. GP assistants and GPs share the practice view;
 * only the GP may reply to patients (increment 4).
 */
public enum Role {
	PATIENT,
	GP_ASSISTANT,
	GP;

	public boolean isStaff() {
		return this == GP_ASSISTANT || this == GP;
	}
}
