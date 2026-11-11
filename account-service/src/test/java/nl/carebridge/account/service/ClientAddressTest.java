package nl.carebridge.account.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ClientAddressTest {

	@Test
	void takesTheVisitorAddedByThePlatformNotOneTheVisitorMadeUp() {
		// Made up by the visitor, then the real address from Azure's proxy, then the proxies inside the platform.
		assertThat(ClientAddress.of("1.2.3.4, 81.204.10.20, 100.100.0.7, 172.18.0.5", "10.0.0.2")).contains("81.204.10.20");
	}

	@Test
	void usesTheConnectionWhenThereIsNoForwardedHeader() {
		assertThat(ClientAddress.of(null, "81.204.10.20")).contains("81.204.10.20");
	}

	@Test
	void findsNoVisitorInsideAPrivateNetwork() {
		assertThat(ClientAddress.of("172.18.0.5", "10.0.0.2")).isEmpty();
		assertThat(ClientAddress.of(null, "127.0.0.1")).isEmpty();
	}

	@Test
	void ignoresAnythingThatIsNotAnAddress() {
		assertThat(ClientAddress.of("evil.example.com, 81.204.10.20", "10.0.0.2")).contains("81.204.10.20");
		assertThat(ClientAddress.of("evil.example.com", "10.0.0.2")).isEmpty();
	}

	@Test
	void understandsIpv6InOneFixedForm() {
		// Always written out in full, so the same address always gives the same hash.
		assertThat(ClientAddress.of("2a02:a46e:1234::1, fd00::5", "::1")).contains("2a02:a46e:1234:0:0:0:0:1");
	}
}
