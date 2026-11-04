package nl.carebridge.intake.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import nl.carebridge.intake.model.PrivacyConsent;

public interface PrivacyConsentRepository extends JpaRepository<PrivacyConsent, String> {
}
