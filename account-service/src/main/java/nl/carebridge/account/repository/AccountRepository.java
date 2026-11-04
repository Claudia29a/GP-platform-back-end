package nl.carebridge.account.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import nl.carebridge.account.model.Account;

public interface AccountRepository extends JpaRepository<Account, UUID> {

	/** Callers pass the hash of the address in lower case. */
	Optional<Account> findByEmailHash(String emailHash);

	boolean existsByEmailHash(String emailHash);
}
