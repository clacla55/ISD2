package es.udc.ws.app.model.response;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Representa la respuesta de un empleado a una encuesta.
 */
public class Response {

    private Long responseId;
    private Long surveyId; // Foreign key a Survey
    private String employeeEmail;
    private boolean response; // true = positiva, false = negativa
    private LocalDateTime responseDate;

    /**
     * Constructor para crear una nueva respuesta.
     */
    public Response(Long surveyId, String employeeEmail, boolean response, LocalDateTime responseDate) {
        this.surveyId = surveyId;
        this.employeeEmail = employeeEmail;
        this.response = response;
        this.responseDate = responseDate;
    }

    /**
     * Constructor completo para instanciar desde la BD.
     */
    public Response(Long responseId, Long surveyId, String employeeEmail,
                    boolean response, LocalDateTime responseDate) {
        this(surveyId, employeeEmail, response, responseDate);
        this.responseId = responseId;
    }

    // Getters y Setters

    public Long getResponseId() {
        return responseId;
    }

    public void setResponseId(Long responseId) {
        this.responseId = responseId;
    }

    public Long getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(Long surveyId) {
        this.surveyId = surveyId;
    }

    public String getEmployeeEmail() {
        return employeeEmail;
    }

    public void setEmployeeEmail(String employeeEmail) {
        this.employeeEmail = employeeEmail;
    }

    public boolean getResponse() {
        return response;
    }

    public void setResponse(boolean response) {
        this.response = response;
    }

    public LocalDateTime getResponseDate() {
        return responseDate;
    }

    public void setResponseDate(LocalDateTime responseDate) {
        this.responseDate = responseDate;
    }

    // Implementación de equals y hashCode

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Response response = (Response) o;
        // La igualdad se basa en el ID, si está presente
        return responseId != null && Objects.equals(responseId, response.responseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(responseId);
    }
}