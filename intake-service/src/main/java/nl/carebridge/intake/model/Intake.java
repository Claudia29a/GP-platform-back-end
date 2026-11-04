package nl.carebridge.intake.model;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import nl.carebridge.intake.encryption.EncryptedConverters;

@Entity
@Table(name = "intake")
public class Intake {

	public enum Status {
		IN_REVIEW
	}

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** A hash of the account ID of the patient who asked the question; hashed, because it is only compared. */
	@Column(nullable = false)
	private String patientId;

	@Convert(converter = EncryptedConverters.Text.class)
	@Column(nullable = false)
	private String patientName;

	@Convert(converter = EncryptedConverters.Text.class)
	@Column(nullable = false)
	private String patientEmail;

	@Convert(converter = EncryptedConverters.Area.class)
	@Column(nullable = false)
	private ComplaintArea complaintArea;

	/** Only filled in when the area is OTHER. */
	@Convert(converter = EncryptedConverters.Text.class)
	private String complaintAreaOther;

	@Convert(converter = EncryptedConverters.Duration.class)
	@Column(nullable = false)
	private ComplaintDuration duration;

	/** How much it bothers the patient, from 1 (barely) to 10 (worst imaginable). */
	@Convert(converter = EncryptedConverters.Number.class)
	@Column(nullable = false)
	private int severity;

	/** The ticked checklist items. */
	@Convert(converter = EncryptedConverters.Symptoms.class)
	@Column(nullable = false)
	private Set<Symptom> symptoms = EnumSet.noneOf(Symptom.class);

	@Convert(converter = EncryptedConverters.Text.class)
	private String symptomsOther;

	@Convert(converter = EncryptedConverters.YesNo.class)
	@Column(nullable = false)
	private boolean takesMedication;

	/** Only filled in when the patient takes medication or treatment. */
	@Convert(converter = EncryptedConverters.Text.class)
	private String currentMedication;

	@Convert(converter = EncryptedConverters.YesNo.class)
	@Column(nullable = false)
	private boolean triedTreatment;

	/** Only filled in when the patient already tried something for this problem. */
	@Convert(converter = EncryptedConverters.Text.class)
	private String treatmentTried;

	@Convert(converter = EncryptedConverters.Text.class)
	@Column(nullable = false)
	private String description;

	/** Sorting suggestion for the practice (0–100); never sent to patients. */
	@Column(nullable = false)
	private int severityScore;

	/** Place in the practice's queue, 1 = first; null once a question leaves the queue. */
	private Integer queuePosition;

	/** True when staff put this question above a more severe one; new questions stay below it. */
	@Column(nullable = false)
	private boolean movedByHand;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status;

	@Column(nullable = false)
	private Instant submittedAt;

	public Intake() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getPatientId() {
		return patientId;
	}

	public void setPatientId(String patientId) {
		this.patientId = patientId;
	}

	public String getPatientName() {
		return patientName;
	}

	public void setPatientName(String patientName) {
		this.patientName = patientName;
	}

	public String getPatientEmail() {
		return patientEmail;
	}

	public void setPatientEmail(String patientEmail) {
		this.patientEmail = patientEmail;
	}

	public ComplaintArea getComplaintArea() {
		return complaintArea;
	}

	public void setComplaintArea(ComplaintArea complaintArea) {
		this.complaintArea = complaintArea;
	}

	public String getComplaintAreaOther() {
		return complaintAreaOther;
	}

	public void setComplaintAreaOther(String complaintAreaOther) {
		this.complaintAreaOther = complaintAreaOther;
	}

	public ComplaintDuration getDuration() {
		return duration;
	}

	public void setDuration(ComplaintDuration duration) {
		this.duration = duration;
	}

	public int getSeverity() {
		return severity;
	}

	public void setSeverity(int severity) {
		this.severity = severity;
	}

	public Set<Symptom> getSymptoms() {
		return symptoms;
	}

	public void setSymptoms(Set<Symptom> symptoms) {
		this.symptoms = symptoms.isEmpty() ? EnumSet.noneOf(Symptom.class) : EnumSet.copyOf(symptoms);
	}

	public String getSymptomsOther() {
		return symptomsOther;
	}

	public void setSymptomsOther(String symptomsOther) {
		this.symptomsOther = symptomsOther;
	}

	public boolean isTakesMedication() {
		return takesMedication;
	}

	public void setTakesMedication(boolean takesMedication) {
		this.takesMedication = takesMedication;
	}

	public String getCurrentMedication() {
		return currentMedication;
	}

	public void setCurrentMedication(String currentMedication) {
		this.currentMedication = currentMedication;
	}

	public boolean isTriedTreatment() {
		return triedTreatment;
	}

	public void setTriedTreatment(boolean triedTreatment) {
		this.triedTreatment = triedTreatment;
	}

	public String getTreatmentTried() {
		return treatmentTried;
	}

	public void setTreatmentTried(String treatmentTried) {
		this.treatmentTried = treatmentTried;
	}

	public int getSeverityScore() {
		return severityScore;
	}

	public void setSeverityScore(int severityScore) {
		this.severityScore = severityScore;
	}

	public Integer getQueuePosition() {
		return queuePosition;
	}

	public void setQueuePosition(Integer queuePosition) {
		this.queuePosition = queuePosition;
	}

	public boolean isMovedByHand() {
		return movedByHand;
	}

	public void setMovedByHand(boolean movedByHand) {
		this.movedByHand = movedByHand;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Instant getSubmittedAt() {
		return submittedAt;
	}

	public void setSubmittedAt(Instant submittedAt) {
		this.submittedAt = submittedAt;
	}
}
