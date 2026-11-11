package nl.carebridge.intake.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import nl.carebridge.intake.dto.MyDataResponse;
import nl.carebridge.intake.security.CurrentUserResolver;
import nl.carebridge.intake.service.MyDataService;

/** "My data": the signed-in patient's own data, found from the token, never from the request. */
@RestController
@RequestMapping("/me/data")
public class MyDataController {

	private final MyDataService myDataService;
	private final CurrentUserResolver currentUserResolver;

	public MyDataController(MyDataService myDataService, CurrentUserResolver currentUserResolver) {
		this.myDataService = myDataService;
		this.currentUserResolver = currentUserResolver;
	}

	@GetMapping
	public MyDataResponse export(@AuthenticationPrincipal Jwt token) {
		return myDataService.export(currentUserResolver.resolve(token));
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Jwt token) {
		myDataService.delete(currentUserResolver.resolve(token));
	}
}
