package nl.carebridge.intake.service;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.carebridge.intake.dto.AccessLogResponse;
import nl.carebridge.intake.encryption.DataProtection;
import nl.carebridge.intake.model.AccessLogEntry;
import nl.carebridge.intake.repository.AccessLogRepository;
import nl.carebridge.intake.security.CurrentUser;

/** Records which staff member opened which question (GDPR Art. 32; NEN 7513), and reads that record back. */
@Service
public class AccessLogService {

	private final AccessLogRepository repository;
	private final DataProtection protection;

	public AccessLogService(AccessLogRepository repository, DataProtection protection) {
		this.repository = repository;
		this.protection = protection;
	}

	@Transactional
	public void record(UUID intakeId, CurrentUser staff) {
		repository.save(new AccessLogEntry(intakeId, protection.hash(staff.id()), staff.name(), staff.role(), Instant.now()));
	}

	@Transactional(readOnly = true)
	public List<AccessLogResponse> entriesFor(UUID intakeId) {
		return repository.findAllByIntakeIdOrderByViewedAtDesc(intakeId).stream().map(AccessLogResponse::from).toList();
	}

	/** Newest first per question; a question nobody opened gets an empty list. */
	@Transactional(readOnly = true)
	public Map<UUID, List<AccessLogResponse>> entriesFor(Collection<UUID> intakeIds) {
		Map<UUID, List<AccessLogResponse>> log = new LinkedHashMap<>();
		intakeIds.forEach(id -> log.put(id, new java.util.ArrayList<>()));
		if (!intakeIds.isEmpty()) {
			repository.findAllByIntakeIdInOrderByViewedAtDesc(intakeIds)
					.forEach(entry -> log.get(entry.getIntakeId()).add(AccessLogResponse.from(entry)));
		}
		return log;
	}
}
