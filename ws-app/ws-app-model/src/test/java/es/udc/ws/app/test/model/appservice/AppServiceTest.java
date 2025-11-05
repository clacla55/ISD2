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
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
            // Ignorar si ya no existe
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

    // --- NUEVOS TESTS PARA FUNC-2 (Búsqueda) ---

    @Test
    public void testFindSurveysByKeyword() throws InputValidationException {
        // Crear conjunto de encuestas para probar
        Survey s1 = surveyService.createSurvey("Encuesta sobre Java y SQL", LocalDateTime.now().plusDays(5));
        Survey s2 = surveyService.createSurvey("Pregunta solo sobre Java", LocalDateTime.now().plusDays(5));
        Survey s3 = surveyService.createSurvey("Otra cosa diferente", LocalDateTime.now().plusDays(5));

        try {
            // [FUNC-2] Buscar por palabra clave "Java" (debería encontrar s1 y s2)
            List<Survey> javaSurveys = surveyService.findSurveys("Java", false);
            assertTrue(javaSurveys.size() >= 2);
            assertTrue(javaSurveys.contains(s1));
            assertTrue(javaSurveys.contains(s2));
            assertFalse(javaSurveys.contains(s3));

            // [FUNC-2] Buscar por "SQL" (debería encontrar s1)
            List<Survey> sqlSurveys = surveyService.findSurveys("SQL", false);
            assertTrue(sqlSurveys.contains(s1));
            assertFalse(sqlSurveys.contains(s2));

            // [FUNC-2] Buscar con keyword vacía (debería encontrar todas)
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

    // Método auxiliar para crear una encuesta en el pasado (para probar el filtro onlyFuture)
    // Necesario porque el servicio no permite crear encuestas con fecha de fin pasada.
    private Survey createPastSurveyRaw(String question) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "INSERT INTO Survey (question, creationDate, endDate, canceled, positiveResponses, negativeResponses) VALUES (?, ?, ?, ?, 0, 0)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                Timestamp pastDate = Timestamp.valueOf(LocalDateTime.now().minusDays(10));
                ps.setString(1, question);
                ps.setTimestamp(2, pastDate); // creationDate
                ps.setTimestamp(3, pastDate); // endDate (ya pasada)
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
            // [FUNC-2] Buscar solo futuras
            List<Survey> onlyFuture = surveyService.findSurveys("", true);
            assertTrue(onlyFuture.contains(futureSurvey));
            assertFalse(onlyFuture.contains(pastSurvey));

            // [FUNC-2] Buscar todas (incluidas pasadas)
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
}