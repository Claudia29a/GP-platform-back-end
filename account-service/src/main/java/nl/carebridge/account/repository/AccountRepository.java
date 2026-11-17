package nl.carebridge.account.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nl.carebridge.account.model.Account;

public interface AccountRepository extends JpaRepository<Account, UUID> {

	/** Callers pass the hash of the address in lower case. */
	Optional<Account> findByEmailHash(String emailHash);

	boolean existsByEmailHash(String emailHash);

	@Modifying
	@Query("delete from Account a where a.emailVerified = false and a.createdAt < :before")
	int deleteUnconfirmedCreatedBefore(@Param("before") Instant before);
}
