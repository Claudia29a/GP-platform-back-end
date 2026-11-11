package nl.carebridge.intake.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import nl.carebridge.intake.model.AccessLogEntry;

public interface AccessLogRepository extends JpaRepository<AccessLogEntry, UUID> {

	List<AccessLogEntry> findAllByIntakeIdOrderByViewedAtDesc(UUID intakeId);

	List<AccessLogEntry> findAllByIntakeIdInOrderByViewedAtDesc(Collection<UUID> intakeIds);
}
