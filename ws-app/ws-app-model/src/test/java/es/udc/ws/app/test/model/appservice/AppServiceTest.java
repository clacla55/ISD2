package es.udc.ws.app.test.model.appservice;

import es.udc.ws.app.model.response.Response;
import es.udc.ws.app.model.response.SqlResponseDao;
import es.udc.ws.app.model.response.SqlResponseDaoFactory;
import es.udc.ws.app.model.surveyservice.exceptions.SurveyAlreadyCanceledException;
import es.udc.ws.app.model.surveyservice.exceptions.SurveyCanceledException;
import es.udc.ws.app.model.surveyservice.exceptions.SurveyFinishedException;
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
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AppServiceTest {

    private static SurveyService surveyService = null;
    private static SqlSurveyDao surveyDao = null;
    private static SqlResponseDao responseDao = null;
    private static DataSource dataSource = null;

    @BeforeAll
    public static void init() {
        dataSource = new SimpleDataSource();
        DataSourceLocator.addDataSource(ModelConstants.APP_DATA_SOURCE, dataSource);
        surveyService = SurveyServiceImpl.getInstance();
        surveyDao = SqlSurveyDaoFactory.getDao();
        responseDao = SqlResponseDaoFactory.getDao();
    }

    private void removeSurvey(Long surveyId) {
        if (surveyId == null) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            surveyDao.remove(connection, surveyId);
        } catch (InstanceNotFoundException e) {
            // Ignorar si ya no existe
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // --- TEST EXISTENTES ---

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

    @Test
    public void testFindSurvey() throws InputValidationException, InstanceNotFoundException {
        String question = "Encuesta para buscar por ID";
        LocalDateTime endDate = LocalDateTime.now().plusDays(10).truncatedTo(ChronoUnit.SECONDS);
        Survey createdSurvey = surveyService.createSurvey(question, endDate);

        try {
            Survey foundSurvey = surveyService.findSurvey(createdSurvey.getSurveyId());

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

    @Test
    public void testFindNonExistentSurvey() {
        assertThrows(InstanceNotFoundException.class, () -> {
            surveyService.findSurvey(-1L);
        });
    }

    @Test
    public void testFindSurveysByKeyword() throws InputValidationException {
        Survey s1 = surveyService.createSurvey("Encuesta sobre Java y SQL", LocalDateTime.now().plusDays(5));
        Survey s2 = surveyService.createSurvey("Pregunta solo sobre Java", LocalDateTime.now().plusDays(5));
        Survey s3 = surveyService.createSurvey("Otra cosa diferente", LocalDateTime.now().plusDays(5));

        try {
            List<Survey> javaSurveys = surveyService.findSurveys("Java", false);
            assertTrue(javaSurveys.size() >= 2);
            assertTrue(javaSurveys.contains(s1));
            assertTrue(javaSurveys.contains(s2));
            assertFalse(javaSurveys.contains(s3));

            List<Survey> sqlSurveys = surveyService.findSurveys("SQL", false);
            assertTrue(sqlSurveys.contains(s1));
            assertFalse(sqlSurveys.contains(s2));

            List<Survey> allSurveys = surveyService.findSurveys("", false);
            assertTrue(allSurveys.size() >= 3);
            assertTrue(allSurveys.contains(s1));
            assertTrue(allSurveys.contains(s2));
            assertTrue(allSurveys.contains(s3));

        } finally {
            removeSurvey(s1.getSurveyId());
            removeSurvey(s2.getSurveyId());
            removeSurvey(s3.getSurveyId());
        }
    }

    private Survey createPastSurveyRaw(String question) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "INSERT INTO Survey (question, creationDate, endDate, canceled, positiveResponses, negativeResponses) VALUES (?, ?, ?, ?, 0, 0)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                Timestamp pastDate = Timestamp.valueOf(LocalDateTime.now().minusDays(10));
                ps.setString(1, question);
                ps.setTimestamp(2, pastDate);
                ps.setTimestamp(3, pastDate);
                ps.setBoolean(4, false);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    return new Survey(rs.getLong(1), question, pastDate.toLocalDateTime(), pastDate.toLocalDateTime(), false, 0, 0);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Test
    public void testFindSurveysOnlyFuture() throws InputValidationException {
        Survey futureSurvey = surveyService.createSurvey("Encuesta futura", LocalDateTime.now().plusDays(10));
        Survey pastSurvey = createPastSurveyRaw("Encuesta pasada");

        try {
            List<Survey> onlyFuture = surveyService.findSurveys("", true);
            assertTrue(onlyFuture.contains(futureSurvey));
            assertFalse(onlyFuture.contains(pastSurvey));

            List<Survey> all = surveyService.findSurveys("", false);
            assertTrue(all.contains(futureSurvey));
            assertTrue(all.contains(pastSurvey));

        } finally {
            removeSurvey(futureSurvey.getSurveyId());
            if (pastSurvey != null) {
                removeSurvey(pastSurvey.getSurveyId());
            }
        }
    }

    // --- NUEVOS TESTS PARA FUNC-4 (Responder encuesta) ---

    @Test
    public void testRespondToSurvey() throws InputValidationException, InstanceNotFoundException, SurveyFinishedException, SurveyCanceledException {
        Survey survey = surveyService.createSurvey("¿Te gusta Java?", LocalDateTime.now().plusDays(1));
        try {
            // 1. Respuesta Positiva (creación)
            Response res1 = surveyService.respondToSurvey(survey.getSurveyId(), "emp1@techfic.com", true);

            assertEquals(survey.getSurveyId(), res1.getSurveyId());
            assertEquals("emp1@techfic.com", res1.getEmployeeEmail());
            assertTrue(res1.getResponse());
            assertNotNull(res1.getResponseDate());

            // Verificar contadores
            Survey sAfterRes1 = surveyService.findSurvey(survey.getSurveyId());
            assertEquals(1, sAfterRes1.getPositiveResponses());
            assertEquals(0, sAfterRes1.getNegativeResponses());

            // 2. Respuesta Negativa de otro empleado (creación)
            surveyService.respondToSurvey(survey.getSurveyId(), "emp2@techfic.com", false);
            Survey sAfterRes2 = surveyService.findSurvey(survey.getSurveyId());
            assertEquals(1, sAfterRes2.getPositiveResponses());
            assertEquals(1, sAfterRes2.getNegativeResponses());

            // 3. Cambio de voto (actualización): emp1 cambia de true a false
            Response res1Updated = surveyService.respondToSurvey(survey.getSurveyId(), "emp1@techfic.com", false);

            assertFalse(res1Updated.getResponse());
            // La fecha debe ser posterior o igual a la original
            assertTrue(!res1Updated.getResponseDate().isBefore(res1.getResponseDate()));
            assertEquals(res1.getResponseId(), res1Updated.getResponseId()); // Mismo ID de respuesta

            // Verificar contadores tras cambio (debe ser 0 positivas, 2 negativas)
            Survey sAfterUpdate = surveyService.findSurvey(survey.getSurveyId());
            assertEquals(0, sAfterUpdate.getPositiveResponses());
            assertEquals(2, sAfterUpdate.getNegativeResponses());

        } finally {
            removeSurvey(survey.getSurveyId());
        }
    }

    @Test
    public void testRespondToNonExistentSurvey() {
        assertThrows(InstanceNotFoundException.class, () -> {
            surveyService.respondToSurvey(-1L, "a@b.com", true);
        });
    }

    @Test
    public void testRespondToFinishedSurvey() {
        Survey pastSurvey = createPastSurveyRaw("Encuesta finalizada");
        try {
            assertThrows(SurveyFinishedException.class, () -> {
                surveyService.respondToSurvey(pastSurvey.getSurveyId(), "a@b.com", true);
            });
        } finally {
            if (pastSurvey != null) removeSurvey(pastSurvey.getSurveyId());
        }
    }

    // Método auxiliar para cancelar una encuesta directamente en BD (usado en test FUNC-4)
    private void cancelSurveyRaw(Long surveyId) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "UPDATE Survey SET canceled = 1 WHERE surveyId = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, surveyId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testRespondToCanceledSurvey() throws InputValidationException {
        Survey survey = surveyService.createSurvey("Encuesta a cancelar", LocalDateTime.now().plusDays(1));
        cancelSurveyRaw(survey.getSurveyId()); // La cancelamos manualmente para el test

        try {
            assertThrows(SurveyCanceledException.class, () -> {
                surveyService.respondToSurvey(survey.getSurveyId(), "a@b.com", true);
            });
        } finally {
            removeSurvey(survey.getSurveyId());
        }
    }

    @Test
    public void testRespondWithInvalidEmail() throws InputValidationException {
        Survey survey = surveyService.createSurvey("Encuesta valida", LocalDateTime.now().plusDays(1));
        try {
            assertThrows(InputValidationException.class, () -> {
                surveyService.respondToSurvey(survey.getSurveyId(), null, true);
            });
            assertThrows(InputValidationException.class, () -> {
                surveyService.respondToSurvey(survey.getSurveyId(), "", true);
            });
            assertThrows(InputValidationException.class, () -> {
                surveyService.respondToSurvey(survey.getSurveyId(), "invalid-email-format", true);
            });
        } finally {
            removeSurvey(survey.getSurveyId());
        }
    }

    // --- NUEVOS TESTS PARA FUNC-5 (Cancelar encuesta) ---

    @Test
    public void testCancelSurvey() throws InputValidationException, InstanceNotFoundException, SurveyFinishedException, SurveyAlreadyCanceledException {
        Survey survey = surveyService.createSurvey("Encuesta para cancelar", LocalDateTime.now().plusDays(2));
        try {
            // Estado inicial
            assertFalse(survey.isCanceled());

            // Cancelamos
            Survey canceledSurvey = surveyService.cancelSurvey(survey.getSurveyId());
            assertTrue(canceledSurvey.isCanceled());
            assertEquals(survey.getSurveyId(), canceledSurvey.getSurveyId());

            // Verificamos en BD
            Survey foundSurvey = surveyService.findSurvey(survey.getSurveyId());
            assertTrue(foundSurvey.isCanceled());

        } finally {
            removeSurvey(survey.getSurveyId());
        }
    }

    @Test
    public void testCancelNonExistentSurvey() {
        assertThrows(InstanceNotFoundException.class, () -> {
            surveyService.cancelSurvey(-1L);
        });
    }

    @Test
    public void testCancelFinishedSurvey() {
        Survey pastSurvey = createPastSurveyRaw("Encuesta finalizada para cancelar");
        try {
            assertThrows(SurveyFinishedException.class, () -> {
                surveyService.cancelSurvey(pastSurvey.getSurveyId());
            });
        } finally {
            if (pastSurvey != null) removeSurvey(pastSurvey.getSurveyId());
        }
    }

    @Test
    public void testCancelAlreadyCanceledSurvey() throws InputValidationException, SurveyFinishedException, SurveyAlreadyCanceledException, InstanceNotFoundException {
        Survey survey = surveyService.createSurvey("Encuesta para doble cancel", LocalDateTime.now().plusDays(1));
        try {
            // Primera cancelación (exitosa)
            surveyService.cancelSurvey(survey.getSurveyId());

            // Segunda cancelación (debe fallar)
            assertThrows(SurveyAlreadyCanceledException.class, () -> {
                surveyService.cancelSurvey(survey.getSurveyId());
            });

        } finally {
            removeSurvey(survey.getSurveyId());
        }
    }
}