package nl.carebridge.intake.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.carebridge.intake.dto.AccessLogResponse;
import nl.carebridge.intake.dto.IntakeResponse;
import nl.carebridge.intake.dto.MyDataResponse;
import nl.carebridge.intake.encryption.DataProtection;
import nl.carebridge.intake.model.Intake;
import nl.carebridge.intake.repository.IntakeRepository;
import nl.carebridge.intake.repository.PrivacyConsentRepository;
import nl.carebridge.intake.security.CurrentUser;

/** A patient's rights over their own data: see and take it (GDPR Art. 15, 20) and delete it (Art. 17). */
@Service
public class MyDataService {

	private final IntakeRepository intakeRepository;
	private final PrivacyConsentRepository consentRepository;
	private final AccessLogService accessLogService;
	private final QueueService queueService;
	private final DataProtection protection;

	public MyDataService(IntakeRepository intakeRepository, PrivacyConsentRepository consentRepository,
			AccessLogService accessLogService, QueueService queueService, DataProtection protection) {
		this.intakeRepository = intakeRepository;
		this.consentRepository = consentRepository;
		this.accessLogService = accessLogService;
		this.queueService = queueService;
		this.protection = protection;
	}

	@Transactional(readOnly = true)
	public MyDataResponse export(CurrentUser patient) {
		String owner = protection.hash(patient.id());
		List<Intake> questions = intakeRepository.findAllByPatientIdOrderBySubmittedAtAsc(owner);
		Map<UUID, List<AccessLogResponse>> openedBy = accessLogService.entriesFor(questions.stream().map(Intake::getId).toList());
		return new MyDataResponse(
				new MyDataResponse.UserData(patient.id(), patient.name(), patient.email()),
				consentRepository.findById(owner)
						.map(consent -> new MyDataResponse.ConsentData(consent.getVersion(), consent.getAcceptedAt()))
						.orElse(null),
				questions.stream()
						.map(question -> new MyDataResponse.QuestionData(IntakeResponse.from(question),
								openedBy.getOrDefault(question.getId(), List.of())))
						.toList());
	}

	/** Removes the patient's questions and permission; the remaining queue closes up. */
	@Transactional
	public void delete(CurrentUser patient) {
		String owner = protection.hash(patient.id());
		intakeRepository.deleteAllByPatientId(owner);
		consentRepository.deleteById(owner);
		queueService.closeGaps();
	}
}
