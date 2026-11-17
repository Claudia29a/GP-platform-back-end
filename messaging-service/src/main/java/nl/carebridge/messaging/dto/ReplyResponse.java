package nl.carebridge.messaging.dto;

import java.time.Instant;
import java.util.UUID;

import nl.carebridge.messaging.model.Reply;

/** A reply as patients and staff read it: never the hash of the GP's account. */
public record ReplyResponse(UUID id, UUID questionId, String authorName, String text, Instant createdAt) {

	public static ReplyResponse from(Reply reply) {
		return new ReplyResponse(reply.getId(), reply.getIntakeId(), reply.getAuthorName(), reply.getText(), reply.getCreatedAt());
	}
}
