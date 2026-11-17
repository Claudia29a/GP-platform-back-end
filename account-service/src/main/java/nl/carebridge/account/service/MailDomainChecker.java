package nl.carebridge.account.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import nl.carebridge.account.service.DomainResolver.MailCapability;

/**
 * Refuses a sign-up whose email domain cannot receive email, such as the typo "gmial.con". It cannot tell whether
 * a mailbox exists; the emailed code does that. When DNS does not answer, the sign-up is allowed.
 */
@Service
public class MailDomainChecker {

	static final String NO_MAIL = "No email can be delivered to this address";
	private static final Logger log = LoggerFactory.getLogger(MailDomainChecker.class);

	private final DomainResolver resolver;
	private final Set<String> uncheckedDomains;

	/** uncheckedDomains: test domains only, set locally and in the pipeline; empty in production. */
	public MailDomainChecker(DomainResolver resolver,
			@Value("${carebridge.sign-up.unchecked-email-domains}") List<String> uncheckedDomains) {
		this.resolver = resolver;
		this.uncheckedDomains = uncheckedDomains.stream().map(MailDomainChecker::normalize)
				.filter(domain -> !domain.isEmpty()).collect(Collectors.toSet());
	}

	public void requireMailDomain(String email) {
		String domain = normalize(email.substring(email.lastIndexOf('@') + 1));
		if (uncheckedDomains.contains(domain)) {
			return;
		}
		MailCapability capability = resolver.mailCapability(domain);
		if (capability == MailCapability.UNKNOWN) {
			log.warn("DNS did not answer for an email domain at sign-up; the sign-up was allowed");
		}
		if (capability == MailCapability.NO_MAIL) {
			throw new ResponseStatusException(HttpStatus.valueOf(422), NO_MAIL);
		}
	}

	private static String normalize(String value) {
		return value.strip().toLowerCase(Locale.ROOT);
	}
}
