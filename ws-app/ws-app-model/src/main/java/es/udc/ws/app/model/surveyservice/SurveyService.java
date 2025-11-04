package es.udc.ws.app.model.surveyservice;
import es.udc.ws.app.model.surveyservice.exceptions.InputValidationException;
import es.udc.ws.app.model.surveyservice.exceptions.SurveyAlreadyCanceledException;
import es.udc.ws.app.model.surveyservice.exceptions.SurveyCanceledException;
import es.udc.ws.app.model.surveyservice.exceptions.SurveyFinishedException;
import es.udc.ws.app.model.survey.Survey;
import es.udc.ws.app.model.response.Response;
import es.udc.ws.util.exceptions.InstanceNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

public interface SurveyService {

    /**
     * [FUNC-1] Crear una nueva encuesta.
     * La fecha de fin debe ser futura.
     * Guarda la fecha de creación.
     */
    public Survey createSurvey(String question, LocalDateTime endDate)
            throws InputValidationException;

    /**
     * [FUNC-2] Buscar encuestas por palabra clave.
     * Si la palabraClave es vacía, devuelve todas.
     * Incluye contadores y estado de cancelación.
     */
    public List<Survey> findSurveys(String keyword, boolean onlyFuture);

    /**
     * [FUNC-3] Encontrar una encuesta por su ID.
     * Devuelve todos los datos, incluidos los contadores.
     */
    public Survey findSurvey(Long surveyId)
            throws InstanceNotFoundException;

    /**
     * [FUNC-4] Permite a un empleado responder a una encuesta.
     * Actualiza la respuesta si ya existía.
     * Lanza error si la encuesta está finalizada o cancelada.
     */
    public Response respondToSurvey(Long surveyId, String employeeEmail, boolean response)
            throws InstanceNotFoundException, InputValidationException,
            SurveyFinishedException, SurveyCanceledException;

    /**
     * [FUNC-5] Cancela una encuesta no finalizada.
     * Lanza error si ya está finalizada o ya está cancelada.
     */
    public Survey cancelSurvey(Long surveyId)
            throws InstanceNotFoundException, SurveyFinishedException,
            SurveyAlreadyCanceledException;

    /**
     * [FUNC-6] Obtiene las respuestas de una encuesta.
     * Puede filtrar solo por respuestas positivas.
     * Devuelve todos los datos de la respuesta (email, respuesta, fecha).
     */
    public List<Response> getResponses(Long surveyId, boolean onlyPositive)
            throws InstanceNotFoundException;
}