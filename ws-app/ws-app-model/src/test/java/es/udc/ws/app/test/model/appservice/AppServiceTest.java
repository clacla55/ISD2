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

import static org.junit.jupiter.api.Assertions.*;

public class AppServiceTest {

    private static SurveyService surveyService = null;
    private static SqlSurveyDao surveyDao = null;
    private static DataSource dataSource = null;

    @BeforeAll
    public static void init() {

        // Creamos y registramos el DataSource para las pruebas
        dataSource = new SimpleDataSource();
        DataSourceLocator.addDataSource(ModelConstants.APP_DATA_SOURCE, dataSource);

        // Obtenemos las instancias del servicio y del DAO
        surveyService = SurveyServiceImpl.getInstance();
        surveyDao = SqlSurveyDaoFactory.getDao();
    }

    // Método auxiliar para limpiar la BD (necesario para las pruebas)
    private void removeSurvey(Long surveyId) {
        if (surveyId == null) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true); // Usar autocommit para las operaciones de limpieza
            surveyDao.remove(connection, surveyId);
        } catch (InstanceNotFoundException e) {
            // Ignoramos si ya ha sido eliminado
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * [FUNC-1] Prueba de éxito.
     * Verifica que la encuesta se haya creado correctamente en la base de datos,
     * que todos los campos estén inicializados y que se devuelva un ID válido.
     */
    @Test
    public void testCreateSurvey() throws InputValidationException {

        String question = "¿Te gusta la práctica de ISD?";
        LocalDateTime endDate = LocalDateTime.now().plusDays(7);
        Survey createdSurvey = null;

        try {
            // 1. Crear la encuesta
            createdSurvey = surveyService.createSurvey(question, endDate);

            // 2. Verificaciones sobre el objeto devuelto
            assertNotNull(createdSurvey.getSurveyId(), "SurveyId no debe ser nulo");
            assertEquals(question, createdSurvey.getQuestion());
            assertEquals(endDate, createdSurvey.getEndDate());
            assertNotNull(createdSurvey.getCreationDate(), "CreationDate no debe ser nula");
            assertFalse(createdSurvey.isCanceled(), "No debe estar cancelada");
            assertEquals(0, createdSurvey.getPositiveResponses(), "Contador positivo debe ser 0");
            assertEquals(0, createdSurvey.getNegativeResponses(), "Contador negativo debe ser 0");

            // 3. Verificaciones en la BD (opcional, pero buena práctica)
            // Si hubiéramos implementado surveyDao.find(), podríamos usarlo aquí.
            // Por ahora, confiamos en que si createSurvey no ha lanzado excepciones
            // y ha devuelto un ID, la inserción ha ido bien.

        } finally {
            // 4. Limpieza
            if (createdSurvey != null) {
                removeSurvey(createdSurvey.getSurveyId());
            }
        }
    }

    /**
     * [FUNC-1] Prueba para el caso de error: pregunta nula.
     * Verifica que se lance InputValidationException.
     */
    @Test
    public void testCreateSurveyNullQuestion() {

        LocalDateTime endDate = LocalDateTime.now().plusDays(7);

        assertThrows(InputValidationException.class, () -> {
            surveyService.createSurvey(null, endDate);
        }, "No se pueden crear encuestas con pregunta nula");

        assertThrows(InputValidationException.class, () -> {
            surveyService.createSurvey("   ", endDate); // También pregunta vacía
        }, "No se pueden crear encuestas con pregunta vacía");
    }

    /**
     * [FUNC-1] Prueba para el caso de error: fecha de finalización pasada.
     * Verifica que se lance InputValidationException.
     */
    @Test
    public void testCreateSurveyPastEndDate() {

        String question = "Pregunta válida";
        LocalDateTime pastEndDate = LocalDateTime.now().minusDays(1);

        assertThrows(InputValidationException.class, () -> {
            surveyService.createSurvey(question, pastEndDate);
        }, "La fecha de fin debe ser futura");
    }

}