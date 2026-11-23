package nl.carebridge.messaging.appointment;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * User story nr 10 TAC1: when patients can book, from the settings (carebridge.appointments.*), never from code.
 *
 * @param days the opening days (APPOINTMENT_DAYS)
 * @param hours the opening periods of each day, such as 08:00-12:00 (APPOINTMENT_HOURS)
 * @param slotLength how long one appointment is (APPOINTMENT_SLOT_LENGTH)
 * @param windowDays how many days ahead, from tomorrow, can be booked (APPOINTMENT_WINDOW_DAYS)
 * @param timeZone the practice's time zone; times are stored in UTC (PRACTICE_TIME_ZONE)
 */
@ConfigurationProperties("carebridge.appointments")
public record AppointmentSettings(List<DayOfWeek> days, List<String> hours, Duration slotLength, int windowDays, ZoneId timeZone) {
}
