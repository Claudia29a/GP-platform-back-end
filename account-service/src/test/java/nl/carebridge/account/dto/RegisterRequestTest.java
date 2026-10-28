package nl.carebridge.account.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class RegisterRequestTest {

	private static ValidatorFactory factory;
	private static Validator validator;

	@BeforeAll
	static void createValidator() {
		factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		factory.close();
	}

	@ParameterizedTest
	@ValueSource(strings = {"Secret12", "Correct horse 9", "ÄÖÜ12345"})
	void acceptsPasswordsThatFollowTheRules(String password) {
		assertThat(messagesFor(password)).isEmpty();
	}

	@ParameterizedTest(name = "\"{0}\" → {1}")
	@CsvSource({
			"Secret1, must be 8 to 64 characters",
			"secret12, must contain a capital letter",
			"SecretPass, must contain a number"})
	void rejectsPasswordsThatBreakARule(String password, String message) {
		assertThat(messagesFor(password)).containsExactly(message);
	}

	@Test
	void rejectsTooLongPasswords() {
		assertThat(messagesFor("A1" + "x".repeat(63))).containsExactly("must be 8 to 64 characters");
	}

	@Test
	void requiresNameAndAValidEmail() {
		Set<String> fields = validator.validate(new RegisterRequest(" ", "not-an-email", "Secret12")).stream()
				.map(v -> v.getPropertyPath().toString())
				.collect(Collectors.toSet());

		assertThat(fields).containsExactlyInAnyOrder("name", "email");
	}

	private static Set<String> messagesFor(String password) {
		return validator.validate(new RegisterRequest("Anna", "anna@example.com", password)).stream()
				.map(ConstraintViolation::getMessage)
				.collect(Collectors.toSet());
	}
}
