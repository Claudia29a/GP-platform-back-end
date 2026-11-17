package nl.carebridge.messaging.gateway;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** Runs the gateway check before everything else, including sign-in checks. Health checks stay open for the platform. */
@Configuration
public class GatewayConfig {

	@Bean
	FilterRegistrationBean<GatewayFilter> gatewayFilter(@Value("${carebridge.gateway.secret}") String secret) {
		FilterRegistrationBean<GatewayFilter> registration =
				new FilterRegistrationBean<>(new GatewayFilter(secret, List.of("/actuator/health")));
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}
}
