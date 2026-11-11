package nl.carebridge.account.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendRequest(@NotBlank String email) {
}
