package nl.carebridge.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code appointment}: the GP asks the patient to make an appointment (User story nr 9); missing means no. */
public record ReplyRequest(@NotBlank @Size(max = 2000) String text, Boolean appointment) {

	public boolean asksForAppointment() {
		return Boolean.TRUE.equals(appointment);
	}
}
