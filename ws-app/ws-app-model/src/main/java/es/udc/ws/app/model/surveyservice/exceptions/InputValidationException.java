package es.udc.ws.app.model.surveyservice.exceptions;

/**
 * Excepción para entradas de datos no válidas (ej. fechas pasadas, 
 * emails mal formados).
 */
public class InputValidationException extends Exception {

    public InputValidationException(String message) {
        super(message);
    }
}