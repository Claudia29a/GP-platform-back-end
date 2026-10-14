package nl.carebridge.intake.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import nl.carebridge.intake.model.Intake;
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

	@InjectMocks
	private IntakeService intakeService;

	@Test
	void submitStoresTrimmedDescriptionWithThePatientAsOwner() {
		when(intakeRepository.save(any(Intake.class))).thenAnswer(invocation -> invocation.getArgument(0));
		Instant before = Instant.now();

		Intake intake = intakeService.submit("  Back pain since Monday  ", PATIENT);

		assertThat(intake.getDescription()).isEqualTo("Back pain since Monday");
		assertThat(intake.getStatus()).isEqualTo(Intake.Status.SUBMITTED);
		assertThat(intake.getSubmittedAt()).isBetween(before, Instant.now());
		assertThat(intake.getPatientId()).isEqualTo("google-anna");
		assertThat(intake.getPatientName()).isEqualTo("Anna de Vries");
		assertThat(intake.getPatientEmail()).isEqualTo("anna@example.com");
		verify(intakeRepository).save(intake);
	}

	@Test
	void submitAcceptsDescriptionOfMaximumLength() {
		when(intakeRepository.save(any(Intake.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Intake intake = intakeService.submit("x".repeat(IntakeService.MAX_DESCRIPTION_LENGTH), PATIENT);

		assertThat(intake.getDescription()).hasSize(IntakeService.MAX_DESCRIPTION_LENGTH);
	}

	@Test
	void submitRejectsMissingDescription() {
		assertBadRequest(() -> intakeService.submit(null, PATIENT));
	}

	@Test
	void submitRejectsBlankDescription() {
		assertBadRequest(() -> intakeService.submit("   ", PATIENT));
	}

	@Test
	void submitRejectsTooLongDescription() {
		assertBadRequest(() -> intakeService.submit("x".repeat(IntakeService.MAX_DESCRIPTION_LENGTH + 1), PATIENT));
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

	private void assertBadRequest(Runnable action) {
		assertThatThrownBy(action::run)
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
		verify(intakeRepository, never()).save(any());
	}

	private void assertNotFound(Runnable action) {
		assertThatThrownBy(action::run)
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}
}
