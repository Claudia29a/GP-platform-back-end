package nl.carebridge.intake;

import java.util.Set;

import nl.carebridge.intake.dto.SubmitIntakeRequest;
import nl.carebridge.intake.model.ComplaintArea;
import nl.carebridge.intake.model.ComplaintDuration;
import nl.carebridge.intake.model.Symptom;

/** A complete, valid set of answers that tests can change one field at a time. */
public final class TestIntakes {

	private TestIntakes() {
	}

	public static SubmitIntakeRequest validRequest() {
		return new SubmitIntakeRequest(ComplaintArea.SKIN, null, ComplaintDuration.ONE_TO_FOUR_WEEKS, 4,
				Set.of(Symptom.RASH_ITCHING, Symptom.SWELLING), null, false, null, true, "Moisturising cream",
				"Itchy rash on my left arm");
	}

	public static final String VALID_JSON = """
			{
			  "complaintArea": "SKIN",
			  "duration": "ONE_TO_FOUR_WEEKS",
			  "severity": 4,
			  "symptoms": ["RASH_ITCHING", "SWELLING"],
			  "takesMedication": false,
			  "triedTreatment": true,
			  "treatmentTried": "Moisturising cream",
			  "description": "Itchy rash on my left arm"
			}
			""";
}
