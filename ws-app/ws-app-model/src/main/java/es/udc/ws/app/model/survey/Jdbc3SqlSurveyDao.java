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
        // No es necesario para FUNC-1 ni FUNC-3
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Survey find(Connection connection, Long surveyId) throws InstanceNotFoundException {

        // [FUNC-3] Consulta para recuperar una encuesta por su ID
        String queryString = "SELECT question, creationDate, endDate, canceled, "
                + "positiveResponses, negativeResponses FROM Survey WHERE surveyId = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            preparedStatement.setLong(1, surveyId);

            // Ejecutar la consulta
            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                // Si no se encuentra, lanzar excepción
                throw new InstanceNotFoundException(surveyId, Survey.class.getName());
            }

            // Recuperar datos del ResultSet
            String question = resultSet.getString(1);
            Timestamp creationDateAsTimestamp = resultSet.getTimestamp(2);
            LocalDateTime creationDate = creationDateAsTimestamp.toLocalDateTime();
            Timestamp endDateAsTimestamp = resultSet.getTimestamp(3);
            LocalDateTime endDate = endDateAsTimestamp.toLocalDateTime();
            boolean canceled = resultSet.getBoolean(4);
            long positiveResponses = resultSet.getLong(5);
            long negativeResponses = resultSet.getLong(6);

            // Devolver el objeto Survey reconstruido
            return new Survey(surveyId, question, creationDate, endDate, canceled,
                    positiveResponses, negativeResponses);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Survey> findByKeyword(Connection connection, String keyword, boolean onlyFuture) {
        // No es necesario para FUNC-1 ni FUNC-3
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