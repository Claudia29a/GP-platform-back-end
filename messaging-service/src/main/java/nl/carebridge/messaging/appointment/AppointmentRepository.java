package nl.carebridge.messaging.appointment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

	Optional<Appointment> findByReplyId(UUID replyId);

	List<Appointment> findAllByReplyIdIn(Collection<UUID> replyIds);

	List<Appointment> findAllByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(Instant from, Instant until);

	/** The booked start times from this moment on, to leave out of the free times. */
	@Query("select a.startsAt from Appointment a where a.startsAt >= :from")
	List<Instant> bookedFrom(@Param("from") Instant from);
}
