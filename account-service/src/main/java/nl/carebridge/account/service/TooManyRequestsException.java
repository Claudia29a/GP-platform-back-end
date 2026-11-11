package nl.carebridge.account.service;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 429 Too Many Requests, with a Retry-After header that says how many seconds to wait. */
public class TooManyRequestsException extends ResponseStatusException {

	private final Duration retryAfter;

	public TooManyRequestsException(String reason, Duration retryAfter) {
		super(HttpStatus.TOO_MANY_REQUESTS, reason);
		this.retryAfter = retryAfter;
	}

	public Duration getRetryAfter() {
		return retryAfter;
	}

	@Override
	public HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		// Rounded up, so a client that waits this long is never too early.
		headers.set(HttpHeaders.RETRY_AFTER, String.valueOf((retryAfter.toMillis() + 999) / 1000));
		return headers;
	}
}
