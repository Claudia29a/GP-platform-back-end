package nl.carebridge.intake.gateway;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Accepts only requests that came through the CareBridge website: its nginx adds a secret header to every request
 * it forwards. A request straight from the internet does not have it and gets 403. Health checks and other open
 * paths work without it. The comparison takes the same time for every wrong guess.
 */
public class GatewayFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-CareBridge-Gateway";

	private final byte[] secret;
	private final List<String> openPaths;

	public GatewayFilter(String secret, List<String> openPaths) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("carebridge.gateway.secret is required; see docs/configuration.md");
		}
		this.secret = secret.getBytes(StandardCharsets.UTF_8);
		this.openPaths = List.copyOf(openPaths);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (isOpen(request.getRequestURI()) || fromWebsite(request.getHeader(HEADER))) {
			chain.doFilter(request, response);
			return;
		}
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType("application/json");
		response.getWriter().write("{\"error\":\"This service can be used only through the CareBridge website\"}");
	}

	private boolean isOpen(String path) {
		return openPaths.stream().anyMatch(open -> path.equals(open) || path.startsWith(open + "/"));
	}

	private boolean fromWebsite(String header) {
		return header != null && MessageDigest.isEqual(secret, header.getBytes(StandardCharsets.UTF_8));
	}
}
