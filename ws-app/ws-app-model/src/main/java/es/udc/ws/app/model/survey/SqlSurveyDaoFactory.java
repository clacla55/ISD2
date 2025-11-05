package es.udc.ws.app.model.survey;

import es.udc.ws.util.configuration.ConfigurationParametersManager;

public class SqlSurveyDaoFactory {

    private final static String CLASS_NAME_PARAMETER = "SqlSurveyDao.className";
    private static SqlSurveyDao dao = null;

    private SqlSurveyDaoFactory() {
    }

    @SuppressWarnings("rawtypes")
    private static SqlSurveyDao getInstance() {
        try {
            String daoClassName = ConfigurationParametersManager.getParameter(CLASS_NAME_PARAMETER);
            Class daoClass = Class.forName(daoClassName);
            return (SqlSurveyDao) daoClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized static SqlSurveyDao getDao() {
        if (dao == null) {
            dao = getInstance();
        }
        return dao;
    }
}