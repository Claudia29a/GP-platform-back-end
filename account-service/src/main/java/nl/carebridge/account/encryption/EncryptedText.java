package nl.carebridge.account.encryption;

import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;

/** Encrypts a field on its way to the database and decrypts it on the way back. */
@Component
public class EncryptedText implements AttributeConverter<String, String> {

	private final DataProtection protection;

	public EncryptedText(DataProtection protection) {
		this.protection = protection;
	}

	@Override
	public String convertToDatabaseColumn(String value) {
		return value == null ? null : protection.encrypt(value);
	}

	@Override
	public String convertToEntityAttribute(String stored) {
		return stored == null ? null : protection.decrypt(stored);
	}
}
