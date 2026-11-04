package nl.carebridge.intake.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.carebridge.intake.encryption.DataProtection;
import nl.carebridge.intake.model.PrivacyConsent;
import nl.carebridge.intake.repository.PrivacyConsentRepository;
import nl.carebridge.intake.security.CurrentUser;

/** Keeps track of which patients allowed the use of their data. Staff are not asked: they send no health data. */
@Service
public class PrivacyConsentService {

	private final PrivacyConsentRepository repository;
	private final DataProtection protection;
	private final String currentVersion;

	public PrivacyConsentService(PrivacyConsentRepository repository, DataProtection protection,
			@Value("${carebridge.privacy.version}") String currentVersion) {
		this.repository = repository;
		this.protection = protection;
		this.currentVersion = currentVersion;
	}

	/** A patient is asked until they allowed the current privacy text, so a new text is asked again. */
	@Transactional(readOnly = true)
	public boolean isNeeded(CurrentUser user) {
		return !user.isStaff() && repository.findById(protection.hash(user.id()))
				.filter(consent -> consent.getVersion().equals(currentVersion))
				.isEmpty();
	}

	@Transactional
	public void accept(CurrentUser patient) {
		repository.save(new PrivacyConsent(protection.hash(patient.id()), currentVersion, Instant.now()));
	}

	@Transactional
	public void withdraw(CurrentUser patient) {
		repository.deleteById(protection.hash(patient.id()));
	}
}
