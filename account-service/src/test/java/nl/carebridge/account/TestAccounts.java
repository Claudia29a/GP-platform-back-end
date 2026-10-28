package nl.carebridge.account;

import java.time.Instant;
import java.util.UUID;

import nl.carebridge.account.model.Account;

public final class TestAccounts {

	private TestAccounts() {
	}

	public static Account account(String email, String passwordHash) {
		Account account = new Account();
		account.setId(UUID.fromString("7d1b6c2e-0000-4000-8000-000000000001"));
		account.setEmail(email);
		account.setName("Anna de Vries");
		account.setPasswordHash(passwordHash);
		account.setCreatedAt(Instant.parse("2026-10-28T09:00:00Z"));
		return account;
	}
}
