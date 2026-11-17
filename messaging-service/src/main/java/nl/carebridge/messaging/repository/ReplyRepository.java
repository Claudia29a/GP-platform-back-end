package nl.carebridge.messaging.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import nl.carebridge.messaging.model.Reply;

public interface ReplyRepository extends JpaRepository<Reply, UUID> {

	List<Reply> findAllByIntakeIdOrderByCreatedAtAsc(UUID intakeId);
}
