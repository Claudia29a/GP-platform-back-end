package nl.carebridge.account.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import nl.carebridge.account.service.DomainResolver.MailCapability;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class MailDomainCheckerTest {

	@Mock
	private DomainResolver resolver;

	private MailDomainChecker checker(String... uncheckedDomains) {
		return new MailDomainChecker(resolver, List.of(uncheckedDomains));
	}

	@Test
	void acceptsADomainThatReceivesMail() {
		when(resolver.mailCapability("gmail.com")).thenReturn(MailCapability.RECEIVES_MAIL);

		assertThatCode(() -> checker().requireMailDomain(" Anna@Gmail.com ")).doesNotThrowAnyException();
	}

	@Test
	void refusesADomainThatCannotReceiveMailWith422() {
		when(resolver.mailCapability("gmial.con")).thenReturn(MailCapability.NO_MAIL);

		assertThatThrownBy(() -> checker().requireMailDomain("anna@gmial.con")).isInstanceOfSatisfying(ResponseStatusException.class,
				ex -> org.assertj.core.api.Assertions.assertThat(ex.getStatusCode().value()).isEqualTo(422));
	}

	@Test
	void allowsTheSignUpWhenDnsDoesNotAnswer() {
		when(resolver.mailCapability("fontys.nl")).thenReturn(MailCapability.UNKNOWN);

		assertThatCode(() -> checker().requireMailDomain("student@fontys.nl")).doesNotThrowAnyException();
	}

	@Test
	void skipsOnlyTheDomainsTheSettingLists() {
		assertThatCode(() -> checker("example.com").requireMailDomain("e2e-patient@Example.com")).doesNotThrowAnyException();
		verify(resolver, never()).mailCapability("example.com");

		when(resolver.mailCapability("example.com")).thenReturn(MailCapability.NO_MAIL);
		assertThatThrownBy(() -> checker().requireMailDomain("e2e-patient@example.com")).isInstanceOf(ResponseStatusException.class);
	}
}
