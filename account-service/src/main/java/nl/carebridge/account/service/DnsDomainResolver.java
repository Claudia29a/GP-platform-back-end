package nl.carebridge.account.service;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.springframework.stereotype.Component;

/**
 * Looks up a domain's mail servers (MX records) with Java's built-in DNS client. Rules from the email standards:
 * no MX but an address means mail goes to that address (RFC 5321); "MX 0 ." means the domain takes no mail
 * (RFC 7505); a domain that does not exist takes no mail.
 */
@Component
public class DnsDomainResolver implements DomainResolver {

	private static final String NULL_MX = "0 .";

	@Override
	public MailCapability mailCapability(String domain) {
		DirContext dns = null;
		try {
			dns = connect();
			Attribute mx = dns.getAttributes(domain, new String[] {"MX"}).get("MX");
			if (mx != null && mx.size() > 0) {
				boolean nullMx = mx.size() == 1 && NULL_MX.equals(String.valueOf(mx.get()).strip());
				return nullMx ? MailCapability.NO_MAIL : MailCapability.RECEIVES_MAIL;
			}
			Attributes address = dns.getAttributes(domain, new String[] {"A", "AAAA"});
			return address.size() > 0 ? MailCapability.RECEIVES_MAIL : MailCapability.NO_MAIL;
		}
		catch (NameNotFoundException ex) {
			return MailCapability.NO_MAIL;
		}
		catch (NamingException ex) {
			return MailCapability.UNKNOWN;
		}
		finally {
			close(dns);
		}
	}

	private static DirContext connect() throws NamingException {
		Hashtable<String, String> env = new Hashtable<>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
		// At most about 4 seconds, so a slow DNS never makes sign-up hang.
		env.put("com.sun.jndi.dns.timeout.initial", "2000");
		env.put("com.sun.jndi.dns.timeout.retries", "1");
		return new InitialDirContext(env);
	}

	private static void close(DirContext dns) {
		if (dns != null) {
			try {
				dns.close();
			}
			catch (NamingException ex) {
				// Nothing left to clean up.
			}
		}
	}
}
