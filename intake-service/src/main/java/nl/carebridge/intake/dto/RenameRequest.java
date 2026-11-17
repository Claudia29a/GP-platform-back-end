package nl.carebridge.intake.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameRequest(@NotBlank @Size(max = 100) String name) {
}
