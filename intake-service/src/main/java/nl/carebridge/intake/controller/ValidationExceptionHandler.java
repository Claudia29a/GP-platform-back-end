package nl.carebridge.intake.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import nl.carebridge.intake.dto.ValidationErrorResponse;
import tools.jackson.databind.DatabindException;

/** Story 3 TAC2: invalid input gets 400 with an error for each field. */
@RestControllerAdvice
class ValidationExceptionHandler {

	private static final String MESSAGE = "Some answers are missing or not valid";

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ValidationErrorResponse invalidAnswers(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		return new ValidationErrorResponse(MESSAGE, errors);
	}

	/** A value of the wrong kind, such as an area that is not one of the options. */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ValidationErrorResponse unreadableAnswers(HttpMessageNotReadableException ex) {
		if (ex.getCause() instanceof DatabindException invalid && !invalid.getPath().isEmpty()) {
			String field = invalid.getPath().stream()
					.map(reference -> reference.getPropertyName())
					.filter(name -> name != null)
					.collect(Collectors.joining("."));
			return new ValidationErrorResponse(MESSAGE, Map.of(field, "is not a valid value"));
		}
		return new ValidationErrorResponse("The request could not be read", Map.of());
	}
}
