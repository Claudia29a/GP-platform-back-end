package nl.carebridge.intake.encryption;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

/**
 * Encrypts the questions that already exist. This migration is written in Java because it needs the
 * data key; Flyway reads the version from the class name, so the name does not follow Java's style.
 */
@Component
@SuppressWarnings("java:S101")
public class V6__EncryptPersonalData extends BaseJavaMigration {

	private static final List<String> ENCRYPTED_COLUMNS = List.of("patient_name", "patient_email", "complaint_area",
			"complaint_area_other", "duration", "severity", "symptoms", "symptoms_other", "takes_medication",
			"current_medication", "tried_treatment", "treatment_tried", "description");

	private static final String MAKE_COLUMNS_TEXT = """
			ALTER TABLE intake
			    DROP CONSTRAINT intake_severity_check,
			    ALTER COLUMN patient_name TYPE TEXT,
			    ALTER COLUMN patient_email TYPE TEXT,
			    ALTER COLUMN complaint_area TYPE TEXT,
			    ALTER COLUMN complaint_area_other TYPE TEXT,
			    ALTER COLUMN duration TYPE TEXT,
			    ALTER COLUMN severity TYPE TEXT USING severity::text,
			    ALTER COLUMN symptoms TYPE TEXT USING symptoms::text,
			    ALTER COLUMN symptoms_other TYPE TEXT,
			    ALTER COLUMN takes_medication TYPE TEXT USING takes_medication::text,
			    ALTER COLUMN current_medication TYPE TEXT,
			    ALTER COLUMN tried_treatment TYPE TEXT USING tried_treatment::text,
			    ALTER COLUMN treatment_tried TYPE TEXT,
			    ALTER COLUMN description TYPE TEXT
			""";

	private static final String UPDATE_ROW = """
			UPDATE intake SET patient_name = ?, patient_email = ?, complaint_area = ?, complaint_area_other = ?,
			    duration = ?, severity = ?, symptoms = ?, symptoms_other = ?, takes_medication = ?,
			    current_medication = ?, tried_treatment = ?, treatment_tried = ?, description = ?, patient_id = ?
			WHERE id = ?
			""";

	private final DataProtection protection;

	public V6__EncryptPersonalData(DataProtection protection) {
		this.protection = protection;
	}

	@Override
	public void migrate(Context context) throws SQLException {
		Connection connection = context.getConnection();
		try (Statement statement = connection.createStatement()) {
			statement.execute(MAKE_COLUMNS_TEXT);
		}
		try (Statement select = connection.createStatement();
				ResultSet row = select.executeQuery("SELECT * FROM intake");
				PreparedStatement update = connection.prepareStatement(UPDATE_ROW)) {
			while (row.next()) {
				int parameter = 1;
				for (String column : ENCRYPTED_COLUMNS) {
					update.setString(parameter++, encrypted(column, row.getString(column)));
				}
				update.setString(parameter++, protection.hash(row.getString("patient_id")));
				update.setObject(parameter, row.getObject("id", UUID.class));
				update.executeUpdate();
			}
		}
	}

	private String encrypted(String column, String value) {
		if (value == null) {
			return null;
		}
		// Symptoms were a JSON array and become the plain list the converter reads.
		return protection.encrypt("symptoms".equals(column) ? value.replaceAll("[\\[\\]\" ]", "") : value);
	}
}
