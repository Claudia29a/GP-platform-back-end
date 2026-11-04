package nl.carebridge.intake.encryption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import nl.carebridge.intake.TestIntakes;

@Tag("unit")
class DataProtectionTest {

	private static final String OTHER_KEY = Base64.getEncoder().encodeToString("abcdef0123456789abcdef0123456789".getBytes());

	private final DataProtection protection = new DataProtection(TestIntakes.DATA_KEY);

	@Test
	void encryptedTextCanBeReadBackButDoesNotShowTheOriginal() {
		String stored = protection.encrypt("Itchy rash on my left arm");

		assertThat(stored).doesNotContain("rash");
		assertThat(protection.decrypt(stored)).isEqualTo("Itchy rash on my left arm");
	}

	@Test
	void theSameTextLooksDifferentEveryTime() {
		assertThat(protection.encrypt("Fever")).isNotEqualTo(protection.encrypt("Fever"));
	}

	@Test
	void changedDataIsRefusedInsteadOfReadAsSomethingElse() {
		byte[] stored = Base64.getDecoder().decode(protection.encrypt("Fever"));
		stored[stored.length - 1] ^= 1;
		String changed = Base64.getEncoder().encodeToString(stored);

		assertThatThrownBy(() -> protection.decrypt(changed)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void anotherKeyCannotReadTheData() {
		String stored = protection.encrypt("Fever");

		assertThatThrownBy(() -> new DataProtection(OTHER_KEY).decrypt(stored)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void aHashIsAlwaysTheSameForTheSameValueAndHidesIt() {
		assertThat(protection.hash("google-anna")).isEqualTo(protection.hash("google-anna"))
				.isNotEqualTo(protection.hash("google-sam"))
				.doesNotContain("anna")
				.hasSize(64);
	}

	@Test
	void aHashCannotBeMadeWithoutTheKey() {
		assertThat(new DataProtection(OTHER_KEY).hash("google-anna")).isNotEqualTo(protection.hash("google-anna"));
	}

	@Test
	void refusesToStartWithAKeyOfTheWrongLength() {
		String shortKey = Base64.getEncoder().encodeToString("too short".getBytes());

		assertThatThrownBy(() -> new DataProtection(shortKey)).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("32 bytes");
		assertThatThrownBy(() -> new DataProtection("not base64!")).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("32 bytes");
	}
}
