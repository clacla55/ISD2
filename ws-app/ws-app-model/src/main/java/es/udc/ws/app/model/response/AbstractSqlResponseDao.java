package es.udc.ws.app.model.response;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Implementación abstracta de {@link SqlResponseDao} para características comunes.
 */
public abstract class AbstractSqlResponseDao implements SqlResponseDao {

    protected AbstractSqlResponseDao() {
    }

    /**
     * Método auxiliar para convertir una fila del ResultSet en un objeto Response.
     */
    protected Response getResponseFromResultSet(ResultSet resultSet) throws SQLException {

        int i = 1;
        Long responseId = resultSet.getLong(i++);
        Long surveyId = resultSet.getLong(i++);
        String employeeEmail = resultSet.getString(i++);
        boolean responseValue = resultSet.getBoolean(i++);
        Timestamp responseDateTs = resultSet.getTimestamp(i++);

        return new Response(responseId, surveyId, employeeEmail, responseValue,
                responseDateTs.toLocalDateTime());
    }

}