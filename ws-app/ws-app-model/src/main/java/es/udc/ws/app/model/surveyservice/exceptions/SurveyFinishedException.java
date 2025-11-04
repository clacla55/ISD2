package es.udc.ws.app.model.surveyservice.exceptions;

/**
 * Excepción para acciones (responder, cancelar) sobre encuestas 
 * cuya fecha de finalización ya ha pasado.
 */
public class SurveyFinishedException extends Exception {

    public SurveyFinishedException(String message) {
        super(message);
    }
}