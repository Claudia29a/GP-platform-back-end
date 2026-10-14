package nl.carebridge.intake.dto;

import static nl.carebridge.intake.TestIntakes.validRequest;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import nl.carebridge.intake.model.ComplaintArea;

class SubmitIntakeRequestTest {

	private static ValidatorFactory factory;
	private static Validator validator;

	@BeforeAll
	static void createValidator() {
		factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		factory.close();
	}

	@Test
	void completeAnswersAreValid() {
		assertThat(invalidFields(validRequest())).isEmpty();
	}

	@Test
	void requiredAnswersMustBeGiven() {
		SubmitIntakeRequest empty = new SubmitIntakeRequest(null, null, null, null, null, null, null, null, null, null, " ");

		assertThat(invalidFields(empty)).containsExactlyInAnyOrder(
				"complaintArea", "duration", "severity", "takesMedication", "triedTreatment", "description");
	}

	@Test
	void severityMustBeBetweenOneAndTen() {
		assertThat(invalidFields(withSeverity(0))).containsExactly("severity");
		assertThat(invalidFields(withSeverity(11))).containsExactly("severity");
		assertThat(invalidFields(withSeverity(1))).isEmpty();
		assertThat(invalidFields(withSeverity(10))).isEmpty();
	}

	@Test
	void otherAreaNeedsADescription() {
		SubmitIntakeRequest r = validRequest();
		SubmitIntakeRequest withoutText = new SubmitIntakeRequest(ComplaintArea.OTHER, "  ", r.duration(), r.severity(),
				r.symptoms(), r.symptomsOther(), r.takesMedication(), r.currentMedication(), r.triedTreatment(),
				r.treatmentTried(), r.description());
		SubmitIntakeRequest withText = new SubmitIntakeRequest(ComplaintArea.OTHER, "Ears", r.duration(), r.severity(),
				r.symptoms(), r.symptomsOther(), r.takesMedication(), r.currentMedication(), r.triedTreatment(),
				r.treatmentTried(), r.description());

		assertThat(invalidFields(withoutText)).containsExactly("complaintAreaOtherGiven");
		assertThat(invalidFields(withText)).isEmpty();
	}

	@Test
	void yesToMedicationNeedsWhichOnes() {
		SubmitIntakeRequest r = validRequest();
		SubmitIntakeRequest yesWithout = new SubmitIntakeRequest(r.complaintArea(), null, r.duration(), r.severity(),
				r.symptoms(), null, true, "", r.triedTreatment(), r.treatmentTried(), r.description());
		SubmitIntakeRequest yesWith = new SubmitIntakeRequest(r.complaintArea(), null, r.duration(), r.severity(),
				r.symptoms(), null, true, "Paracetamol", r.triedTreatment(), r.treatmentTried(), r.description());

		assertThat(invalidFields(yesWithout)).containsExactly("currentMedicationGiven");
		assertThat(invalidFields(yesWith)).isEmpty();
	}

	@Test
	void yesToTriedTreatmentNeedsWhat() {
		SubmitIntakeRequest r = validRequest();
		SubmitIntakeRequest yesWithout = new SubmitIntakeRequest(r.complaintArea(), null, r.duration(), r.severity(),
				r.symptoms(), null, false, null, true, null, r.description());

		assertThat(invalidFields(yesWithout)).containsExactly("treatmentTriedGiven");
	}

	@Test
	void freeTextHasMaximumLengths() {
		SubmitIntakeRequest r = validRequest();
		SubmitIntakeRequest tooLong = new SubmitIntakeRequest(ComplaintArea.OTHER, "x".repeat(101), r.duration(),
				r.severity(), r.symptoms(), "x".repeat(201), true, "x".repeat(501), true, "x".repeat(501), "x".repeat(1001));

		assertThat(invalidFields(tooLong)).containsExactlyInAnyOrder(
				"complaintAreaOther", "symptomsOther", "currentMedication", "treatmentTried", "description");
	}

	@Test
	void missingSymptomListMeansNoSymptoms() {
		SubmitIntakeRequest r = validRequest();
		SubmitIntakeRequest noList = new SubmitIntakeRequest(r.complaintArea(), null, r.duration(), r.severity(), null,
				null, false, null, false, null, r.description());

		assertThat(noList.symptoms()).isEmpty();
		assertThat(invalidFields(noList)).isEmpty();
	}

	private static SubmitIntakeRequest withSeverity(int severity) {
		SubmitIntakeRequest r = validRequest();
		return new SubmitIntakeRequest(r.complaintArea(), r.complaintAreaOther(), r.duration(), severity, r.symptoms(),
				r.symptomsOther(), r.takesMedication(), r.currentMedication(), r.triedTreatment(), r.treatmentTried(),
				r.description());
	}

	private static Set<String> invalidFields(SubmitIntakeRequest request) {
		return validator.validate(request).stream()
				.map(ConstraintViolation::getPropertyPath)
				.map(Object::toString)
				.collect(Collectors.toSet());
	}
}
