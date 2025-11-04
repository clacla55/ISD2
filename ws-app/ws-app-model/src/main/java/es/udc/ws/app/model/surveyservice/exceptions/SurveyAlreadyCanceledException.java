package es.udc.ws.app.model.surveyservice.exceptions;

/**
 * Excepción para intentar cancelar una encuesta que ya estaba cancelada.
 */
public class SurveyAlreadyCanceledException extends Exception {

    public SurveyAlreadyCanceledException(String message) {
        super(message);
    }
}