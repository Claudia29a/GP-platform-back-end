package nl.carebridge.account.encryption;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

/**
 * Encrypts the accounts that already exist. The address itself can no longer be searched, so a hash of
 * it takes over that job. This migration is written in Java because it needs the data key; Flyway
 * reads the version from the class name, so the name does not follow Java's style.
 */
@Component
@SuppressWarnings("java:S101")
public class V2__EncryptPersonalData extends BaseJavaMigration {

	private static final String PREPARE_COLUMNS = """
			DROP INDEX uq_account_email;
			ALTER TABLE account
			    ALTER COLUMN email TYPE TEXT,
			    ALTER COLUMN name TYPE TEXT,
			    ADD COLUMN email_hash VARCHAR(64);
			""";

	private static final String FINISH_COLUMNS = """
			ALTER TABLE account ALTER COLUMN email_hash SET NOT NULL;
			CREATE UNIQUE INDEX uq_account_email_hash ON account (email_hash);
			""";

	private final DataProtection protection;

	public V2__EncryptPersonalData(DataProtection protection) {
		this.protection = protection;
	}

	@Override
	public void migrate(Context context) throws SQLException {
		Connection connection = context.getConnection();
		try (Statement statement = connection.createStatement()) {
			statement.execute(PREPARE_COLUMNS);
		}
		try (Statement select = connection.createStatement();
				ResultSet row = select.executeQuery("SELECT id, email, name FROM account");
				PreparedStatement update = connection.prepareStatement("UPDATE account SET email = ?, name = ?, email_hash = ? WHERE id = ?")) {
			while (row.next()) {
				String email = row.getString("email");
				update.setString(1, protection.encrypt(email));
				update.setString(2, protection.encrypt(row.getString("name")));
				update.setString(3, protection.hash(email));
				update.setObject(4, row.getObject("id", UUID.class));
				update.executeUpdate();
			}
		}
		try (Statement statement = connection.createStatement()) {
			statement.execute(FINISH_COLUMNS);
		}
	}
}
