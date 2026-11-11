package nl.carebridge.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyRequest(@NotBlank String email, @NotBlank @Pattern(regexp = "\\d{6}") String code) {
}
