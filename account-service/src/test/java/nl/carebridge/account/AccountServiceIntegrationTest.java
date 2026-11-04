package nl.carebridge.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import nl.carebridge.account.encryption.DataProtection;
import nl.carebridge.account.repository.AccountRepository;

/**
 * Sign-up, sign-in and the published key together, against a real PostgreSQL with the Flyway schema.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Tag("integration")
class AccountServiceIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AccountRepository repository;

	@Autowired
	private DataProtection protection;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void signUpThenSignInGivesATokenThatThePublishedKeyVerifies() throws Exception {
		mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"Bram\", \"email\": \"Bram@Example.com\", \"password\": \"Secret12\"}"))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"Bram again\", \"email\": \"bram@example.COM\", \"password\": \"Secret12\"}"))
				.andExpect(status().isConflict());

		String login = mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"bram@example.com\", \"password\": \"Secret12\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String jwks = mockMvc.perform(get("/.well-known/jwks.json")).andReturn().getResponse().getContentAsString();

		RSAKey publicKey = (RSAKey) JWKSet.parse(jwks).getKeys().getFirst();
		Jwt jwt = NimbusJwtDecoder.withPublicKey(publicKey.toRSAPublicKey()).build().decode(JsonPath.read(login, "$.token"));
		assertThat(jwt.getClaimAsString("email")).isEqualTo("bram@example.com");
		assertThat(repository.findByEmailHash(protection.hash("bram@example.com"))).get()
				.satisfies(account -> assertThat(account.getPasswordHash()).doesNotContain("Secret12"));

		Map<String, Object> row = jdbc.queryForMap("SELECT email, name, email_hash, password_hash FROM account");
		assertThat(row.values()).allSatisfy(column -> assertThat(column.toString()).doesNotContain("bram", "Bram", "Secret12"));
		assertThat(row.get("password_hash").toString()).startsWith("$2a$");
	}

	@Test
	void healthIsUp() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}
}
