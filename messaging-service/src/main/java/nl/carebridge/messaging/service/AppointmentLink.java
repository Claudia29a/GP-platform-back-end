package nl.carebridge.messaging.service;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * User story nr 9: the practice's booking page, from the settings (APPOINTMENT_URL), never from code or from the GP.
 * Only an https link is accepted, or plain http on this computer for local testing; otherwise the service does not start.
 */
@Component
public class AppointmentLink {

	private final String url;

	public AppointmentLink(@Value("${carebridge.appointment-url}") String url) {
		this.url = checked(url);
	}

	public String url() {
		return url;
	}

	private static String checked(String url) {
		String value = url == null ? "" : url.strip();
		try {
			URI uri = new URI(value);
			boolean secure = "https".equals(uri.getScheme()) && uri.getHost() != null;
			boolean local = "http".equals(uri.getScheme()) && "localhost".equals(uri.getHost());
			if (secure || local) {
				return value;
			}
		}
		catch (URISyntaxException ex) {
			// Reported below with the setting's name.
		}
		throw new IllegalStateException("APPOINTMENT_URL must be an https link (plain http only for localhost)");
	}
}
