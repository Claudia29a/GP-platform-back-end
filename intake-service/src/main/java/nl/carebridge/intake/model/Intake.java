package nl.carebridge.intake.model;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "intake")
public class Intake {

	public enum Status {
		SUBMITTED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** Google account ID of the patient who asked the question. */
	@Column(nullable = false)
	private String patientId;

	@Column(nullable = false)
	private String patientName;

	@Column(nullable = false, length = 320)
	private String patientEmail;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ComplaintArea complaintArea;

	/** Only filled in when the area is OTHER. */
	@Column(length = 100)
	private String complaintAreaOther;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ComplaintDuration duration;

	/** How much it bothers the patient, from 1 (barely) to 10 (worst imaginable). */
	@Column(nullable = false)
	private int severity;

	/** The ticked checklist items, stored as a JSON array (ADR-005). */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private Set<Symptom> symptoms = EnumSet.noneOf(Symptom.class);

	@Column(length = 200)
	private String symptomsOther;

	@Column(nullable = false)
	private boolean takesMedication;

	/** Only filled in when the patient takes medication or treatment. */
	@Column(length = 500)
	private String currentMedication;

	@Column(nullable = false)
	private boolean triedTreatment;

	/** Only filled in when the patient already tried something for this problem. */
	@Column(length = 500)
	private String treatmentTried;

	@Column(nullable = false, length = 1000)
	private String description;

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
