package nl.carebridge.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

	/** Catches the emails, so the test can read the code like a user would. */
	@MockitoBean
	private JavaMailSender mailSender;

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

		Map<String, Object> row = jdbc.queryForMap("SELECT email, name, email_hash, password_hash FROM account WHERE email_hash = ?",
				protection.hash("bram@example.com"));
		assertThat(row.values()).allSatisfy(column -> assertThat(column.toString()).doesNotContain("bram", "Bram", "Secret12"));
		assertThat(row.get("password_hash").toString()).startsWith("$2a$");
	}

	@Test
	void theEmailedCodeVerifiesTheAccountAndOnlyItsHashIsStored() throws Exception {
		mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"Cleo\", \"email\": \"cleo@example.com\", \"password\": \"Secret12\"}"))
				.andExpect(status().isCreated());
		org.mockito.ArgumentCaptor<SimpleMailMessage> mail = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(mail.capture());
		Matcher code = Pattern.compile("\\b(\\d{6})\\b").matcher(mail.getValue().getText());
		assertThat(code.find()).isTrue();
		assertThat(jdbc.queryForObject("SELECT verification_code_hash FROM account WHERE email_hash = ?", String.class,
				protection.hash("cleo@example.com"))).isNotNull().doesNotContain(code.group(1));

		String verified = mockMvc.perform(post("/accounts/verify").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"cleo@example.com\", \"code\": \"" + code.group(1) + "\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String jwks = mockMvc.perform(get("/.well-known/jwks.json")).andReturn().getResponse().getContentAsString();
		RSAKey publicKey = (RSAKey) JWKSet.parse(jwks).getKeys().getFirst();
		Jwt jwt = NimbusJwtDecoder.withPublicKey(publicKey.toRSAPublicKey()).build().decode(JsonPath.read(verified, "$.token"));
		assertThat(jwt.getClaimAsBoolean("email_verified")).isTrue();
	}

	@Test
	void healthIsUp() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}
}
