package nl.carebridge.intake.dto;

import java.util.Map;

/** The answer to invalid input: one error per field, so the front-end can show each next to its field. */
public record ValidationErrorResponse(String message, Map<String, String> errors) {
}
