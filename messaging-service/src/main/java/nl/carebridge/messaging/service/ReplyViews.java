package nl.carebridge.messaging.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import nl.carebridge.messaging.appointment.Appointment;
import nl.carebridge.messaging.appointment.AppointmentRepository;
import nl.carebridge.messaging.appointment.AppointmentResponse;
import nl.carebridge.messaging.appointment.SlotCalendar;
import nl.carebridge.messaging.dto.ReplyResponse;
import nl.carebridge.messaging.model.Reply;

/** Turns replies into what the screens and My data show, with the link and any booked appointment (User story nr 9, 10). */
@Component
public class ReplyViews {

	private final AppointmentLink link;
	private final AppointmentRepository appointments;
	private final SlotCalendar calendar;

	public ReplyViews(AppointmentLink link, AppointmentRepository appointments, SlotCalendar calendar) {
		this.link = link;
		this.appointments = appointments;
		this.calendar = calendar;
	}

	@Transactional(readOnly = true)
	public List<ReplyResponse> of(List<Reply> replies) {
		List<UUID> invitations = replies.stream().filter(Reply::isAppointment).map(Reply::getId).toList();
		Map<UUID, Appointment> booked = invitations.isEmpty() ? Map.of()
				: appointments.findAllByReplyIdIn(invitations).stream().collect(Collectors.toMap(Appointment::getReplyId, Function.identity()));
		return replies.stream().map(reply -> view(reply, reply.getId() == null ? null : booked.get(reply.getId()))).toList();
	}

	public ReplyResponse of(Reply reply) {
		return view(reply, null);
	}

	private ReplyResponse view(Reply reply, Appointment appointment) {
		return new ReplyResponse(reply.getId(), reply.getIntakeId(), reply.getAuthorName(), reply.getText(), reply.getCreatedAt(),
				reply.isAppointment(), reply.isAppointment() ? link.urlFor(reply.getId()) : null,
				appointment == null ? null : AppointmentResponse.from(appointment, calendar));
	}
}
