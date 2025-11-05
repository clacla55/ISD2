package es.udc.ws.app.model.survey;

import es.udc.ws.util.exceptions.InstanceNotFoundException;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class Jdbc3SqlSurveyDao extends AbstractSqlSurveyDao {

    @Override
    public Survey create(Connection connection, Survey survey) {

        String queryString = "INSERT INTO Survey"
                + " (question, creationDate, endDate, canceled, "
                + " positiveResponses, negativeResponses)"
                + " VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(
                queryString, Statement.RETURN_GENERATED_KEYS)) {

            // Establecer los parámetros
            preparedStatement.setString(1, survey.getQuestion());
            preparedStatement.setTimestamp(2, Timestamp.valueOf(survey.getCreationDate()));
            preparedStatement.setTimestamp(3, Timestamp.valueOf(survey.getEndDate()));
            preparedStatement.setBoolean(4, survey.isCanceled());
            preparedStatement.setLong(5, survey.getPositiveResponses());
            preparedStatement.setLong(6, survey.getNegativeResponses());

            // Realizar la inserción
            preparedStatement.executeUpdate();

            // Obtener el ID generado
            ResultSet resultSet = preparedStatement.getGeneratedKeys();

            if (!resultSet.next()) {
                throw new SQLException("JDBC driver did not return generated key.");
            }
            Long surveyId = resultSet.getLong(1);

            // Actualiza el objeto de encuesta con el ID y devuélvelo.
            survey.setSurveyId(surveyId);
            return survey;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(Connection connection, Survey survey) throws InstanceNotFoundException {
        // No es necesario para FUNC-1
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Survey find(Connection connection, Long surveyId) throws InstanceNotFoundException {
        // No es necesario para FUNC-1
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Survey> findByKeyword(Connection connection, String keyword, boolean onlyFuture) {
        // No es necesario para FUNC-1
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void remove(Connection connection, Long surveyId) throws InstanceNotFoundException {

        String queryString = "DELETE FROM Survey WHERE surveyId = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            preparedStatement.setLong(1, surveyId);
            int removedRows = preparedStatement.executeUpdate();

            if (removedRows == 0) {
                throw new InstanceNotFoundException(surveyId, Survey.class.getName());
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}