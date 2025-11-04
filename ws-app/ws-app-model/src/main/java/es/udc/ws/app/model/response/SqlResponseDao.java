package es.udc.ws.app.model.response;

import es.udc.ws.app.model.surveyservice.exceptions.InstanceNotFoundException;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

// Importar las excepciones custom
// import es.udc.fic.isd.exceptions.InstanceNotFoundException;

public interface SqlResponseDao {

    /**
     * Crea una respuesta en la base de datos.
     */
    public Response create(Connection connection, Response response);

    /**
     * Actualiza una respuesta existente (cuando un empleado cambia su voto).
     */
    public void update(Connection connection, Response response)
            throws InstanceNotFoundException;

    /**
     * (Útil para pruebas). Busca una respuesta por su ID.
     */
    public Response find(Connection connection, Long responseId)
            throws InstanceNotFoundException;

    /**
     * Busca la respuesta de un empleado específico para una encuesta específica.
     * Crucial para saber si crear o actualizar.
     */
    public Optional<Response> findBySurveyAndEmployee(Connection connection, Long surveyId, String employeeEmail);

    /**
     * Recupera las respuestas para una encuesta dada.
     * @param onlyPositive true si se desean solo las respuestas positivas.
     */
    public List<Response> findBySurveyId(Connection connection, Long surveyId, boolean onlyPositive);

    /**
     * Necesario para las pruebas de integración.
     */
    public void remove(Connection connection, Long responseId)
            throws InstanceNotFoundException;
}