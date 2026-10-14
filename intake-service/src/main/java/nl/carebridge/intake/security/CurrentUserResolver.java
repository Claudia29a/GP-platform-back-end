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
 * Turns a verified Google ID token into a {@link CurrentUser}. Google only proves who someone is;
 * the role comes from the staff allow-lists, and everyone else is a patient (ADR-006).
 */
@Component
public class CurrentUserResolver {

	private final Set<String> gpEmails;
	private final Set<String> assistantEmails;

	public CurrentUserResolver(SecurityProperties properties) {
		this.gpEmails = normalize(properties.gpEmails());
		this.assistantEmails = normalize(properties.assistantEmails());
	}

	public CurrentUser resolve(Jwt token) {
		String email = token.getClaimAsString("email");
		String name = token.getClaimAsString("name");
		return new CurrentUser(token.getSubject(), name == null || name.isBlank() ? email : name, email, roleFor(email));
	}

	/** Spring Security authorities for the role, e.g. ROLE_GP. */
	public Collection<GrantedAuthority> authorities(Jwt token) {
		return List.of(new SimpleGrantedAuthority("ROLE_" + roleFor(token.getClaimAsString("email")).name()));
	}

	Role roleFor(String email) {
		String key = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
		if (gpEmails.contains(key)) {
			return Role.GP;
		}
		if (assistantEmails.contains(key)) {
			return Role.GP_ASSISTANT;
		}
		return Role.PATIENT;
	}

	private static Set<String> normalize(List<String> emails) {
		return emails.stream()
				.map(email -> email.strip().toLowerCase(Locale.ROOT))
				.filter(email -> !email.isEmpty())
				.collect(Collectors.toUnmodifiableSet());
	}
}
