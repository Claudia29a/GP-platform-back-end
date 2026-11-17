package nl.carebridge.messaging.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import nl.carebridge.messaging.client.IntakeClient;
import nl.carebridge.messaging.client.IntakeClient.Question;
import nl.carebridge.messaging.encryption.DataProtection;
import nl.carebridge.messaging.model.Reply;
import nl.carebridge.messaging.repository.ReplyRepository;
import nl.carebridge.messaging.security.CurrentUser;

/** The GP's replies. Whether a user may see a question is asked to the Intake Service every time. */
@Service
public class ReplyService {

	private final ReplyRepository repository;
	private final IntakeClient intakeClient;
	private final ReplyNotifier notifier;
	private final DataProtection protection;

	public ReplyService(ReplyRepository repository, IntakeClient intakeClient, ReplyNotifier notifier, DataProtection protection) {
		this.repository = repository;
		this.intakeClient = intakeClient;
		this.notifier = notifier;
		this.protection = protection;
	}

	@Transactional
	public Reply reply(UUID questionId, String text, CurrentUser gp, String authorization) {
		Question question = visibleQuestion(questionId, authorization);
		Reply reply = repository.save(new Reply(questionId, protection.hash(gp.id()), gp.name(), text.strip(), Instant.now()));
		notifier.replyArrived(question);
		return reply;
	}

	@Transactional(readOnly = true)
	public List<Reply> replies(UUID questionId, CurrentUser user, String authorization) {
		visibleQuestion(questionId, authorization);
		return repository.findAllByIntakeIdOrderByCreatedAtAsc(questionId);
	}

	private Question visibleQuestion(UUID questionId, String authorization) {
		return intakeClient.find(questionId, authorization)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question " + questionId + " not found"));
	}
}
