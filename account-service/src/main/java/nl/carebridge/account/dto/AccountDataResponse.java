package nl.carebridge.account.dto;

import java.time.Instant;

import nl.carebridge.account.model.Account;

/** What the Account Service stores about a person, for "My data" (GDPR Art. 15, 20). Never the password hash. */
public record AccountDataResponse(String name, String email, boolean emailVerified, Instant createdAt) {

	public static AccountDataResponse from(Account account) {
		return new AccountDataResponse(account.getName(), account.getEmail(), account.isEmailVerified(), account.getCreatedAt());
	}
}
