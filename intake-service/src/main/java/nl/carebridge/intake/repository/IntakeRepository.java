package nl.carebridge.intake.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import nl.carebridge.intake.model.Intake;

public interface IntakeRepository extends JpaRepository<Intake, UUID> {

	List<Intake> findAllByOrderBySubmittedAtAsc();
}
