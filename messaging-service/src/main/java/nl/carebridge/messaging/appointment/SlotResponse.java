package nl.carebridge.messaging.appointment;

import java.time.Instant;

/** User story nr 10: one free time, with its day and time as the practice's clock shows them. */
public record SlotResponse(Instant start, String day, String time) {

	public static SlotResponse from(Instant start, SlotCalendar calendar) {
		return new SlotResponse(start, calendar.dayLabel(start), calendar.timeLabel(start));
	}
}
