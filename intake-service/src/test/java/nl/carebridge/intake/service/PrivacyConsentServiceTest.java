package nl.carebridge.intake.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import nl.carebridge.intake.TestIntakes;
import nl.carebridge.intake.encryption.DataProtection;
import nl.carebridge.intake.model.PrivacyConsent;
import nl.carebridge.intake.repository.PrivacyConsentRepository;
import nl.carebridge.intake.security.CurrentUser;
import nl.carebridge.intake.security.Role;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PrivacyConsentServiceTest {

	private static final CurrentUser PATIENT = new CurrentUser("google-anna", "Anna", "anna@example.com", Role.PATIENT);
	private static final CurrentUser GP = new CurrentUser("google-gp", "Dr. Jansen", "gp@example.com", Role.GP);
	private static final String CURRENT_VERSION = "2026-11-04";

	private final DataProtection protection = new DataProtection(TestIntakes.DATA_KEY);

	@Mock
	private PrivacyConsentRepository repository;

	private PrivacyConsentService service;

	@BeforeEach
	void createService() {
		service = new PrivacyConsentService(repository, protection, CURRENT_VERSION);
	}

	@Test
	void aPatientWhoNeverAllowedItIsAsked() {
		when(repository.findById(protection.hash("google-anna"))).thenReturn(Optional.empty());

		assertThat(service.isNeeded(PATIENT)).isTrue();
	}

	@Test
	void aPatientWhoAllowedTheCurrentVersionIsNotAskedAgain() {
		when(repository.findById(protection.hash("google-anna")))
				.thenReturn(Optional.of(new PrivacyConsent("hash", CURRENT_VERSION, Instant.now())));

		assertThat(service.isNeeded(PATIENT)).isFalse();
	}

	@Test
	void aPatientIsAskedAgainWhenThePrivacyTextChanged() {
		when(repository.findById(protection.hash("google-anna")))
				.thenReturn(Optional.of(new PrivacyConsent("hash", "2020-01-01", Instant.now())));

		assertThat(service.isNeeded(PATIENT)).isTrue();
	}

	@Test
	void staffAreNeverAsked() {
		assertThat(service.isNeeded(GP)).isFalse();
		verify(repository, never()).findById(any());
	}

	@Test
	void allowingStoresTheVersionAndTimeUnderAHashNotThePatientId() {
		Instant before = Instant.now();

		service.accept(PATIENT);

		ArgumentCaptor<PrivacyConsent> saved = ArgumentCaptor.forClass(PrivacyConsent.class);
		verify(repository).save(saved.capture());
		assertThat(saved.getValue().getUserHash()).isEqualTo(protection.hash("google-anna"));
		assertThat(saved.getValue().getVersion()).isEqualTo(CURRENT_VERSION);
		assertThat(saved.getValue().getAcceptedAt()).isAfterOrEqualTo(before);
	}

	@Test
	void withdrawingRemovesTheConsent() {
		service.withdraw(PATIENT);

		verify(repository).deleteById(protection.hash("google-anna"));
	}
}
