package nl.carebridge.intake.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class CurrentUserResolverTest {

	private final CurrentUserResolver resolver = new CurrentUserResolver(new SecurityProperties("client-id", "carebridge-account-service", "http://account/jwks",
			List.of(" GP@Example.com "), List.of("assistant@example.com", "")));

	@Test
	void staffRolesComeFromTheAllowListsIgnoringCaseAndSpaces() {
		assertThat(resolver.roleFor("gp@example.com", true)).isEqualTo(Role.GP);
		assertThat(resolver.roleFor("Assistant@Example.com", true)).isEqualTo(Role.GP_ASSISTANT);
	}

	@Test
	void anUnverifiedStaffAddressIsOnlyAPatient() {
		// Someone created a password account with the GP's address; the address is not proven to be theirs.
		assertThat(resolver.roleFor("gp@example.com", false)).isEqualTo(Role.PATIENT);
		assertThat(resolver.roleFor("assistant@example.com", false)).isEqualTo(Role.PATIENT);
	}

	@Test
	void readsTheVerifiedFlagFromTheToken() {
		Jwt unverified = Jwt.withTokenValue("token").header("alg", "RS256").subject("account-1")
				.claim("email", "gp@example.com").claim("email_verified", false).build();
		Jwt googleStyle = Jwt.withTokenValue("token").header("alg", "RS256").subject("google-1")
				.claim("email", "gp@example.com").claim("email_verified", "true").build();

		assertThat(resolver.resolve(unverified).role()).isEqualTo(Role.PATIENT);
		assertThat(resolver.resolve(googleStyle).role()).isEqualTo(Role.GP);
		assertThat(resolver.authorities(unverified)).extracting(Object::toString).containsExactly("ROLE_PATIENT");
	}

	@Test
	void everyoneElseIsAPatient() {
		assertThat(resolver.roleFor("anna@example.com", true)).isEqualTo(Role.PATIENT);
		assertThat(resolver.roleFor("", true)).isEqualTo(Role.PATIENT);
		assertThat(resolver.roleFor(null, true)).isEqualTo(Role.PATIENT);
	}

	@Test
	void resolvesUserFromToken() {
		CurrentUser user = resolver.resolve(token("google-gp", "Dr. Jansen", "gp@example.com"));

		assertThat(user).isEqualTo(new CurrentUser("google-gp", "Dr. Jansen", "gp@example.com", Role.GP));
		assertThat(user.isStaff()).isTrue();
	}

	@Test
	void usesEmailAsNameWhenTokenHasNoName() {
		CurrentUser user = resolver.resolve(token("google-anna", null, "anna@example.com"));

		assertThat(user.name()).isEqualTo("anna@example.com");
		assertThat(user.isStaff()).isFalse();
	}

	@Test
	void authorityMatchesRole() {
		assertThat(resolver.authorities(token("google-sam", "Sam", "assistant@example.com")))
				.extracting(Object::toString).containsExactly("ROLE_GP_ASSISTANT");
	}

	private static Jwt token(String subject, String name, String email) {
		Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256").subject(subject).claim("email", email)
				.claim("email_verified", true);
		return name == null ? builder.build() : builder.claim("name", name).build();
	}
}
