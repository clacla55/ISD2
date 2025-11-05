package es.udc.ws.app.model.surveyservice;

import es.udc.ws.app.model.util.ModelConstants;
import es.udc.ws.app.model.response.Response;
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

public class SurveyServiceImpl implements SurveyService {

    private static SurveyService instance = null;

    private final DataSource dataSource;
    private final SqlSurveyDao surveyDao;

    private SurveyServiceImpl() {
        dataSource = DataSourceLocator.getDataSource(ModelConstants.APP_DATA_SOURCE);
        surveyDao = SqlSurveyDaoFactory.getDao();
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
        // [FUNC-2] Buscar encuestas delegando en el DAO
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
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Survey cancelSurvey(Long surveyId)
            throws InstanceNotFoundException, SurveyFinishedException,
            SurveyAlreadyCanceledException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Response> getResponses(Long surveyId, boolean onlyPositive)
            throws InstanceNotFoundException {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}