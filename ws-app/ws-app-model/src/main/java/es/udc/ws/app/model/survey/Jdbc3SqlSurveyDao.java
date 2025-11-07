package es.udc.ws.app.model.survey;

import es.udc.ws.util.exceptions.InstanceNotFoundException;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

        // [FUNC-4] Actualizamos los contadores y otros campos posibles
        String queryString = "UPDATE Survey"
                + " SET question = ?, creationDate = ?, endDate = ?, "
                + " canceled = ?, positiveResponses = ?, negativeResponses = ?"
                + " WHERE surveyId = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            int i = 1;
            preparedStatement.setString(i++, survey.getQuestion());
            preparedStatement.setTimestamp(i++, Timestamp.valueOf(survey.getCreationDate()));
            preparedStatement.setTimestamp(i++, Timestamp.valueOf(survey.getEndDate()));
            preparedStatement.setBoolean(i++, survey.isCanceled());
            preparedStatement.setLong(i++, survey.getPositiveResponses());
            preparedStatement.setLong(i++, survey.getNegativeResponses());
            preparedStatement.setLong(i++, survey.getSurveyId());

            int updatedRows = preparedStatement.executeUpdate();

            if (updatedRows == 0) {
                throw new InstanceNotFoundException(survey.getSurveyId(),
                        Survey.class.getName());
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Survey find(Connection connection, Long surveyId) throws InstanceNotFoundException {

        String queryString = "SELECT surveyId, question, creationDate, endDate, canceled, "
                + "positiveResponses, negativeResponses FROM Survey WHERE surveyId = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            preparedStatement.setLong(1, surveyId);

            // Ejecutar la consulta
            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                // Si no se encuentra, lanzar excepción
                throw new InstanceNotFoundException(surveyId, Survey.class.getName());
            }

            // Usamos el método auxiliar de la clase abstracta
            return getSurveyFromResultSet(resultSet);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Survey> findByKeyword(Connection connection, String keyword, boolean onlyFuture) {

        // [FUNC-2] Búsqueda de encuestas por palabra clave y/o fecha futura
        String queryString = "SELECT surveyId, question, creationDate, endDate, canceled, "
                + "positiveResponses, negativeResponses FROM Survey";

        // Determinar si hay palabra clave para filtrar
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();

        // Construir la cláusula WHERE dinámicamente
        if (hasKeyword) {
            queryString += " WHERE LOWER(question) LIKE LOWER(?)";
        }

        if (onlyFuture) {
            queryString += (hasKeyword ? " AND" : " WHERE") + " endDate > ?";
        }

        // Ordenar por fecha de creación descendente (las más recientes primero)
        queryString += " ORDER BY creationDate DESC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            int i = 1;
            if (hasKeyword) {
                // Usar % para búsqueda parcial
                preparedStatement.setString(i++, "%" + keyword + "%");
            }
            if (onlyFuture) {
                // Filtrar encuestas cuya fecha de fin sea posterior a ahora
                preparedStatement.setTimestamp(i++, Timestamp.valueOf(LocalDateTime.now()));
            }

            ResultSet resultSet = preparedStatement.executeQuery();
            List<Survey> surveys = new ArrayList<>();

            while (resultSet.next()) {
                // Usamos el método auxiliar de la clase abstracta
                surveys.add(getSurveyFromResultSet(resultSet));
            }

            return surveys;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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