package nl.carebridge.messaging.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Scheduling runs the clean-up of replies whose question passed the retention period. */
@Configuration
@EnableScheduling
public class AppConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
