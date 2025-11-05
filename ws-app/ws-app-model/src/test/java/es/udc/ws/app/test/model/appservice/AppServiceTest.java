package es.udc.ws.app.test.model.appservice;

import es.udc.ws.app.model.util.ModelConstants;
import es.udc.ws.app.model.survey.SqlSurveyDao;
import es.udc.ws.app.model.survey.SqlSurveyDaoFactory;
import es.udc.ws.app.model.survey.Survey;
import es.udc.ws.app.model.surveyservice.SurveyService;
import es.udc.ws.app.model.surveyservice.SurveyServiceImpl;
import es.udc.ws.app.model.surveyservice.exceptions.InputValidationException;
import es.udc.ws.util.sql.SimpleDataSource;
import es.udc.ws.util.exceptions.InstanceNotFoundException;
import es.udc.ws.util.sql.DataSourceLocator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

public class AppServiceTest {

    private static SurveyService surveyService = null;
    private static SqlSurveyDao surveyDao = null;
    private static DataSource dataSource = null;

    @BeforeAll
    public static void init() {
        dataSource = new SimpleDataSource();
        DataSourceLocator.addDataSource(ModelConstants.APP_DATA_SOURCE, dataSource);
        surveyService = SurveyServiceImpl.getInstance();
        surveyDao = SqlSurveyDaoFactory.getDao();
    }

    private void removeSurvey(Long surveyId) {
        if (surveyId == null) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            surveyDao.remove(connection, surveyId);
        } catch (InstanceNotFoundException e) {
            // Ignorar
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCreateSurvey() throws InputValidationException {
        String question = "¿Te gusta la práctica de ISD?";
        LocalDateTime endDate = LocalDateTime.now().plusDays(7).truncatedTo(ChronoUnit.SECONDS);
        Survey createdSurvey = null;
        try {
            createdSurvey = surveyService.createSurvey(question, endDate);
            assertNotNull(createdSurvey.getSurveyId());
            assertEquals(question, createdSurvey.getQuestion());
            assertEquals(endDate, createdSurvey.getEndDate());
            assertNotNull(createdSurvey.getCreationDate());
            assertFalse(createdSurvey.isCanceled());
            assertEquals(0, createdSurvey.getPositiveResponses());
            assertEquals(0, createdSurvey.getNegativeResponses());
        } finally {
            if (createdSurvey != null) {
                removeSurvey(createdSurvey.getSurveyId());
            }
        }
    }

    @Test
    public void testCreateSurveyNullQuestion() {
        LocalDateTime endDate = LocalDateTime.now().plusDays(7);
        assertThrows(InputValidationException.class, () -> {
            surveyService.createSurvey(null, endDate);
        });
        assertThrows(InputValidationException.class, () -> {
            surveyService.createSurvey("", endDate);
        });
    }

    @Test
    public void testCreateSurveyPastEndDate() {
        String question = "Pregunta válida";
        LocalDateTime pastEndDate = LocalDateTime.now().minusDays(1);
        assertThrows(InputValidationException.class, () -> {
            surveyService.createSurvey(question, pastEndDate);
        });
    }

    // --- NUEVOS TESTS PARA FUNC-3 ---

    /**
     * [FUNC-3] Prueba de éxito para findSurvey.
     */
    @Test
    public void testFindSurvey() throws InputValidationException, InstanceNotFoundException {
        String question = "Encuesta para buscar";
        // Truncamos a segundos para evitar problemas de precisión con la BD
        LocalDateTime endDate = LocalDateTime.now().plusDays(10).truncatedTo(ChronoUnit.SECONDS);
        Survey createdSurvey = surveyService.createSurvey(question, endDate);

        try {
            // [FUNC-3] Buscar la encuesta recién creada
            Survey foundSurvey = surveyService.findSurvey(createdSurvey.getSurveyId());

            // Verificar que los datos son correctos
            assertEquals(createdSurvey, foundSurvey);
            assertEquals(createdSurvey.getSurveyId(), foundSurvey.getSurveyId());
            assertEquals(createdSurvey.getQuestion(), foundSurvey.getQuestion());
            assertEquals(createdSurvey.getCreationDate(), foundSurvey.getCreationDate());
            assertEquals(createdSurvey.getEndDate(), foundSurvey.getEndDate());
            assertEquals(createdSurvey.isCanceled(), foundSurvey.isCanceled());
            assertEquals(createdSurvey.getPositiveResponses(), foundSurvey.getPositiveResponses());
            assertEquals(createdSurvey.getNegativeResponses(), foundSurvey.getNegativeResponses());

        } finally {
            removeSurvey(createdSurvey.getSurveyId());
        }
    }

    /**
     * [FUNC-3] Prueba de error para findSurvey (ID inexistente).
     */
    @Test
    public void testFindNonExistentSurvey() {
        assertThrows(InstanceNotFoundException.class, () -> {
            surveyService.findSurvey(-1L);
        });
    }
}