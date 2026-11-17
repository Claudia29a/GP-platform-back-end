package nl.carebridge.messaging.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nl.carebridge.messaging.model.Reply;

public interface ReplyRepository extends JpaRepository<Reply, UUID> {

	List<Reply> findAllByIntakeIdOrderByCreatedAtAsc(UUID intakeId);

	List<Reply> findAllByIntakeIdInOrderByCreatedAtAsc(Collection<UUID> intakeIds);

	@Modifying
	@Query("delete from Reply r where r.intakeId in :intakeIds")
	int deleteAllByIntakeIdIn(@Param("intakeIds") Collection<UUID> intakeIds);

	@Modifying
	@Query("delete from Reply r where r.questionSubmittedAt < :before")
	int deleteQuestionSubmittedBefore(@Param("before") Instant before);
}
