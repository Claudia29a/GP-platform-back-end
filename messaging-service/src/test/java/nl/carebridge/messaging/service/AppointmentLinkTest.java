package nl.carebridge.messaging.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** User story nr 9 TAC2: the booking link is a setting, and only a safe link is accepted. */
@Tag("unit")
class AppointmentLinkTest {

	@Test
	void acceptsAnHttpsLink() {
		assertThat(new AppointmentLink("https://practice.example/book").url()).isEqualTo("https://practice.example/book");
	}

	@Test
	void acceptsPlainHttpOnlyOnThisComputerForLocalTesting() {
		assertThat(new AppointmentLink("http://localhost:4200/appointment").url()).isEqualTo("http://localhost:4200/appointment");
	}

	@Test
	void refusesToStartWithAnUnsafeOrMissingLink() {
		for (String unsafe : new String[] {"http://practice.example/book", "javascript:alert(1)", "ftp://practice.example", "", "   ",
				"https://", "http://localhost.evil.example/book"}) {
			assertThatThrownBy(() -> new AppointmentLink(unsafe))
					.as(unsafe)
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("APPOINTMENT_URL");
		}
	}
}
