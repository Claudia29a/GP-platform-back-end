package nl.carebridge.account.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import nl.carebridge.account.service.SigningKey;

/** Publishes the public key, so other services can check the signature of our tokens. */
@RestController
public class JwksController {

	private final SigningKey signingKey;

	public JwksController(SigningKey signingKey) {
		this.signingKey = signingKey;
	}

	@GetMapping("/.well-known/jwks.json")
	public Map<String, Object> keys() {
		return signingKey.publicKeys().toJSONObject(true);
	}
}
