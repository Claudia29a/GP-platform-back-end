package nl.carebridge.messaging.appointment;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

/** User story nr 10: the free time the patient picked. */
public record BookRequest(@NotNull Instant start) {
}
