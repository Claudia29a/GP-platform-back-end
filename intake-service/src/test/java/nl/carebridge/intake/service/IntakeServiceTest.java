package nl.carebridge.intake.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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

@ExtendWith(MockitoExtension.class)
class IntakeServiceTest {

	@Mock
	private IntakeRepository intakeRepository;

	@InjectMocks
	private IntakeService intakeService;

	@Test
	void submitStoresTrimmedDescriptionAsSubmitted() {
		when(intakeRepository.save(any(Intake.class))).thenAnswer(invocation -> invocation.getArgument(0));
		Instant before = Instant.now();

		Intake intake = intakeService.submit("  Back pain since Monday  ");

		assertThat(intake.getDescription()).isEqualTo("Back pain since Monday");
		assertThat(intake.getStatus()).isEqualTo(Intake.Status.SUBMITTED);
		assertThat(intake.getSubmittedAt()).isBetween(before, Instant.now());
		verify(intakeRepository).save(intake);
	}

	@Test
	void submitAcceptsDescriptionOfMaximumLength() {
		when(intakeRepository.save(any(Intake.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Intake intake = intakeService.submit("x".repeat(IntakeService.MAX_DESCRIPTION_LENGTH));

		assertThat(intake.getDescription()).hasSize(IntakeService.MAX_DESCRIPTION_LENGTH);
	}

	@Test
	void submitRejectsMissingDescription() {
		assertBadRequest(() -> intakeService.submit(null));
	}

	@Test
	void submitRejectsBlankDescription() {
		assertBadRequest(() -> intakeService.submit("   "));
	}

	@Test
	void submitRejectsTooLongDescription() {
		assertBadRequest(() -> intakeService.submit("x".repeat(IntakeService.MAX_DESCRIPTION_LENGTH + 1)));
	}

	@Test
	void findByIdOfUnknownIntakeIsNotFound() {
		UUID id = UUID.randomUUID();
		when(intakeRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> intakeService.findById(id))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	private void assertBadRequest(Runnable action) {
		assertThatThrownBy(action::run)
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
		verify(intakeRepository, never()).save(any());
	}
}
