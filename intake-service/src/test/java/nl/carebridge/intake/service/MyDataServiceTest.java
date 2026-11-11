package nl.carebridge.intake.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import nl.carebridge.intake.TestIntakes;
import nl.carebridge.intake.dto.AccessLogResponse;
import nl.carebridge.intake.dto.MyDataResponse;
import nl.carebridge.intake.encryption.DataProtection;
import nl.carebridge.intake.model.Intake;
import nl.carebridge.intake.model.PrivacyConsent;
import nl.carebridge.intake.repository.IntakeRepository;
import nl.carebridge.intake.repository.PrivacyConsentRepository;
import nl.carebridge.intake.security.CurrentUser;
import nl.carebridge.intake.security.Role;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class MyDataServiceTest {

	private static final CurrentUser PATIENT = new CurrentUser("google-anna", "Anna de Vries", "anna@example.com", Role.PATIENT);

	@Mock
	private IntakeRepository intakeRepository;

	@Mock
	private PrivacyConsentRepository consentRepository;

	@Mock
	private AccessLogService accessLogService;

	@Mock
	private QueueService queueService;

	private final DataProtection protection = new DataProtection(TestIntakes.DATA_KEY);
	private MyDataService service;
	private String owner;

	@BeforeEach
	void createService() {
		service = new MyDataService(intakeRepository, consentRepository, accessLogService, queueService, protection);
		owner = protection.hash("google-anna");
	}

	@Test
	void theExportHoldsTheUserTheirPermissionTheirQuestionsAndWhoOpenedThem() {
		Intake question = new Intake();
		question.setId(UUID.randomUUID());
		question.setDescription("Itchy rash on my left arm");
		when(intakeRepository.findAllByPatientIdOrderBySubmittedAtAsc(owner)).thenReturn(List.of(question));
		when(consentRepository.findById(owner))
				.thenReturn(Optional.of(new PrivacyConsent(owner, "2026-11-11", Instant.parse("2026-11-11T10:00:00Z"))));
		AccessLogResponse opened = new AccessLogResponse("Dr. Jansen", Role.GP, Instant.parse("2026-11-11T11:00:00Z"));
		when(accessLogService.entriesFor(List.of(question.getId()))).thenReturn(Map.of(question.getId(), List.of(opened)));

		MyDataResponse data = service.export(PATIENT);

		assertThat(data.user().email()).isEqualTo("anna@example.com");
		assertThat(data.privacyConsent().version()).isEqualTo("2026-11-11");
		assertThat(data.questions()).singleElement().satisfies(item -> {
			assertThat(item.question().description()).isEqualTo("Itchy rash on my left arm");
			assertThat(item.openedBy()).containsExactly(opened);
		});
	}

	@Test
	void theExportSaysWhenThereIsNoPermission() {
		when(intakeRepository.findAllByPatientIdOrderBySubmittedAtAsc(owner)).thenReturn(List.of());
		when(consentRepository.findById(owner)).thenReturn(Optional.empty());
		when(accessLogService.entriesFor(List.of())).thenReturn(Map.of());

		assertThat(service.export(PATIENT).privacyConsent()).isNull();
	}

	@Test
	void deletingRemovesTheQuestionsAndPermissionThenClosesTheGapsInTheQueue() {
		service.delete(PATIENT);

		var order = inOrder(intakeRepository, consentRepository, queueService);
		order.verify(intakeRepository).deleteAllByPatientId(owner);
		order.verify(consentRepository).deleteById(owner);
		order.verify(queueService).closeGaps();
	}
}
