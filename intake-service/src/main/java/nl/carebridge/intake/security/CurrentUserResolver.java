package nl.carebridge.intake.security;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Turns a verified token (Google or the CareBridge Account Service) into a {@link CurrentUser}.
 * The token only proves who someone is; the role comes from the staff allow-lists, and everyone else
 * is a patient (ADR-006, ADR-010).
 */
@Component
public class CurrentUserResolver {

	public static final String EMAIL_VERIFIED = "EMAIL_VERIFIED";

	private final Set<String> gpEmails;
	private final Set<String> assistantEmails;

	public CurrentUserResolver(SecurityProperties properties) {
		this.gpEmails = normalize(properties.gpEmails());
		this.assistantEmails = normalize(properties.assistantEmails());
	}

	public CurrentUser resolve(Jwt token) {
		String email = token.getClaimAsString("email");
		String name = token.getClaimAsString("name");
		return new CurrentUser(token.getSubject(), name == null || name.isBlank() ? email : name, email,
				roleFor(email, isVerified(token)));
	}

	/** Spring Security authorities: the role, e.g. ROLE_GP, plus EMAIL_VERIFIED once the address is proven. */
	public Collection<GrantedAuthority> authorities(Jwt token) {
		GrantedAuthority role = new SimpleGrantedAuthority("ROLE_" + roleFor(token.getClaimAsString("email"), isVerified(token)).name());
		return isVerified(token) ? List.of(role, new SimpleGrantedAuthority(EMAIL_VERIFIED)) : List.of(role);
	}

	/** Google always verifies; a password account after its emailed code was typed in. */
	public boolean isEmailVerified(Jwt token) {
		return isVerified(token);
	}

	/**
	 * Staff roles need a verified email address: otherwise anyone could create a password account
	 * with a GP's address and act as that GP (ADR-010). Google always verifies; a password account
	 * is unverified until email verification exists.
	 */
	Role roleFor(String email, boolean emailVerified) {
		if (!emailVerified) {
			return Role.PATIENT;
		}
		String key = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
		if (gpEmails.contains(key)) {
			return Role.GP;
		}
		if (assistantEmails.contains(key)) {
			return Role.GP_ASSISTANT;
		}
		return Role.PATIENT;
	}

	private static boolean isVerified(Jwt token) {
		return "true".equals(String.valueOf(token.getClaims().get("email_verified")));
	}

	private static Set<String> normalize(List<String> emails) {
		return emails.stream()
				.map(email -> email.strip().toLowerCase(Locale.ROOT))
				.filter(email -> !email.isEmpty())
				.collect(Collectors.toUnmodifiableSet());
	}
}
