package nl.carebridge.intake.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class CurrentUserResolverTest {

	private final CurrentUserResolver resolver = new CurrentUserResolver(new SecurityProperties("client-id",
			List.of(" GP@Example.com "), List.of("assistant@example.com", "")));

	@Test
	void staffRolesComeFromTheAllowListsIgnoringCaseAndSpaces() {
		assertThat(resolver.roleFor("gp@example.com")).isEqualTo(Role.GP);
		assertThat(resolver.roleFor("Assistant@Example.com")).isEqualTo(Role.GP_ASSISTANT);
	}

	@Test
	void everyoneElseIsAPatient() {
		assertThat(resolver.roleFor("anna@example.com")).isEqualTo(Role.PATIENT);
		assertThat(resolver.roleFor("")).isEqualTo(Role.PATIENT);
		assertThat(resolver.roleFor(null)).isEqualTo(Role.PATIENT);
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
		Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256").subject(subject).claim("email", email);
		return name == null ? builder.build() : builder.claim("name", name).build();
	}
}
