package nl.carebridge.account.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nl.carebridge.account.model.SignUp;

public interface SignUpRepository extends JpaRepository<SignUp, UUID> {

	long countByIpHashAndCreatedAtAfter(String ipHash, Instant since);

	@Query("select min(s.createdAt) from SignUp s where s.ipHash = :ipHash and s.createdAt > :since")
	Optional<Instant> findOldestCreatedAt(@Param("ipHash") String ipHash, @Param("since") Instant since);

	@Modifying
	void deleteByCreatedAtBefore(Instant before);
}
