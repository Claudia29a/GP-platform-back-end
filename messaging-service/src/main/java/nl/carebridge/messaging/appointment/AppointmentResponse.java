package nl.carebridge.messaging.appointment;

import java.time.Instant;
import java.util.UUID;

/** User story nr 10: a booked appointment, with its day and time as the practice's clock shows them. */
public record AppointmentResponse(UUID id, UUID questionId, Instant start, String day, String time, String patientName) {

	public static AppointmentResponse from(Appointment appointment, SlotCalendar calendar) {
		return new AppointmentResponse(appointment.getId(), appointment.getIntakeId(), appointment.getStartsAt(),
				calendar.dayLabel(appointment.getStartsAt()), calendar.timeLabel(appointment.getStartsAt()), appointment.getPatientName());
	}
}
