package es.udc.ws.app.model.surveyservice;

import es.udc.ws.app.model.util.ModelConstants;
import es.udc.ws.app.model.response.Response;
import es.udc.ws.app.model.response.SqlResponseDao;
import es.udc.ws.app.model.response.SqlResponseDaoFactory;
import es.udc.ws.app.model.survey.SqlSurveyDao;
import es.udc.ws.app.model.survey.SqlSurveyDaoFactory;
import es.udc.ws.app.model.survey.Survey;
import es.udc.ws.app.model.surveyservice.exceptions.InputValidationException;
import es.udc.ws.app.model.surveyservice.exceptions.SurveyAlreadyCanceledException;
import es.udc.ws.app.model.surveyservice.exceptions.SurveyCanceledException;
import es.udc.ws.app.model.surveyservice.exceptions.SurveyFinishedException;
import es.udc.ws.util.exceptions.InstanceNotFoundException;
import es.udc.ws.util.sql.DataSourceLocator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class SurveyServiceImpl implements SurveyService {

    private static SurveyService instance = null;

    private final DataSource dataSource;
    private final SqlSurveyDao surveyDao;
    private final SqlResponseDao responseDao;

    private SurveyServiceImpl() {
        dataSource = DataSourceLocator.getDataSource(ModelConstants.APP_DATA_SOURCE);
        surveyDao = SqlSurveyDaoFactory.getDao();
        responseDao = SqlResponseDaoFactory.getDao();
    }

    public synchronized static SurveyService getInstance() {
        if (instance == null) {
            instance = new SurveyServiceImpl();
        }
        return instance;
    }

    private void validateCreateSurvey(String question, LocalDateTime endDate)
            throws InputValidationException {

        if (question == null || question.trim().isEmpty()) {
            throw new InputValidationException("Question cannot be null or empty");
        }
        if (endDate.isBefore(LocalDateTime.now())) {
            throw new InputValidationException("End date must be a future date");
        }
    }

    @Override
    public Survey createSurvey(String question, LocalDateTime endDate)
            throws InputValidationException {

        validateCreateSurvey(question, endDate);

        Survey survey = new Survey(question, endDate);
        // Ignoramos nanosegundos para compatibilidad con BD MySQL y evitar errores en tests
        survey.setCreationDate(LocalDateTime.now().withNano(0));

        try (Connection connection = dataSource.getConnection()) {

            try {
                connection.setAutoCommit(false);
                Survey createdSurvey = surveyDao.create(connection, survey);
                connection.commit();
                return createdSurvey;

            } catch (SQLException e) {
                connection.rollback();
                throw new RuntimeException(e);
            } catch (RuntimeException | Error e) {
                connection.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Survey> findSurveys(String keyword, boolean onlyFuture) {
        try (Connection connection = dataSource.getConnection()) {
            return surveyDao.findByKeyword(connection, keyword, onlyFuture);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Survey findSurvey(Long surveyId) throws InstanceNotFoundException {
        try (Connection connection = dataSource.getConnection()) {
            return surveyDao.find(connection, surveyId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Response respondToSurvey(Long surveyId, String employeeEmail, boolean response)
            throws InstanceNotFoundException, InputValidationException,
            SurveyFinishedException, SurveyCanceledException {

        // Validar email
        if (employeeEmail == null || employeeEmail.trim().isEmpty()) {
            throw new InputValidationException("Employee email cannot be null or empty");
        }
        // [OPCIONAL] Validación simple de formato de email
        if (!employeeEmail.contains("@")) {
            throw new InputValidationException("Invalid email format");
        }

        try (Connection connection = dataSource.getConnection()) {

            try {
                // CORRECCIÓN PRIMERA ITERACION: Configuración del nivel de aislamiento
                connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                connection.setAutoCommit(false);

                // 1. Buscar la encuesta. Lanza InstanceNotFoundException si no existe
                Survey survey = surveyDao.find(connection, surveyId);

                // 2. Verificar si está cancelada [FUNC-4]
                if (survey.isCanceled()) {
                    throw new SurveyCanceledException("Cannot respond to a canceled survey (ID: " + surveyId + ")");
                }

                // 3. Verificar si ha finalizado [FUNC-4]
                if (survey.getEndDate().isBefore(LocalDateTime.now())) {
                    throw new SurveyFinishedException("Cannot respond to a finished survey (ID: " + surveyId + ")");
                }

                // 4. Buscar si ya existe respuesta de este empleado
                Optional<Response> existingResponseOp = responseDao.findBySurveyAndEmployee(connection, surveyId, employeeEmail);
                Response resultResponse;

                if (existingResponseOp.isPresent()) {
                    // Actualizar respuesta existente [FUNC-4]
                    Response existingResponse = existingResponseOp.get();
                    boolean oldResponseValue = existingResponse.getResponse();

                    // Si cambia el sentido del voto, actualizamos contadores
                    if (oldResponseValue != response) {
                        if (oldResponseValue) { // Era positiva, ahora negativa
                            survey.setPositiveResponses(survey.getPositiveResponses() - 1);
                            survey.setNegativeResponses(survey.getNegativeResponses() + 1);
                        } else { // Era negativa, ahora positiva
                            survey.setNegativeResponses(survey.getNegativeResponses() - 1);
                            survey.setPositiveResponses(survey.getPositiveResponses() + 1);
                        }
                        surveyDao.update(connection, survey);
                    }

                    // Actualizamos la respuesta con el nuevo valor y fecha
                    existingResponse.setResponse(response);
                    existingResponse.setResponseDate(LocalDateTime.now().withNano(0));
                    responseDao.update(connection, existingResponse);
                    resultResponse = existingResponse;

                } else {
                    // Crear nueva respuesta [FUNC-4]
                    resultResponse = new Response(surveyId, employeeEmail, response, LocalDateTime.now().withNano(0));
                    responseDao.create(connection, resultResponse);

                    // Actualizar contadores de la encuesta
                    if (response) {
                        survey.setPositiveResponses(survey.getPositiveResponses() + 1);
                    } else {
                        survey.setNegativeResponses(survey.getNegativeResponses() + 1);
                    }
                    surveyDao.update(connection, survey);
                }

                connection.commit();
                return resultResponse;

            } catch (InstanceNotFoundException | SurveyCanceledException | SurveyFinishedException e) {
                connection.commit(); // Liberar bloqueos de lectura si los hubiera
                throw e;
            } catch (SQLException e) {
                connection.rollback();
                throw new RuntimeException(e);
            } catch (RuntimeException | Error e) {
                connection.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Survey cancelSurvey(Long surveyId)
            throws InstanceNotFoundException, SurveyFinishedException,
            SurveyAlreadyCanceledException {

        try (Connection connection = dataSource.getConnection()) {
            try {
                connection.setAutoCommit(false);

                // 1. Buscar la encuesta. Lanza InstanceNotFoundException si no existe
                Survey survey = surveyDao.find(connection, surveyId);

                // 2. [FUNC-5] Verificar si ha finalizado
                if (survey.getEndDate().isBefore(LocalDateTime.now())) {
                    throw new SurveyFinishedException("Cannot cancel a finished survey (ID: " + surveyId + ")");
                }

                // 3. [FUNC-5] Verificar si ya estaba cancelada
                if (survey.isCanceled()) {
                    throw new SurveyAlreadyCanceledException("Survey is already canceled (ID: " + surveyId + ")");
                }

                // 4. Cancelar la encuesta
                survey.setCanceled(true);
                surveyDao.update(connection, survey);

                connection.commit();
                return survey;

            } catch (InstanceNotFoundException | SurveyFinishedException | SurveyAlreadyCanceledException e) {
                connection.commit(); // Liberar bloqueos de lectura
                throw e;
            } catch (SQLException e) {
                connection.rollback();
                throw new RuntimeException(e);
            } catch (RuntimeException | Error e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Response> getResponses(Long surveyId, boolean onlyPositive)
            throws InstanceNotFoundException {

        try (Connection connection = dataSource.getConnection()) {

            // [FUNC-6] Primero verificamos que la encuesta existe
            // Si no existe, el DAO lanzará InstanceNotFoundException
            surveyDao.find(connection, surveyId);

            // Recuperamos las respuestas
            return responseDao.findBySurveyId(connection, surveyId, onlyPositive);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}