package nl.carebridge.account.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@Tag("unit")
class GatewayFilterTest {

	private final GatewayFilter filter = new GatewayFilter("the-gateway-secret", List.of("/actuator/health", "/.well-known/jwks.json"));

	private MockHttpServletResponse send(String path, String header) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
		if (header != null) {
			request.addHeader(GatewayFilter.HEADER, header);
		}
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());
		return response;
	}

	@Test
	void letsThroughRequestsFromTheWebsite() throws Exception {
		assertThat(send("/accounts/me", "the-gateway-secret").getStatus()).isEqualTo(200);
	}

	@Test
	void refusesRequestsThatDoNotComeThroughTheWebsite() throws Exception {
		MockHttpServletResponse response = send("/accounts/me", null);

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentAsString()).contains("only through the CareBridge website");
	}

	@Test
	void refusesAWrongSecret() throws Exception {
		assertThat(send("/accounts/me", "a-guess").getStatus()).isEqualTo(403);
		assertThat(send("/accounts/me", "").getStatus()).isEqualTo(403);
	}

	@Test
	void healthChecksWorkWithoutTheSecret() throws Exception {
		assertThat(send("/actuator/health/readiness", null).getStatus()).isEqualTo(200);
	}

	@Test
	void aPathThatOnlyStartsLikeAnOpenOneIsStillChecked() throws Exception {
		assertThat(send("/actuator/healthy-looking", null).getStatus()).isEqualTo(403);
	}

	@Test
	void theIntakeServiceReadsThePublicKeyDirectly() throws Exception {
		assertThat(send("/.well-known/jwks.json", null).getStatus()).isEqualTo(200);
	}
}
