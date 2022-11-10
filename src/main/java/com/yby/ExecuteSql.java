package com.yby;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExecuteSql {

    private String path;

    private JdbcTemplate jdbcTemplate;

    private String fileName;

    protected void setFileName(String fileName) {
        this.fileName = fileName;
    }

    protected void setPath(String path) {
        this.path = path;
    }

    protected void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getPath() {
        return this.path;
    }

    public File createDataSqlFile(List<String> table) throws IOException {
        File file = new File(path);
        if (!file.exists()) file.mkdirs();
        File sqlFile = new File(file, fileName);
        if (sqlFile.exists()) sqlFile.delete();
        FileWriter writer = new FileWriter(sqlFile);
        try {
            writeSql(writer, table);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            writer.flush();
            writer.close();
        }
        return sqlFile;
    }

    public void executeSql(File sql) throws Exception {
        if (!sql.exists()) throw new FileNotFoundException("SQL文件缺失");
        BufferedReader reader = new BufferedReader(new FileReader(sql));
        DataSource source = jdbcTemplate.getDataSource();
        if (source == null) {
            throw new NullPointerException("数据源为空");
        }
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
            String s;
            while ((s = reader.readLine()) != null) {
                statement.addBatch(s);
            }
            statement.executeBatch();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void writeSql(FileWriter writer, List<String> tables) throws Exception {
        writer.write("SET FOREIGN_KEY_CHECKS = 0;");
        writer.write("\n");
        for (String table : tables) {
            List<String> insertSql = getInsertSql(table);
            for (String s : insertSql) {
                String replace = s.replace("\r\n", "\\r\\n").replace("\n", "\\n");
                writer.write(replace);
                writer.write("\n");
            }
        }
        writer.write("SET FOREIGN_KEY_CHECKS = 1;");
    }

    private List<String> getInsertSql(String table) throws SQLException {
        ResultSet resultSet = null;
        List<String> sqls = new ArrayList<>();
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(String.format("select * from %s", table));
            resultSet = preparedStatement.executeQuery();
            ResultSetMetaData metaData = resultSet.getMetaData();
            //列数
            int columnCount = metaData.getColumnCount();
            //获取insertSql
            while (resultSet.next()) {
                String insertSql = insertSqlProccess(resultSet, columnCount, table, metaData);
                sqls.add(insertSql);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            resultSet.close();
        }
        return sqls;
    }

    private String getCreateTableSql(String table) throws Exception {
        String createTableSql = null;
        ResultSet resultSet = null;
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            String sql = String.format("show create table %s", table);
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            resultSet.next();
            createTableSql = resultSet.getString(2);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            resultSet.close();
        }
        if (createTableSql == null) throw new Exception("获取创建表语句失败!");
        return createTableSql;
    }

    private String insertSqlProccess(ResultSet rs, int columnCount, String table, ResultSetMetaData metaData) throws SQLException {
        StringBuilder sb = new StringBuilder();
        StringBuilder columnName = new StringBuilder();
        StringBuilder values = new StringBuilder();
        sb.append("REPLACE INTO ").append(table).append(" (");
        for (int i = 1; i <= columnCount; i++) {
            String data = rs.getString(i);
            columnName.append("`").append(metaData.getColumnName(i)).append("`");
            if (data != null) {
                values.append("'").append(data).append("'");
            } else {
                values.append("null");
            }
            if (i != columnCount) {
                columnName.append(",");
                values.append(",");
            }
        }
        sb.append(columnName).append(")").append(" VALUES (").append(values).append(");");
        return sb.toString();
    }
}
