package nl.carebridge.intake.encryption;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import nl.carebridge.intake.model.ComplaintArea;
import nl.carebridge.intake.model.ComplaintDuration;
import nl.carebridge.intake.model.Symptom;

/** Encrypt a field on its way to the database and decrypt it on the way back, one converter per field type. */
public final class EncryptedConverters {

	private EncryptedConverters() {
	}

	abstract static class Encrypted<T> implements AttributeConverter<T, String> {

		private final DataProtection protection;

		Encrypted(DataProtection protection) {
			this.protection = protection;
		}

		abstract String toText(T value);

		abstract T fromText(String text);

		@Override
		public String convertToDatabaseColumn(T value) {
			return value == null ? null : protection.encrypt(toText(value));
		}

		@Override
		public T convertToEntityAttribute(String stored) {
			return stored == null ? null : fromText(protection.decrypt(stored));
		}
	}

	@Component
	public static class Text extends Encrypted<String> {

		public Text(DataProtection protection) {
			super(protection);
		}

		@Override
		String toText(String value) {
			return value;
		}

		@Override
		String fromText(String text) {
			return text;
		}
	}

	@Component
	public static class Number extends Encrypted<Integer> {

		public Number(DataProtection protection) {
			super(protection);
		}

		@Override
		String toText(Integer value) {
			return value.toString();
		}

		@Override
		Integer fromText(String text) {
			return Integer.valueOf(text);
		}
	}

	@Component
	public static class YesNo extends Encrypted<Boolean> {

		public YesNo(DataProtection protection) {
			super(protection);
		}

		@Override
		String toText(Boolean value) {
			return value.toString();
		}

		@Override
		Boolean fromText(String text) {
			return Boolean.valueOf(text);
		}
	}

	@Component
	public static class Area extends Encrypted<ComplaintArea> {

		public Area(DataProtection protection) {
			super(protection);
		}

		@Override
		String toText(ComplaintArea value) {
			return value.name();
		}

		@Override
		ComplaintArea fromText(String text) {
			return ComplaintArea.valueOf(text);
		}
	}

	@Component
	public static class Duration extends Encrypted<ComplaintDuration> {

		public Duration(DataProtection protection) {
			super(protection);
		}

		@Override
		String toText(ComplaintDuration value) {
			return value.name();
		}

		@Override
		ComplaintDuration fromText(String text) {
			return ComplaintDuration.valueOf(text);
		}
	}

	@Component
	public static class Symptoms extends Encrypted<Set<Symptom>> {

		public Symptoms(DataProtection protection) {
			super(protection);
		}

		@Override
		String toText(Set<Symptom> value) {
			return value.stream().map(Symptom::name).collect(Collectors.joining(","));
		}

		@Override
		Set<Symptom> fromText(String text) {
			return text.isEmpty() ? EnumSet.noneOf(Symptom.class)
					: Arrays.stream(text.split(",")).map(Symptom::valueOf).collect(Collectors.toCollection(() -> EnumSet.noneOf(Symptom.class)));
		}
	}
}
