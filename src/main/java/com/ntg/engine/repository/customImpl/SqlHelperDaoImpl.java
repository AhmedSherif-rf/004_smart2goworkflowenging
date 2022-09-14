package com.ntg.engine.repository.customImpl;

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.transaction.Transactional;

import com.ntg.common.NTGMessageOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Repository;

import com.ntg.engine.repository.custom.SqlHelperDao;
import com.ntg.engine.util.Setting;

@Repository
public class SqlHelperDaoImpl implements SqlHelperDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private NamedParameterJdbcTemplate namedParamJdbcTemplate;

    @Autowired
    public void setDataSource(javax.sql.DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedParamJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public List<Map<String, Object>> queryForList(String query) {
        List<Map<String, Object>> objs = jdbcTemplate.query(query, new ColumnMapRowMapper() {

            @Override
            protected String getColumnKey(String columnName) {
                return columnName.toLowerCase();
            }
        });
        return objs;
    }

    @Override
    public List<Map<String, Object>> queryForList(String query, Object[] params) {
        List<Map<String, Object>> objs = jdbcTemplate.query(query, params, new ColumnMapRowMapper() {

            @Override
            protected String getColumnKey(String columnName) {
                return columnName.toLowerCase();
            }
        });
        return objs;
    }

    @Override
    public void updateTable(List<Long> recids, String sql) {
        Map<String, List<Long>> params = Collections.singletonMap("recids", recids);
        // String sql = "update adm_schedule_conf set job_Running=1 where recid in
        // (:recids)";
        namedParamJdbcTemplate.update(sql, params);
    }

    @Override
    public long FtechRecID(String SeqnaceName) {
        long nextId = jdbcTemplate.queryForObject(this.SquanceFetchSql(SeqnaceName), Long.class);
        return nextId;
    }

    public void RunUpdateSql(String Sql) {
        this.jdbcTemplate.update(Sql);
    }

    private String SquanceFetchSql(String SequanceName) {
        String Sql = "";
        if (this.getConnectionType() == 1) {// oracle
            Sql = "Select " + SequanceName + ".NEXTVAL from dual";
        } else {// postgres syntax
            Sql = "select nextval('" + SequanceName + "') ";
        }
        return Sql;
    }

    public int getConnectionType() { // return 1 for oracle AND 2 FOR POSTGRES

        return (Setting.databasetype.indexOf("oracle") > -1) ? 1 : 2;
    }

    public String LimitSql(int limitSize) {
        String Sql = "";
        if (this.getConnectionType() == 1) {// oracle
            Sql = " rowNum <  " + limitSize;
        } else { // postgres syntax
            Sql = " Limit (" + limitSize + ")";
        }
        return Sql;
    }

    public String[] BuildCreateTableSql(String TableName, String TableFields, String PrimaryKeyField, String tenantSchema) {
        String[] createUdaTableQuery = new String[]{"", ""};
        if (this.getConnectionType() == 1) {// oracle
            createUdaTableQuery[0] = "declare" + "  eAlreadyExists exception;"
                    + "  pragma exception_init(eAlreadyExists, -00955);" + " begin"
                    + " execute immediate 'CREATE TABLE " + tenantSchema + TableName + " ( " + TableFields
                    + ((PrimaryKeyField != null)
                    ? (", CONSTRAINT " + TableName + "_p PRIMARY KEY (" + PrimaryKeyField + ")")
                    : "")
                    + ")';" + " exception when eAlreadyExists then" + " null;" + " end;";
            // Sequence Creation Syntax
            createUdaTableQuery[1] = "declare" + "  eAlreadyExists exception;"
                    + "  pragma exception_init(eAlreadyExists, -00955);" + " begin"
                    + " execute immediate 'CREATE SEQUENCE  " + tenantSchema + TableName + "_s';"
                    + " exception when eAlreadyExists then" + " null;" + " end;";
        } else { // postgres syntax

            createUdaTableQuery = new String[]{"", ""};
            TableFields = TableFields.replace("NUMBER(1,0)", "Boolean");
            TableFields = TableFields.replace("raw(255)", "uuid");
            createUdaTableQuery[0] = "CREATE TABLE IF NOT EXISTS " + tenantSchema + TableName + " ( " + TableFields
                    + ((PrimaryKeyField != null)
                    ? (",CONSTRAINT " + TableName + "_p PRIMARY KEY (" + PrimaryKeyField + ")")
                    : "")
                    + ")";
            // Sequence Creation Syntax
            createUdaTableQuery[1] = "CREATE SEQUENCE if not EXISTS " + tenantSchema + TableName + "_s";
        }
        return createUdaTableQuery;
    }

    @Override
    @Transactional
    public List<Map<String, Object>> queryForObjectWithLobItem(String query, Long emailTemplateId,
                                                               String propertyName) {
        List<Map<String, Object>> obj = jdbcTemplate.query(query, new Object[]{emailTemplateId}, (resultSet, i) -> {
            return toMapObject(resultSet, propertyName);
        });
        return obj;
    }

    @SuppressWarnings("resource")
    @Transactional
    private Map<String, Object> toMapObject(ResultSet resultSet, String propertyName) {
        Map<String, Object> mapOfObject = new HashMap<String, Object>();
        try {
            ResultSetMetaData rsmd = resultSet.getMetaData();
            int columnCount = rsmd.getColumnCount();
            for (int x = 1; x < columnCount + 1; x++) {
                String column = JdbcUtils.lookupColumnName(rsmd, x).toLowerCase();
                if (propertyName.equalsIgnoreCase(column)) {
                    InputStream contentStream = resultSet.getClob(propertyName).getAsciiStream();
                    String content = new Scanner(contentStream, "UTF-8").useDelimiter("\\A").next();
                    mapOfObject.put(propertyName, content);
                } else {
                    mapOfObject.put(column, resultSet.getObject(x));
                }
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            NTGMessageOperation.PrintErrorTrace(e);
        }

        return mapOfObject;
    }

}
