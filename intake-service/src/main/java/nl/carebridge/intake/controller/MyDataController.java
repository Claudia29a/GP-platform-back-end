package nl.carebridge.intake.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import nl.carebridge.intake.dto.MyDataResponse;
import nl.carebridge.intake.dto.RenameRequest;
import nl.carebridge.intake.security.CurrentUserResolver;
import nl.carebridge.intake.service.MyDataService;

/** "My data": the signed-in patient's own data, found from the token, never from the request. */
@RestController
public class MyDataController {

	private final MyDataService myDataService;
	private final CurrentUserResolver currentUserResolver;

	public MyDataController(MyDataService myDataService, CurrentUserResolver currentUserResolver) {
		this.myDataService = myDataService;
		this.currentUserResolver = currentUserResolver;
	}

	@GetMapping("/me/data")
	public MyDataResponse export(@AuthenticationPrincipal Jwt token) {
		return myDataService.export(currentUserResolver.resolve(token));
	}

	@PutMapping("/me/data/name")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void rename(@AuthenticationPrincipal Jwt token, @Valid @RequestBody RenameRequest request) {
		myDataService.rename(currentUserResolver.resolve(token), request.name());
	}

	@DeleteMapping("/me/data")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Jwt token) {
		myDataService.delete(currentUserResolver.resolve(token));
	}

	/** For the Messaging Service: the patient's question IDs, to export or delete their replies. */
	@GetMapping("/me/question-ids")
	public List<UUID> questionIds(@AuthenticationPrincipal Jwt token) {
		return myDataService.questionIds(currentUserResolver.resolve(token));
	}
}
