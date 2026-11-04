package nl.carebridge.intake.dto;

import nl.carebridge.intake.security.CurrentUser;
import nl.carebridge.intake.security.Role;

/** Who is signed in, and whether the front-end has to ask them to allow the use of their data. */
public record UserResponse(String id, String name, String email, Role role, boolean privacyConsentNeeded) {

	public static UserResponse from(CurrentUser user, boolean privacyConsentNeeded) {
		return new UserResponse(user.id(), user.name(), user.email(), user.role(), privacyConsentNeeded);
	}
}
