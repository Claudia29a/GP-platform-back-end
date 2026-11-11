package nl.carebridge.intake.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nl.carebridge.intake.model.Intake;

public interface IntakeRepository extends JpaRepository<Intake, UUID> {

	List<Intake> findAllByOrderBySubmittedAtAsc();

	List<Intake> findAllByPatientIdOrderBySubmittedAtAsc(String patientId);

	Optional<Intake> findByIdAndPatientId(UUID id, String patientId);

	void deleteAllByPatientId(String patientId);

	/**
	 * Takes a database lock until the transaction ends, so only one change to the queue runs at a
	 * time, also across several copies of this service (Story 4 TAC6).
	 */
	@Query(value = "select 1 from pg_advisory_xact_lock(:key)", nativeQuery = true)
	int lockQueue(@Param("key") long key);

	/** The practice's queue: open questions in queue order. */
	List<Intake> findAllByStatusOrderByQueuePositionAscSubmittedAtAsc(Intake.Status status);
}
