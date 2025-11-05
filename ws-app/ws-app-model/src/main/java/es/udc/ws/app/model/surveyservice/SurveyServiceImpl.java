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
    // private final SqlResponseDao responseDao; // Non necessario per FUNC-1

    private SurveyServiceImpl() {
        dataSource = DataSourceLocator.getDataSource(ModelConstants.APP_DATA_SOURCE);
        surveyDao = SqlSurveyDaoFactory.getDao();
        // responseDao = SqlResponseDaoFactory.getDao(); // Non necessario per FUNC-1
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

        // Validazione
        validateCreateSurvey(question, endDate);

        // Creazione dell'oggetto
        Survey survey = new Survey(question, endDate);
        survey.setCreationDate(LocalDateTime.now()); // Impostiamo la data di creazione [cite: 29]

        // Logica di persistenza
        try (Connection connection = dataSource.getConnection()) {

            try {
                // Inizio transazione
                connection.setAutoCommit(false);

                // Persistenza
                Survey createdSurvey = surveyDao.create(connection, survey);

                // Fine transazione
                connection.commit();

                return createdSurvey;

            } catch (SQLException e) {
                connection.rollback();
                throw new RuntimeException(e);
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Survey> findSurveys(String keyword, boolean onlyFuture) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Survey findSurvey(Long surveyId) throws InstanceNotFoundException {
        throw new UnsupportedOperationException("Not implemented yet");
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