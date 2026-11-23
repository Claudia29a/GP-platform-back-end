package nl.carebridge.messaging.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** User story nr 10 TAC1 TAC2: the free times come from the opening hours in the practice's time zone. */
@Tag("unit")
class SlotCalendarTest {

	private static final ZoneId AMSTERDAM = ZoneId.of("Europe/Amsterdam");
	/** Monday 30 November 2026, 15:00 in Amsterdam. */
	private static final Instant MONDAY_AFTERNOON = Instant.parse("2026-11-30T14:00:00Z");

	static AppointmentSettings settings() {
		return new AppointmentSettings(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
				List.of("08:00-12:00", "13:00-17:00"), Duration.ofMinutes(15), 14, AMSTERDAM);
	}

	private static SlotCalendar calendarAt(Instant now) {
		return new SlotCalendar(settings(), Clock.fixed(now, AMSTERDAM));
	}

	@Test
	void offersFifteenMinuteSlotsFromTomorrowDuringOpeningHoursOnly() {
		List<Instant> offered = calendarAt(MONDAY_AFTERNOON).offered();

		// Tuesday 1 December, 08:00 in Amsterdam is 07:00 UTC (winter time).
		assertThat(offered.getFirst()).isEqualTo(Instant.parse("2026-12-01T07:00:00Z"));
		assertThat(offered).doesNotContain(Instant.parse("2026-11-30T15:00:00Z"));
		assertThat(offered).contains(Instant.parse("2026-12-01T10:45:00Z"), Instant.parse("2026-12-01T12:00:00Z"))
				.doesNotContain(Instant.parse("2026-12-01T11:00:00Z"), Instant.parse("2026-12-01T16:00:00Z"));
		// 16 slots per opening period, two periods a day, ten working days in the next 14 days.
		assertThat(offered).hasSize(16 * 2 * 10);
		assertThat(offered.getLast()).isEqualTo(Instant.parse("2026-12-14T15:45:00Z"));
	}

	@Test
	void neverOffersAWeekend() {
		List<Instant> offered = calendarAt(MONDAY_AFTERNOON).offered();

		assertThat(offered).noneMatch(start -> java.util.EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
				.contains(start.atZone(AMSTERDAM).getDayOfWeek()));
	}

	@Test
	void usesSummerTimeWhenTheClocksHaveGoneForward() {
		// Monday 29 March 2027: summer time since Sunday, so 08:00 in Amsterdam is 06:00 UTC.
		assertThat(calendarAt(Instant.parse("2027-03-29T10:00:00Z")).offered().getFirst()).isEqualTo(Instant.parse("2027-03-30T06:00:00Z"));
	}

	@Test
	void leavesBookedTimesOut() {
		SlotCalendar calendar = calendarAt(MONDAY_AFTERNOON);
		Instant taken = Instant.parse("2026-12-01T07:15:00Z");

		assertThat(calendar.free(Set.of(taken))).doesNotContain(taken).hasSize(calendar.offered().size() - 1);
	}

	@Test
	void onlyAnOfferedTimeCanBeBooked() {
		SlotCalendar calendar = calendarAt(MONDAY_AFTERNOON);

		assertThat(calendar.isOffered(Instant.parse("2026-12-01T07:15:00Z"))).isTrue();
		assertThat(calendar.isOffered(Instant.parse("2026-12-01T07:20:00Z"))).as("not on a slot boundary").isFalse();
		assertThat(calendar.isOffered(Instant.parse("2026-11-30T15:00:00Z"))).as("today").isFalse();
		assertThat(calendar.isOffered(Instant.parse("2026-12-05T09:00:00Z"))).as("Saturday").isFalse();
		assertThat(calendar.isOffered(Instant.parse("2026-12-15T07:00:00Z"))).as("after the window").isFalse();
	}

	@Test
	void namesTheDayAndTimeInThePracticesTimeZone() {
		SlotCalendar calendar = calendarAt(MONDAY_AFTERNOON);

		assertThat(calendar.dayLabel(Instant.parse("2026-12-01T09:15:00Z"))).isEqualTo("Tuesday 1 December 2026");
		assertThat(calendar.timeLabel(Instant.parse("2026-12-01T09:15:00Z"))).isEqualTo("10:15");
		assertThat(calendar.dayStart(java.time.LocalDate.of(2026, 12, 1))).isEqualTo(Instant.parse("2026-11-30T23:00:00Z"));
	}

	@Test
	void refusesToStartWithOpeningHoursItCannotRead() {
		for (String hours : new String[] {"8-12", "12:00-08:00", "08:00-08:00", "08:00"}) {
			AppointmentSettings wrong = new AppointmentSettings(settings().days(), List.of(hours), Duration.ofMinutes(15), 14, AMSTERDAM);
			assertThatThrownBy(() -> new SlotCalendar(wrong, Clock.systemUTC())).as(hours)
					.isInstanceOf(IllegalStateException.class).hasMessageContaining("APPOINTMENT_HOURS");
		}
	}
}
