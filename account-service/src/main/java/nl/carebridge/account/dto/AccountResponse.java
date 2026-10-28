package nl.carebridge.account.dto;

import java.util.UUID;

import nl.carebridge.account.model.Account;

/** What the outside world may see of an account: never the password hash or the lock-out state. */
public record AccountResponse(UUID id, String name, String email) {

	public static AccountResponse from(Account account) {
		return new AccountResponse(account.getId(), account.getName(), account.getEmail());
	}
}
