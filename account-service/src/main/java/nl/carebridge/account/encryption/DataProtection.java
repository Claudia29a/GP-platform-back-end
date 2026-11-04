package nl.carebridge.account.encryption;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Protects personal data in the database, so a copy of it shows nothing without the key.
 * Data that people read is encrypted (AES-256-GCM); data that is only compared, such as an email address at sign-in,
 * is hashed (HMAC-SHA-256), because a hash cannot be turned back into the original.
 */
@Component
public class DataProtection {

	private static final String CIPHER = "AES/GCM/NoPadding";
	private static final String MAC = "HmacSHA256";
	private static final int KEY_BYTES = 32;
	private static final int IV_BYTES = 12;
	private static final int TAG_BITS = 128;

	private final SecureRandom random = new SecureRandom();
	private final SecretKeySpec encryptionKey;
	private final SecretKeySpec hashKey;

	public DataProtection(@Value("${carebridge.data.key}") String base64Key) {
		byte[] key = decodeKey(base64Key);
		encryptionKey = new SecretKeySpec(key, "AES");
		// A separate key for hashing, derived from the same setting.
		hashKey = new SecretKeySpec(mac(new SecretKeySpec(key, MAC), "hash"), MAC);
	}

	/** Returns the text encrypted; the random start value makes the same text look different every time. */
	public String encrypt(String text) {
		byte[] iv = new byte[IV_BYTES];
		random.nextBytes(iv);
		try {
			Cipher cipher = Cipher.getInstance(CIPHER);
			cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, iv));
			byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Could not encrypt the data", ex);
		}
	}

	/** Reads encrypted text back; data that was changed or uses another key is refused. */
	public String decrypt(String stored) {
		byte[] data = Base64.getDecoder().decode(stored);
		try {
			Cipher cipher = Cipher.getInstance(CIPHER);
			cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES));
			return new String(cipher.doFinal(data, IV_BYTES, data.length - IV_BYTES), StandardCharsets.UTF_8);
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Could not decrypt the data", ex);
		}
	}

	/** Always gives the same 64 characters for the same value, so it can be searched for. */
	public String hash(String value) {
		return HexFormat.of().formatHex(mac(hashKey, value));
	}

	private static byte[] mac(SecretKeySpec key, String value) {
		try {
			Mac mac = Mac.getInstance(MAC);
			mac.init(key);
			return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Could not hash the data", ex);
		}
	}

	private static byte[] decodeKey(String base64Key) {
		try {
			byte[] key = Base64.getDecoder().decode(base64Key);
			if (key.length == KEY_BYTES) {
				return key;
			}
		}
		catch (IllegalArgumentException ex) {
			// Reported below, without repeating the key in a log.
		}
		throw new IllegalStateException("The data key must be 32 bytes in base64; see docs/configuration.md");
	}
}
