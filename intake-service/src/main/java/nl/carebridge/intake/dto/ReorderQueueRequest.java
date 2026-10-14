package nl.carebridge.intake.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** The ids of all open questions, in the order a staff member put them. */
public record ReorderQueueRequest(@NotNull List<@NotNull UUID> intakeIds) {
}
