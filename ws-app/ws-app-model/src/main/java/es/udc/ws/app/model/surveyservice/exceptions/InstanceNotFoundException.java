package es.udc.ws.app.model.surveyservice.exceptions;

/**
 * Excepción para cuando se busca una entidad por su ID 
 * (ej. Encuesta o Respuesta) y no se encuentra.
 */
public class InstanceNotFoundException extends Exception {

    public InstanceNotFoundException(String message) {
        super(message);
    }
}