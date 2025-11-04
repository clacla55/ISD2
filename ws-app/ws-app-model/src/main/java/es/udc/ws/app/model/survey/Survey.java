package es.udc.ws.app.model.survey;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Representa la entidad Encuesta.
 */
public class Survey {

    private Long surveyId;
    private String question;
    private LocalDateTime creationDate;
    private LocalDateTime endDate;
    private boolean canceled;
    private long positiveResponses; // Contador de respuestas positivas
    private long negativeResponses; // Contador de respuestas negativas

    /**
     * Constructor para crear una nueva encuesta (aún no persistida).
     */
    public Survey(String question, LocalDateTime endDate) {
        this.question = question;
        this.endDate = endDate;
        this.canceled = false;
        this.positiveResponses = 0;
        this.negativeResponses = 0;
        // creationDate se asignará en la capa de servicio
    }

    /**
     * Constructor completo para instanciar desde la BD.
     */
    public Survey(Long surveyId, String question, LocalDateTime creationDate,
                  LocalDateTime endDate, boolean canceled,
                  long positiveResponses, long negativeResponses) {
        this(question, endDate);
        this.surveyId = surveyId;
        this.creationDate = creationDate;
        this.canceled = canceled;
        this.positiveResponses = positiveResponses;
        this.negativeResponses = negativeResponses;
    }

    // Getters y Setters

    public Long getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(Long surveyId) {
        this.surveyId = surveyId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public long getPositiveResponses() {
        return positiveResponses;
    }

    public void setPositiveResponses(long positiveResponses) {
        this.positiveResponses = positiveResponses;
    }

    public long getNegativeResponses() {
        return negativeResponses;
    }

    public void setNegativeResponses(long negativeResponses) {
        this.negativeResponses = negativeResponses;
    }

    // Implementación de equals y hashCode

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Survey survey = (Survey) o;
        // La igualdad se basa en el ID, si está presente
        return surveyId != null && Objects.equals(surveyId, survey.surveyId);
    }

    @Override
    public int hashCode() {
        // Usamos el ID para el hashCode si existe
        return Objects.hash(surveyId);
    }
}