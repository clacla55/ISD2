package es.udc.ws.app.model.survey;
import es.udc.ws.app.model.survey.Survey;
import es.udc.ws.util.exceptions.InstanceNotFoundException;
import java.sql.Connection;
import java.util.List;

// Importar las excepciones custom (suponiendo que están en un paquete)
// import es.udc.fic.isd.exceptions.InstanceNotFoundException;

public interface SqlSurveyDao {

    /**
     * [FUNC-1] Crea una encuesta en la base de datos.
     */
    public Survey create(Connection connection, Survey survey);

    /**
     * [FUNC-4] [FUNC-5] Actualiza una encuesta existente.
     * (Necesario para cancelar o actualizar contadores).
     */
    public void update(Connection connection, Survey survey)
            throws InstanceNotFoundException;

    /**
     * [FUNC-3] [FUNC-4] [FUNC-5] [FUNC-6] Busca una encuesta por su ID.
     */
    public Survey find(Connection connection, Long surveyId)
            throws InstanceNotFoundException;

    /**
     * [FUNC-2] Busca encuestas por palabra clave en la pregunta.
     * @param onlyFuture true si deben recuperarse solo encuestas 
     * con fecha de fin futura.
     */
    public List<Survey> findByKeyword(Connection connection, String keyword, boolean onlyFuture);

    /**
     * Necesario para las pruebas de integración (borrar datos).
     */
    public void remove(Connection connection, Long surveyId)
            throws InstanceNotFoundException;
}