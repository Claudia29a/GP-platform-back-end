package nl.carebridge.messaging.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * User story nr 9 and 10: where the "Make an appointment" link goes. Without a setting it opens CareBridge's own
 * booking page for that invitation. A practice with its own booking system sets APPOINTMENT_URL; only an https link
 * is accepted (plain http only on this computer), otherwise the service does not start. Never from the GP.
 */
@Component
public class AppointmentLink {

	private final String configured;
	private final String siteUrl;

	public AppointmentLink(@Value("${carebridge.appointment-url:}") String configured, @Value("${carebridge.site-url}") String siteUrl) {
		this.configured = configured == null || configured.isBlank() ? null : checked(configured.strip());
		this.siteUrl = siteUrl;
	}

	public String urlFor(UUID replyId) {
		return configured != null ? configured : siteUrl + "/appointment?reply=" + replyId;
	}

	private static String checked(String url) {
		try {
			URI uri = new URI(url);
			boolean secure = "https".equals(uri.getScheme()) && uri.getHost() != null;
			boolean local = "http".equals(uri.getScheme()) && "localhost".equals(uri.getHost());
			if (secure || local) {
				return url;
			}
		}
		catch (URISyntaxException ex) {
			// Reported below with the setting's name.
		}
		throw new IllegalStateException("APPOINTMENT_URL must be an https link (plain http only for localhost)");
	}
}
