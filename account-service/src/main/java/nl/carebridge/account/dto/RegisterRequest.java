package nl.carebridge.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A new account. The password rules (at least 8 characters, a capital letter and a number) are the
 * same ones the sign-up form shows; at most 64 characters because BCrypt only uses the first 72 bytes.
 */
public record RegisterRequest(
		@NotBlank @Size(max = 100) String name,
		@NotBlank @Email @Size(max = 320) String email,
		@NotBlank
		@Size(min = PasswordRules.MIN_LENGTH, max = PasswordRules.MAX_LENGTH, message = "must be 8 to 64 characters")
		@Pattern(regexp = PasswordRules.HAS_CAPITAL, message = "must contain a capital letter")
		@Pattern(regexp = PasswordRules.HAS_NUMBER, message = "must contain a number")
		String password) {
}
