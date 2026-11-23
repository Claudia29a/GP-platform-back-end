package nl.carebridge.messaging.appointment;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * User story nr 10 TAC1 TAC2: which times can be booked. Slots run through each opening period on the opening days,
 * from tomorrow until the end of the booking window, in the practice's time zone; they are returned in UTC.
 */
public class SlotCalendar {

	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

	private record Period(LocalTime start, LocalTime end) {
	}

	private final AppointmentSettings settings;
	private final List<Period> periods;
	private final Clock clock;

	public SlotCalendar(AppointmentSettings settings, Clock clock) {
		this.settings = settings;
		this.periods = settings.hours().stream().map(SlotCalendar::period).toList();
		this.clock = clock;
		if (settings.slotLength().isZero() || settings.slotLength().isNegative() || settings.windowDays() < 1 || settings.days().isEmpty()) {
			throw new IllegalStateException("APPOINTMENT_DAYS, APPOINTMENT_SLOT_LENGTH and APPOINTMENT_WINDOW_DAYS must be filled in");
		}
	}

	/** Every bookable start time, oldest first. */
	public List<Instant> offered() {
		LocalDate today = LocalDate.now(clock.withZone(settings.timeZone()));
		List<Instant> slots = new ArrayList<>();
		for (LocalDate day = today.plusDays(1); !day.isAfter(today.plusDays(settings.windowDays())); day = day.plusDays(1)) {
			if (!settings.days().contains(day.getDayOfWeek())) {
				continue;
			}
			for (Period period : periods) {
				for (LocalTime time = period.start(); !time.plus(settings.slotLength()).isAfter(period.end())
						&& !time.plus(settings.slotLength()).isBefore(time); time = time.plus(settings.slotLength())) {
					slots.add(ZonedDateTime.of(day, time, settings.timeZone()).toInstant());
				}
			}
		}
		return slots;
	}

	public List<Instant> free(Collection<Instant> booked) {
		Set<Instant> taken = new HashSet<>(booked);
		return offered().stream().filter(slot -> !taken.contains(slot)).toList();
	}

	public boolean isOffered(Instant start) {
		return offered().contains(start);
	}

	public Instant earliest() {
		return ZonedDateTime.of(LocalDate.now(clock.withZone(settings.timeZone())).plusDays(1), LocalTime.MIDNIGHT, settings.timeZone()).toInstant();
	}

	/** Where a day starts in UTC, for the practice's list of one day. */
	public Instant dayStart(LocalDate day) {
		return day.atStartOfDay(settings.timeZone()).toInstant();
	}

	public String dayLabel(Instant start) {
		return DAY.format(start.atZone(settings.timeZone()));
	}

	public String timeLabel(Instant start) {
		return TIME.format(start.atZone(settings.timeZone()));
	}

	public Duration slotLength() {
		return settings.slotLength();
	}

	private static Period period(String text) {
		String[] parts = text.strip().split("-");
		try {
			if (parts.length == 2) {
				Period period = new Period(LocalTime.parse(parts[0].strip()), LocalTime.parse(parts[1].strip()));
				if (period.start().isBefore(period.end())) {
					return period;
				}
			}
		}
		catch (DateTimeParseException ex) {
			// Reported below with the setting's name.
		}
		throw new IllegalStateException("APPOINTMENT_HOURS must look like 08:00-12:00,13:00-17:00, but contains: " + text);
	}
}
