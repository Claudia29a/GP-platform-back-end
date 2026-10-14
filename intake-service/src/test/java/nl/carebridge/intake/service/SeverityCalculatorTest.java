package nl.carebridge.intake.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import nl.carebridge.intake.model.ComplaintDuration;
import nl.carebridge.intake.model.Symptom;

class SeverityCalculatorTest {

	private final SeverityCalculator calculator = new SeverityCalculator();

	@Test
	void mildNewProblemWithoutSymptomsScoresLow() {
		assertThat(calculator.calculate(1, ComplaintDuration.LESS_THAN_A_DAY, Set.of())).isEqualTo(6);
	}

	@Test
	void addsSeverityDurationAndSymptomPoints() {
		int score = calculator.calculate(4, ComplaintDuration.ONE_TO_FOUR_WEEKS, Set.of(Symptom.RASH_ITCHING, Symptom.SWELLING));

		assertThat(score).isEqualTo(4 * 6 + 15 + 2 * 4);
	}

	@Test
	void everyDurationStepAddsFivePoints() {
		assertThat(ComplaintDuration.values())
				.extracting(duration -> calculator.calculate(1, duration, Set.of()))
				.containsExactly(6, 11, 16, 21, 26);
	}

	@Test
	void symptomPointsStopAtFiveSymptoms() {
		int five = calculator.calculate(1, ComplaintDuration.LESS_THAN_A_DAY,
				EnumSet.of(Symptom.FEVER, Symptom.PAIN, Symptom.COUGH, Symptom.NAUSEA_VOMITING, Symptom.TIREDNESS));
		int all = calculator.calculate(1, ComplaintDuration.LESS_THAN_A_DAY, EnumSet.allOf(Symptom.class));

		assertThat(all).isEqualTo(five);
	}

	@Test
	void worstAnswersScoreExactlyTheMaximum() {
		assertThat(calculator.calculate(10, ComplaintDuration.MORE_THAN_A_MONTH, EnumSet.allOf(Symptom.class)))
				.isEqualTo(SeverityCalculator.MAX_SCORE);
	}
}
