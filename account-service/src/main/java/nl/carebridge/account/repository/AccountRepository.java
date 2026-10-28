package nl.carebridge.account.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import nl.carebridge.account.model.Account;

public interface AccountRepository extends JpaRepository<Account, UUID> {

	/** Emails are stored in lower case; callers pass the address in lower case too. */
	Optional<Account> findByEmail(String email);

	boolean existsByEmail(String email);
}
