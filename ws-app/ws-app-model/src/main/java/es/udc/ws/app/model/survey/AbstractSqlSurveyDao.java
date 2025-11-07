package es.udc.ws.app.model.survey;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Implementación abstracta de {@link SqlSurveyDao} que incluye características comunes.
 */
public abstract class AbstractSqlSurveyDao implements SqlSurveyDao {

    protected AbstractSqlSurveyDao() {
    }

    /**
     * Método auxiliar para convertir una fila del ResultSet en un objeto Survey.
     * Se usa en find y findByKeyword para evitar duplicación de código.
     */
    protected Survey getSurveyFromResultSet(ResultSet resultSet) throws SQLException {

        Long surveyId = resultSet.getLong(1);
        String question = resultSet.getString(2);
        Timestamp creationDateAsTimestamp = resultSet.getTimestamp(3);
        LocalDateTime creationDate = creationDateAsTimestamp.toLocalDateTime();
        Timestamp endDateAsTimestamp = resultSet.getTimestamp(4);
        LocalDateTime endDate = endDateAsTimestamp.toLocalDateTime();
        boolean canceled = resultSet.getBoolean(5);
        long positiveResponses = resultSet.getLong(6);
        long negativeResponses = resultSet.getLong(7);

        return new Survey(surveyId, question, creationDate, endDate, canceled,
                positiveResponses, negativeResponses);
    }

}