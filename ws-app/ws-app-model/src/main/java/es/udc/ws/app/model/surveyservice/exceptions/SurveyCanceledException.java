package es.udc.ws.app.model.surveyservice.exceptions;

/**
 * Excepción utilizada al intentar responder a una encuesta 
 * que ya ha sido cancelada.
 */
public class SurveyCanceledException extends Exception {

    public SurveyCanceledException(String message) {
        super(message);
    }
}