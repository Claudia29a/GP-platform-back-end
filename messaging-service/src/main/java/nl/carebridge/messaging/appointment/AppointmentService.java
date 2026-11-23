package nl.carebridge.messaging.appointment;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
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
import nl.carebridge.messaging.service.ReplyNotifier;

/**
 * User story nr 10: patients book and cancel appointments after the GP's invitation. Whether a patient owns the
 * question is asked to the Intake Service with their own sign-in, as for replies.
 */
@Service
public class AppointmentService {

	static final String TAKEN = "This time was just taken. Please choose another.";

	private final AppointmentRepository appointments;
	private final ReplyRepository replies;
	private final IntakeClient intakeClient;
	private final ReplyNotifier notifier;
	private final DataProtection protection;
	private final SlotCalendar calendar;
	private final Clock clock;

	public AppointmentService(AppointmentRepository appointments, ReplyRepository replies, IntakeClient intakeClient, ReplyNotifier notifier,
			DataProtection protection, SlotCalendar calendar, Clock clock) {
		this.appointments = appointments;
		this.replies = replies;
		this.intakeClient = intakeClient;
		this.notifier = notifier;
		this.protection = protection;
		this.calendar = calendar;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<Instant> free() {
		return calendar.free(appointments.bookedFrom(calendar.earliest()));
	}

	/** One appointment per invitation and per time; the database decides when two patients pick a time together. */
	@Transactional
	public Appointment book(UUID replyId, Instant start, CurrentUser patient, String authorization) {
		Reply invitation = replies.findById(replyId).filter(Reply::isAppointment)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No invitation to make an appointment was found."));
		Question question = intakeClient.find(invitation.getIntakeId(), authorization)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No invitation to make an appointment was found."));
		if (appointments.findByReplyId(replyId).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an appointment for this invitation.");
		}
		if (!calendar.isOffered(start)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This time cannot be booked. Please choose one of the free times.");
		}
		Appointment appointment;
		try {
			appointment = appointments.saveAndFlush(new Appointment(replyId, invitation.getIntakeId(), protection.hash(patient.id()),
					question.patientName(), start, Instant.now(clock)));
		}
		catch (DataIntegrityViolationException taken) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, TAKEN);
		}
		notifier.appointmentBooked(question, calendar.dayLabel(start), calendar.timeLabel(start));
		return appointment;
	}

	/** Only the patient who booked, and only before the appointment starts; the time is free again straight away. */
	@Transactional
	public void cancel(UUID id, CurrentUser patient, String authorization) {
		Appointment appointment = appointments.findById(id).filter(found -> found.getPatientHash().equals(protection.hash(patient.id())))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found."));
		if (!appointment.getStartsAt().isAfter(Instant.now(clock))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This appointment has already started and can no longer be cancelled.");
		}
		appointments.delete(appointment);
		intakeClient.find(appointment.getIntakeId(), authorization).ifPresent(question -> notifier.appointmentCancelled(question,
				calendar.dayLabel(appointment.getStartsAt()), calendar.timeLabel(appointment.getStartsAt())));
	}

	/** The practice's list of one day, in the practice's time zone. */
	@Transactional(readOnly = true)
	public List<Appointment> onDay(LocalDate day) {
		return appointments.findAllByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(calendar.dayStart(day),
				calendar.dayStart(day.plusDays(1)));
	}
}
