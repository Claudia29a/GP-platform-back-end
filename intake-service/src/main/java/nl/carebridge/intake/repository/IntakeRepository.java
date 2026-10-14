package nl.carebridge.intake.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import nl.carebridge.intake.model.Intake;

public interface IntakeRepository extends JpaRepository<Intake, UUID> {

	List<Intake> findAllByOrderBySubmittedAtAsc();

	List<Intake> findAllByPatientIdOrderBySubmittedAtAsc(String patientId);

	Optional<Intake> findByIdAndPatientId(UUID id, String patientId);
}
