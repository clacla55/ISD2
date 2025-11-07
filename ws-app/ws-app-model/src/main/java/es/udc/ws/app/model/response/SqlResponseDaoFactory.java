package es.udc.ws.app.model.response;

import es.udc.ws.util.configuration.ConfigurationParametersManager;

/**
 * Factoría para obtener instancias de {@link SqlResponseDao}.
 */
public class SqlResponseDaoFactory {

    private final static String CLASS_NAME_PARAMETER = "SqlResponseDao.className";
    private static SqlResponseDao dao = null;

    private SqlResponseDaoFactory() {
    }

    @SuppressWarnings("rawtypes")
    private static SqlResponseDao getInstance() {
        try {
            String daoClassName = ConfigurationParametersManager.getParameter(CLASS_NAME_PARAMETER);
            Class daoClass = Class.forName(daoClassName);
            return (SqlResponseDao) daoClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized static SqlResponseDao getDao() {
        if (dao == null) {
            dao = getInstance();
        }
        return dao;
    }
}