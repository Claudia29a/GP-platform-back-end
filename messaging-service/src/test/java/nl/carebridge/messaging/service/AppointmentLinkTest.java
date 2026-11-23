package nl.carebridge.messaging.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** User story nr 9 TAC2, nr 10: the booking link is a setting; without one, CareBridge's own booking page is used. */
@Tag("unit")
class AppointmentLinkTest {

	@Test
	void acceptsAnHttpsLink() {
		assertThat(new AppointmentLink("https://practice.example/book", "https://carebridge.test").urlFor(java.util.UUID.randomUUID()))
				.isEqualTo("https://practice.example/book");
	}

	@Test
	void acceptsPlainHttpOnlyOnThisComputerForLocalTesting() {
		assertThat(new AppointmentLink("http://localhost:4200/book", "https://carebridge.test").urlFor(java.util.UUID.randomUUID()))
				.isEqualTo("http://localhost:4200/book");
	}

	/** User story nr 10: without a practice booking system, the link opens CareBridge's booking page for that invitation. */
	@Test
	void withoutASettingTheLinkOpensCareBridgesBookingPageForThatInvitation() {
		java.util.UUID reply = java.util.UUID.fromString("3f1c2a4e-0000-4000-8000-000000000002");

		assertThat(new AppointmentLink("", "https://carebridge.test").urlFor(reply))
				.isEqualTo("https://carebridge.test/appointment?reply=3f1c2a4e-0000-4000-8000-000000000002");
		assertThat(new AppointmentLink("  ", "https://carebridge.test").urlFor(reply)).startsWith("https://carebridge.test/appointment?reply=");
	}

	@Test
	void refusesToStartWithAnUnsafeOrMissingLink() {
		for (String unsafe : new String[] {"http://practice.example/book", "javascript:alert(1)", "ftp://practice.example",
				"https://", "http://localhost.evil.example/book"}) {
			assertThatThrownBy(() -> new AppointmentLink(unsafe, "https://carebridge.test"))
					.as(unsafe)
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("APPOINTMENT_URL");
		}
	}
}
