package nl.carebridge.intake.security;

/** The signed-in user, taken from the verified Google ID token and never from the request body. */
public record CurrentUser(String id, String name, String email, Role role) {

	public boolean isStaff() {
		return role.isStaff();
	}
}
