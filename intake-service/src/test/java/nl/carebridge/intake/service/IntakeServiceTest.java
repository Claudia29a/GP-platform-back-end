package nl.carebridge.intake.service;

import static nl.carebridge.intake.TestIntakes.validRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import nl.carebridge.intake.dto.SubmitIntakeRequest;
import nl.carebridge.intake.model.ComplaintArea;
import nl.carebridge.intake.model.ComplaintDuration;
import nl.carebridge.intake.model.Intake;
import nl.carebridge.intake.model.Symptom;
import nl.carebridge.intake.repository.IntakeRepository;
import nl.carebridge.intake.security.CurrentUser;
import nl.carebridge.intake.security.Role;

@ExtendWith(MockitoExtension.class)
class IntakeServiceTest {

	private static final CurrentUser PATIENT = new CurrentUser("google-anna", "Anna de Vries", "anna@example.com", Role.PATIENT);
	private static final CurrentUser ASSISTANT = new CurrentUser("google-sam", "Sam", "sam@example.com", Role.GP_ASSISTANT);
	private static final CurrentUser GP = new CurrentUser("google-gp", "Dr. Jansen", "gp@example.com", Role.GP);

	@Mock
	private IntakeRepository intakeRepository;

	@Spy
	private SeverityCalculator severityCalculator = new SeverityCalculator();

	@Mock
	private QueueService queueService;

	@InjectMocks
	private IntakeService intakeService;

	@BeforeEach
	void saveReturnsTheIntake() {
		org.mockito.Mockito.lenient().when(intakeRepository.save(any(Intake.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void submitStoresAllAnswersWithThePatientAsOwner() {
		Instant before = Instant.now();

		Intake intake = intakeService.submit(validRequest(), PATIENT);

		assertThat(intake.getPatientId()).isEqualTo("google-anna");
		assertThat(intake.getPatientName()).isEqualTo("Anna de Vries");
		assertThat(intake.getPatientEmail()).isEqualTo("anna@example.com");
		assertThat(intake.getComplaintArea()).isEqualTo(ComplaintArea.SKIN);
		assertThat(intake.getDuration()).isEqualTo(ComplaintDuration.ONE_TO_FOUR_WEEKS);
		assertThat(intake.getSeverity()).isEqualTo(4);
		assertThat(intake.getSymptoms()).containsExactlyInAnyOrder(Symptom.RASH_ITCHING, Symptom.SWELLING);
		assertThat(intake.isTakesMedication()).isFalse();
		assertThat(intake.isTriedTreatment()).isTrue();
		assertThat(intake.getTreatmentTried()).isEqualTo("Moisturising cream");
		assertThat(intake.getDescription()).isEqualTo("Itchy rash on my left arm");
		assertThat(intake.getStatus()).isEqualTo(Intake.Status.SUBMITTED);
		assertThat(intake.getSubmittedAt()).isBetween(before, Instant.now());
		verify(intakeRepository).save(intake);
	}

	@Test
	void submitScoresTheQuestionAndPlacesItInTheQueue() {
		Intake intake = intakeService.submit(validRequest(), PATIENT);

		assertThat(intake.getSeverityScore()).isEqualTo(4 * 6 + 15 + 2 * 4);
		verify(queueService).placeByScore(intake);
	}

	@Test
	void freeTextIsTrimmedAndEmptyTextIsNoAnswer() {
		SubmitIntakeRequest request = new SubmitIntakeRequest(ComplaintArea.OTHER, "  Ears  ", ComplaintDuration.LESS_THAN_A_DAY,
				2, Set.of(), "   ", true, " Paracetamol ", false, null, "  Ringing in my ears  ");

		Intake intake = intakeService.submit(request, PATIENT);

		assertThat(intake.getComplaintAreaOther()).isEqualTo("Ears");
		assertThat(intake.getSymptomsOther()).isNull();
		assertThat(intake.getCurrentMedication()).isEqualTo("Paracetamol");
		assertThat(intake.getDescription()).isEqualTo("Ringing in my ears");
	}

	@Test
	void followUpAnswersAreOnlyKeptWhenTheyApply() {
		SubmitIntakeRequest request = new SubmitIntakeRequest(ComplaintArea.SKIN, "Left over from OTHER",
				ComplaintDuration.LESS_THAN_A_DAY, 2, Set.of(), null, false, "Left over from yes", false,
				"Left over from yes", "Rash");

		Intake intake = intakeService.submit(request, PATIENT);

		assertThat(intake.getComplaintAreaOther()).isNull();
		assertThat(intake.getCurrentMedication()).isNull();
		assertThat(intake.getTreatmentTried()).isNull();
	}

	@Test
	void patientOnlySeesOwnQuestions() {
		List<Intake> own = List.of(new Intake());
		when(intakeRepository.findAllByPatientIdOrderBySubmittedAtAsc("google-anna")).thenReturn(own);

		assertThat(intakeService.findVisibleTo(PATIENT)).isSameAs(own);
		verify(intakeRepository, never()).findAllByOrderBySubmittedAtAsc();
	}

	@Test
	void assistantAndGpSeeAllQuestions() {
		List<Intake> all = List.of(new Intake(), new Intake());
		when(intakeRepository.findAllByOrderBySubmittedAtAsc()).thenReturn(all);

		assertThat(intakeService.findVisibleTo(ASSISTANT)).isSameAs(all);
		assertThat(intakeService.findVisibleTo(GP)).isSameAs(all);
	}

	@Test
	void patientGetsNotFoundForSomeoneElsesQuestion() {
		UUID id = UUID.randomUUID();
		when(intakeRepository.findByIdAndPatientId(id, "google-anna")).thenReturn(Optional.empty());

		assertNotFound(() -> intakeService.findById(id, PATIENT));
		verify(intakeRepository, never()).findById(any());
	}

	@Test
	void staffCanOpenAnyQuestion() {
		Intake intake = new Intake();
		UUID id = UUID.randomUUID();
		when(intakeRepository.findById(id)).thenReturn(Optional.of(intake));

		assertThat(intakeService.findById(id, GP)).isSameAs(intake);
	}

	@Test
	void unknownQuestionIsNotFoundForStaff() {
		UUID id = UUID.randomUUID();
		when(intakeRepository.findById(id)).thenReturn(Optional.empty());

		assertNotFound(() -> intakeService.findById(id, ASSISTANT));
	}

	private void assertNotFound(Runnable action) {
		assertThatThrownBy(action::run)
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}
}
