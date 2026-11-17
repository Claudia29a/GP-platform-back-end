package nl.carebridge.account.service;

/** Asks DNS whether a domain can receive email. */
public interface DomainResolver {

	enum MailCapability {
		RECEIVES_MAIL,
		NO_MAIL,
		/** DNS did not answer in time; nothing can be concluded. */
		UNKNOWN
	}

	MailCapability mailCapability(String domain);
}
