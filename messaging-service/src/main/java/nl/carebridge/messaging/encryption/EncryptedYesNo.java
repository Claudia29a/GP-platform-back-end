package nl.carebridge.messaging.encryption;

import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;

/**
 * Encrypts a yes/no answer, so the database never shows it in plain text. An empty column (rows from before the
 * field existed) reads as "no".
 */
@Component
public class EncryptedYesNo implements AttributeConverter<Boolean, String> {

	private final DataProtection protection;

	public EncryptedYesNo(DataProtection protection) {
		this.protection = protection;
	}

	@Override
	public String convertToDatabaseColumn(Boolean value) {
		return protection.encrypt(Boolean.TRUE.equals(value) ? "yes" : "no");
	}

	@Override
	public Boolean convertToEntityAttribute(String stored) {
		return stored != null && "yes".equals(protection.decrypt(stored));
	}
}
