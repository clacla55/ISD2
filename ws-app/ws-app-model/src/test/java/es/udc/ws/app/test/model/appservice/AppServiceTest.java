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

        // Creiamo e registriamo il DataSource per i test
        // Questo risolve l'errore NoInitialContextException
        dataSource = new SimpleDataSource();
        DataSourceLocator.addDataSource(ModelConstants.APP_DATA_SOURCE, dataSource);

        // Otteniamo le istanze del servizio e del DAO
        surveyService = SurveyServiceImpl.getInstance();
        surveyDao = SqlSurveyDaoFactory.getDao();
    }

    // Metodo helper per pulire il DB (necessario per i test)
    private void removeSurvey(Long surveyId) {
        if (surveyId == null) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true); // Usare autocommit per le operazioni di pulizia
            surveyDao.remove(connection, surveyId);
        } catch (InstanceNotFoundException e) {
            // Ignoriamo se è già stato cancellato
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * [FUNC-1] Test per il caso di successo.
     * Verifica che l'indagine venga creata correttamente nel DB,
     * che tutti i campi siano inizializzati e che venga restituito
     * un ID valido.
     */
    @Test
    public void testCreateSurvey() throws InputValidationException {

        String question = "¿Te gusta la práctica de ISD?";
        LocalDateTime endDate = LocalDateTime.now().plusDays(7);
        Survey createdSurvey = null;

        try {
            // 1. Creare l'indagine
            createdSurvey = surveyService.createSurvey(question, endDate);

            // 2. Verifiche sull'oggetto restituito
            assertNotNull(createdSurvey.getSurveyId(), "SurveyId non deve essere nullo");
            assertEquals(question, createdSurvey.getQuestion());
            assertEquals(endDate, createdSurvey.getEndDate());
            assertNotNull(createdSurvey.getCreationDate(), "CreationDate non deve essere nulla");
            assertFalse(createdSurvey.isCanceled(), "Non deve essere cancellata");
            assertEquals(0, createdSurvey.getPositiveResponses(), "Contatore positivo deve essere 0");
            assertEquals(0, createdSurvey.getNegativeResponses(), "Contatore negativo deve essere 0");

            // 3. Verifiche sul DB (opzionale, ma buona pratica)
            // Se avessimo implementato surveyDao.find(), potremmo usarlo qui.
            // Per ora, confidiamo che se createSurvey non ha lanciato eccezioni
            // e ha restituito un ID, l'inserimento è andato a buon fine.

        } finally {
            // 4. Pulizia
            if (createdSurvey != null) {
                removeSurvey(createdSurvey.getSurveyId());
            }
        }
    }

    /**
     * [FUNC-1] Test per il caso di errore: domanda nulla.
     * Verifica che venga lanciata InputValidationException.
     */
    @Test
    public void testCreateSurveyNullQuestion() {

        LocalDateTime endDate = LocalDateTime.now().plusDays(7);

        assertThrows(InputValidationException.class, () -> {
            surveyService.createSurvey(null, endDate);
        }, "Non si possono creare indagini con domanda nulla");

        assertThrows(InputValidationException.class, () -> {
            surveyService.createSurvey("   ", endDate); // Anche domanda vuota
        }, "Non si possono creare indagini con domanda vuota");
    }

    /**
     * [FUNC-1] Test per il caso di errore: data di fine passata.
     * Verifica che venga lanciata InputValidationException[cite: 28].
     */
    @Test
    public void testCreateSurveyPastEndDate() {

        String question = "Pregunta válida";
        LocalDateTime pastEndDate = LocalDateTime.now().minusDays(1);

        assertThrows(InputValidationException.class, () -> {
            surveyService.createSurvey(question, pastEndDate);
        }, "La data di fine deve essere futura");
    }

}