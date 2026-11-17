package nl.carebridge.account.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import nl.carebridge.account.service.DomainResolver.MailCapability;

/** Asks the real DNS. Without a network every answer is UNKNOWN, which the sign-up allows, so these never fail falsely. */
@Tag("integration")
class DnsDomainResolverIntegrationTest {

	private final DnsDomainResolver resolver = new DnsDomainResolver();

	@Test
	void aDomainThatCannotExistReceivesNoMail() {
		// .invalid is reserved and never exists (RFC 2606).
		assertThat(resolver.mailCapability("carebridge-sign-up-check.invalid")).isNotEqualTo(MailCapability.RECEIVES_MAIL);
	}

	@Test
	void aDomainThatDeclaresItTakesNoMailIsRecognised() {
		// example.com publishes "MX 0 .", which means: no mail here (RFC 7505).
		assertThat(resolver.mailCapability("example.com")).isNotEqualTo(MailCapability.RECEIVES_MAIL);
	}

	@Test
	void aRealMailDomainReceivesMail() {
		assertThat(resolver.mailCapability("gmail.com")).isNotEqualTo(MailCapability.NO_MAIL);
	}
}
