package nl.carebridge.intake.service;

import java.util.Set;

import org.springframework.stereotype.Component;

import nl.carebridge.intake.model.ComplaintDuration;
import nl.carebridge.intake.model.Symptom;

/**
 * Turns the structured answers into a score from 0 to 100 that pre-sorts the practice's queue.
 * The score is a sorting suggestion for the practice only: it is never shown to the patient and is
 * never a medical judgment (SAD v2, section 2.4). Migration V4 applies the same rule in SQL.
 */
@Component
public class SeverityCalculator {

	static final int MAX_SCORE = 100;
	private static final int POINTS_PER_SEVERITY_STEP = 6;
	private static final int POINTS_PER_SYMPTOM = 4;
	private static final int MAX_SYMPTOM_POINTS = 20;

	public int calculate(int severity, ComplaintDuration duration, Set<Symptom> symptoms) {
		int score = severity * POINTS_PER_SEVERITY_STEP
				+ durationPoints(duration)
				+ Math.min(symptoms.size() * POINTS_PER_SYMPTOM, MAX_SYMPTOM_POINTS);
		return Math.min(score, MAX_SCORE);
	}

	private static int durationPoints(ComplaintDuration duration) {
		return switch (duration) {
			case LESS_THAN_A_DAY -> 0;
			case ONE_TO_THREE_DAYS -> 5;
			case FOUR_TO_SEVEN_DAYS -> 10;
			case ONE_TO_FOUR_WEEKS -> 15;
			case MORE_THAN_A_MONTH -> 20;
		};
	}
}
