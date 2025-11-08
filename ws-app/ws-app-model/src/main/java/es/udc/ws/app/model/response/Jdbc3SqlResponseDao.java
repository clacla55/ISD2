package es.udc.ws.app.model.response;

import es.udc.ws.util.exceptions.InstanceNotFoundException;

import java.sql.*;
import java.util.ArrayList; // [FUNC-6] Necesario añadir este import
import java.util.List;
import java.util.Optional;

public class Jdbc3SqlResponseDao extends AbstractSqlResponseDao {

    @Override
    public Response create(Connection connection, Response response) {

        String queryString = "INSERT INTO Response"
                + " (surveyId, employeeEmail, response, responseDate)"
                + " VALUES (?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(
                queryString, Statement.RETURN_GENERATED_KEYS)) {

            int i = 1;
            preparedStatement.setLong(i++, response.getSurveyId());
            preparedStatement.setString(i++, response.getEmployeeEmail());
            preparedStatement.setBoolean(i++, response.getResponse());
            preparedStatement.setTimestamp(i++, Timestamp.valueOf(response.getResponseDate()));

            preparedStatement.executeUpdate();

            ResultSet resultSet = preparedStatement.getGeneratedKeys();

            if (!resultSet.next()) {
                throw new SQLException("JDBC driver did not return generated key.");
            }
            Long responseId = resultSet.getLong(1);

            response.setResponseId(responseId);
            return response;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(Connection connection, Response response) throws InstanceNotFoundException {

        String queryString = "UPDATE Response"
                + " SET response = ?, responseDate = ?"
                + " WHERE responseId = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            int i = 1;
            preparedStatement.setBoolean(i++, response.getResponse());
            preparedStatement.setTimestamp(i++, Timestamp.valueOf(response.getResponseDate()));
            preparedStatement.setLong(i++, response.getResponseId());

            int updatedRows = preparedStatement.executeUpdate();

            if (updatedRows == 0) {
                throw new InstanceNotFoundException(response.getResponseId(),
                        Response.class.getName());
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Response find(Connection connection, Long responseId) throws InstanceNotFoundException {
        String queryString = "SELECT responseId, surveyId, employeeEmail, response, responseDate "
                + "FROM Response WHERE responseId = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            preparedStatement.setLong(1, responseId);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                throw new InstanceNotFoundException(responseId, Response.class.getName());
            }

            return getResponseFromResultSet(resultSet);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Response> findBySurveyAndEmployee(Connection connection, Long surveyId, String employeeEmail) {

        String queryString = "SELECT responseId, surveyId, employeeEmail, response, responseDate "
                + "FROM Response WHERE surveyId = ? AND employeeEmail = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            preparedStatement.setLong(1, surveyId);
            preparedStatement.setString(2, employeeEmail);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                return Optional.empty();
            }

            return Optional.of(getResponseFromResultSet(resultSet));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Response> findBySurveyId(Connection connection, Long surveyId, boolean onlyPositive) {

        // [FUNC-6] Implementación de la búsqueda de respuestas
        String queryString = "SELECT responseId, surveyId, employeeEmail, response, responseDate "
                + "FROM Response WHERE surveyId = ?";

        // Si se solicitan sólo positivas, añadimos la condición
        if (onlyPositive) {
            queryString += " AND response = ?";
        }

        // Ordenamos por fecha de respuesta descendente (las más recientes primero)
        queryString += " ORDER BY responseDate DESC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            int i = 1;
            preparedStatement.setLong(i++, surveyId);

            if (onlyPositive) {
                // true representa una respuesta positiva en la BD
                preparedStatement.setBoolean(i++, true);
            }

            ResultSet resultSet = preparedStatement.executeQuery();
            List<Response> responses = new ArrayList<>();

            while (resultSet.next()) {
                responses.add(getResponseFromResultSet(resultSet));
            }

            return responses;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void remove(Connection connection, Long responseId) throws InstanceNotFoundException {
        String queryString = "DELETE FROM Response WHERE responseId = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            preparedStatement.setLong(1, responseId);
            int removedRows = preparedStatement.executeUpdate();

            if (removedRows == 0) {
                throw new InstanceNotFoundException(responseId, Response.class.getName());
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}