package nl.carebridge.messaging.client;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Asks the Intake Service about a question, with the user's own sign-in token. Who may see which question is
 * decided there and nowhere else: when the Intake Service does not show the question to this user, neither do we.
 */
@Component
public class IntakeClient {

	/** To whom the notification goes; the Intake Service sends no health details here. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Question(UUID id, String patientName, String patientEmail, Instant submittedAt) {
	}

	private final RestClient intake;
	private final String gatewaySecret;

	public IntakeClient(RestClient.Builder builder, @Value("${carebridge.intake.api-url}") String intakeApiUrl,
			@Value("${carebridge.gateway.secret}") String gatewaySecret) {
		this.intake = builder.baseUrl(intakeApiUrl).build();
		this.gatewaySecret = gatewaySecret;
	}

	/** The IDs of the signed-in patient's own questions; never their health details. */
	public List<UUID> myQuestionIds(String authorization) {
		UUID[] ids = intake.get().uri("/me/question-ids")
				.header("Authorization", authorization)
				.header("X-CareBridge-Gateway", gatewaySecret)
				.retrieve()
				.body(UUID[].class);
		return ids == null ? List.of() : Arrays.asList(ids);
	}

	public Optional<Question> find(UUID questionId, String authorization) {
		try {
			return Optional.ofNullable(intake.get().uri("/intakes/{id}/recipient", questionId)
					.header("Authorization", authorization)
					// The Intake Service only accepts requests that come through the website or carry its secret.
					.header("X-CareBridge-Gateway", gatewaySecret)
					.retrieve()
					.body(Question.class));
		}
		catch (HttpClientErrorException ex) {
			if (ex.getStatusCode() == HttpStatus.NOT_FOUND || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
				return Optional.empty();
			}
			throw ex;
		}
	}
}
