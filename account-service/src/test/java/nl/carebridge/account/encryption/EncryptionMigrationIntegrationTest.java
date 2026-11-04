package nl.carebridge.account.encryption;

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

import nl.carebridge.account.TestAccounts;

/** Accounts from before encryption existed must keep working after the migration. */
@Testcontainers
@Tag("integration")
class EncryptionMigrationIntegrationTest {

	@Container
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	private final DataProtection protection = new DataProtection(TestAccounts.DATA_KEY);

	@Test
	void encryptsAnExistingAccountAndKeepsTheEmailFindable() throws SQLException {
		flyway().target("1").load().migrate();
		try (Connection connection = connect(); Statement statement = connection.createStatement()) {
			statement.execute("""
					INSERT INTO account (id, email, name, password_hash, created_at)
					VALUES ('7d1b6c2e-0000-4000-8000-000000000001', 'anna@example.com', 'Anna', '$2a$10$hash', now())
					""");
		}

		flyway().javaMigrations(new V2__EncryptPersonalData(protection)).load().migrate();

		try (Connection connection = connect(); Statement statement = connection.createStatement();
				ResultSet row = statement.executeQuery("SELECT * FROM account")) {
			assertThat(row.next()).isTrue();
			assertThat(row.getString("email_hash")).isEqualTo(protection.hash("anna@example.com"));
			assertThat(protection.decrypt(row.getString("email"))).isEqualTo("anna@example.com");
			assertThat(protection.decrypt(row.getString("name"))).isEqualTo("Anna");
			assertThat(row.getString("password_hash")).isEqualTo("$2a$10$hash");
		}
	}

	private static org.flywaydb.core.api.configuration.FluentConfiguration flyway() {
		return Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
	}

	private static Connection connect() throws SQLException {
		return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
	}
}
