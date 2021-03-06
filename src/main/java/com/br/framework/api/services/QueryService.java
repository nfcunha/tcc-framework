package com.br.framework.api.services;

import com.br.framework.core.database.connection.IConnectionPool;
import com.br.framework.core.database.query.QueryResult;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class QueryService {
    
    private static IConnectionPool connectionPool;
    
    private static void checkLogSql(final String sql) {
        if (LoggingService.isLoggingSql()) {
            LoggingService.info(QueryService.class, "QUERY: ".concat(sql));
        }
    }
    
    private static void checkLogSql(final String sql, final Object... arguments) {
        checkLogSql(sql);
        if (LoggingService.isLoggingSql() && arguments != null && arguments.length > 0) {
            final StringBuilder argsStr = new StringBuilder();
            argsStr.append("ARGUMENTS : [");
            for (final Object iterator : arguments) {
                argsStr.append("\"").append(String.valueOf(iterator)).append("\";");
            }
            argsStr.setLength(argsStr.length() - 1);
            argsStr.append("]");
            LoggingService.info(QueryService.class, argsStr.toString());
        }
    }
    
    public static boolean isPoolDefined() {
        return connectionPool != null;
    }
    
    public static void connectionPool(final IConnectionPool connectionPool) {
        QueryService.connectionPool = connectionPool;
    }
    
    public static QueryResult run(final String query) throws SQLException {
        checkLogSql(query);
        final Connection conn = connectionPool.open();
        final PreparedStatement preparedStatement = conn.prepareStatement(query);
        
        final ResultSet resultSet = preparedStatement.executeQuery();
        connectionPool.close(conn);
        return QueryResultFactory.build(resultSet);
    }
    
    public static QueryResult run(final String query, final Object... arguments) throws SQLException {
        checkLogSql(query, arguments);
        final Connection conn = connectionPool.open();
        final PreparedStatement preparedStatement = conn.prepareStatement(query);
        for (int i = 1; i <= arguments.length; i++) {
            preparedStatement.setObject(i, arguments[i - 1]);
        }
        
        final ResultSet resultSet = preparedStatement.executeQuery();
        connectionPool.close(conn);
        return QueryResultFactory.build(resultSet);
    }
    
    public static void execute(final String query) throws Exception {
        checkLogSql(query);
        final Connection conn = connectionPool.open();
        final PreparedStatement preparedStatement = conn.prepareStatement(query);
        preparedStatement.execute();
        connectionPool.close(conn);
    }
    
    public static void execute(final String query, final Object... arguments) throws SQLException {
        checkLogSql(query, arguments);
        final Connection conn = connectionPool.open();
        final PreparedStatement preparedStatement = conn.prepareStatement(query);
        for (int i = 1; i <= arguments.length; i++) {
            preparedStatement.setObject(i, arguments[i - 1]);
        }
        
        preparedStatement.execute();
        connectionPool.close(conn);
    }
    
    private static class QueryResultFactory {
        
        public static QueryResult build(final ResultSet resultSet) throws SQLException {
            final QueryResult sqlResult = new QueryResult();
            
            sqlResult.setRows(MetadataController.buildRowsFromResultSet(resultSet));
            sqlResult.setColumns(MetadataController.buildColumnsFromResultSet(resultSet));
            sqlResult.setAliasToRow(MetadataController.mapColumnAlias(resultSet));
            
            return sqlResult;
        }
    }
    
    private static class MetadataController {
        
        public static List<Map<String, Object>> buildRowsFromResultSet(final ResultSet resultSet) throws SQLException {
            final ResultSetMetaData resultSetMetadata = resultSet.getMetaData();
            final List<Map<String, Object>> rowList = new ArrayList<>();
            while (resultSet.next()) {
                final Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= resultSetMetadata.getColumnCount(); i++) {
                    row.put(resultSetMetadata.getColumnLabel(i), resultSet.getObject(i));
                }
                rowList.add(row);
            }
            return rowList;
        }
        
        public static List<String> buildColumnsFromResultSet(final ResultSet resultSet) throws SQLException {
            final ResultSetMetaData resultSetMetadata = resultSet.getMetaData();
            final List<String> fieldList = new ArrayList<>();
            resultSet.beforeFirst();
            if (resultSet.next()) {
                for (int i = 1; i <= resultSetMetadata.getColumnCount(); i++) {
                    fieldList.add(resultSetMetadata.getColumnName(i));
                }
            }
            return fieldList;
        }
        
        public static Map<String, String> mapColumnAlias(final ResultSet resultSet) throws SQLException {
            final Map<String, String> mapping = new LinkedHashMap<>();
            final ResultSetMetaData resultSetMetadata = resultSet.getMetaData();
            resultSet.beforeFirst();
            if (resultSet.next()) {
                for (int i = 1; i <= resultSetMetadata.getColumnCount(); i++) {
                    mapping.put(resultSetMetadata.getColumnName(i), resultSetMetadata.getColumnLabel(i));
                }
            }
            return mapping;
        }
        
    }
    
}
