package nl.carebridge.messaging.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@Tag("unit")
class IntakeClientTest {

	private static final UUID QUESTION = UUID.fromString("3f1c2a4e-0000-4000-8000-000000000001");

	private MockRestServiceServer intake;
	private IntakeClient client;

	@BeforeEach
	void createClient() {
		RestClient.Builder builder = RestClient.builder();
		intake = MockRestServiceServer.bindTo(builder).build();
		client = new IntakeClient(builder, "http://intake.test", "the-gateway-secret");
	}

	@Test
	void asksTheIntakeServiceWithTheUsersOwnTokenAndTheWebsiteSecret() {
		intake.expect(requestTo("http://intake.test/intakes/" + QUESTION + "/recipient"))
				.andExpect(header("Authorization", "Bearer user.token"))
				.andExpect(header("X-CareBridge-Gateway", "the-gateway-secret"))
				.andRespond(withSuccess("""
						{"id": "%s", "patientName": "Anna de Vries", "patientEmail": "anna@example.com", "description": "Rash"}
						""".formatted(QUESTION), MediaType.APPLICATION_JSON));

		assertThat(client.find(QUESTION, "Bearer user.token")).hasValueSatisfying(question -> {
			assertThat(question.patientName()).isEqualTo("Anna de Vries");
			assertThat(question.patientEmail()).isEqualTo("anna@example.com");
		});
		intake.verify();
	}

	@Test
	void aQuestionTheUserMayNotSeeIsNotFound() {
		intake.expect(requestTo("http://intake.test/intakes/" + QUESTION + "/recipient")).andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThat(client.find(QUESTION, "Bearer user.token")).isEmpty();
	}
}
