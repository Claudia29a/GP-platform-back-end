package nl.carebridge.intake.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import nl.carebridge.intake.TestIntakes;

/** Questions stored before encryption existed must survive the migration, readable with the key only. */
@Testcontainers
@Tag("integration")
class EncryptionMigrationIntegrationTest {

	@Container
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	private final DataProtection protection = new DataProtection(TestIntakes.DATA_KEY);

	@Test
	void encryptsAnExistingQuestionAndHashesItsOwner() throws SQLException {
		flyway().target("5").load().migrate();
		try (Connection connection = connect(); Statement statement = connection.createStatement()) {
			statement.execute("""
					INSERT INTO intake (id, description, status, submitted_at, patient_id, patient_name, patient_email,
					    complaint_area, duration, severity, symptoms, takes_medication, current_medication,
					    tried_treatment, severity_score, queue_position)
					VALUES ('3f1c2a4e-0000-4000-8000-000000000001', 'Itchy rash', 'IN_REVIEW', now(), 'google-anna', 'Anna',
					    'anna@example.com', 'SKIN', 'ONE_TO_THREE_DAYS', 4, '["FEVER", "COUGH"]', true, 'Inhaler', false, 37, 1)
					""");
		}

		flyway().javaMigrations(new V6__EncryptPersonalData(protection)).load().migrate();

		try (Connection connection = connect(); Statement statement = connection.createStatement();
				ResultSet row = statement.executeQuery("SELECT * FROM intake")) {
			assertThat(row.next()).isTrue();
			assertThat(row.getString("patient_id")).isEqualTo(protection.hash("google-anna"));
			assertThat(protection.decrypt(row.getString("description"))).isEqualTo("Itchy rash");
			assertThat(protection.decrypt(row.getString("patient_name"))).isEqualTo("Anna");
			assertThat(protection.decrypt(row.getString("patient_email"))).isEqualTo("anna@example.com");
			assertThat(protection.decrypt(row.getString("complaint_area"))).isEqualTo("SKIN");
			assertThat(protection.decrypt(row.getString("severity"))).isEqualTo("4");
			assertThat(protection.decrypt(row.getString("symptoms"))).isEqualTo("FEVER,COUGH");
			assertThat(protection.decrypt(row.getString("takes_medication"))).isEqualTo("true");
			assertThat(protection.decrypt(row.getString("current_medication"))).isEqualTo("Inhaler");
			assertThat(row.getString("treatment_tried")).isNull();
			assertThat(row.getInt("severity_score")).isEqualTo(37);
		}
	}

	private static org.flywaydb.core.api.configuration.FluentConfiguration flyway() {
		return Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
	}

	private static Connection connect() throws SQLException {
		return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
	}
}
