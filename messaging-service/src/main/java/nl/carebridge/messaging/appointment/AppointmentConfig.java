package nl.carebridge.messaging.appointment;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** User story nr 10: reads the opening hours once, at start-up; wrong values stop the service from starting. */
@Configuration
@EnableConfigurationProperties(AppointmentSettings.class)
public class AppointmentConfig {

	@Bean
	SlotCalendar slotCalendar(AppointmentSettings settings, Clock clock) {
		return new SlotCalendar(settings, clock);
	}
}
