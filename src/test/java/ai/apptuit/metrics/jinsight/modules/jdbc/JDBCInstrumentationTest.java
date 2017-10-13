/*
 * Copyright 2017 Agilx, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.apptuit.metrics.jinsight.modules.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.tools.RunScript;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Rajiv Shivane
 */
public class JDBCInstrumentationTest {

  private static final String CREATE_TABLE_STRING = "CREATE TABLE TEST(ID  VARCHAR(255) PRIMARY KEY, VALUE INT);";
  private static final String SELECT_QUERY = "SELECT * FROM TEST where ID=?";
  private static final int POOL_SIZE = 5;

  private MetricRegistry registry;
  private Map<String, Integer> presetElements;
  private List<String> presetElementKeys;
  private DataSource datasource;
  private TagEncodedMetricName executeStatementName;

  @Before
  public void setUp() throws Exception {
    registry = RegistryService.getMetricRegistry();

    String sqlId = new StringUniqueIdService().getUniqueId(SELECT_QUERY);
    executeStatementName = JdbcRuleHelper.EXECUTE_STATEMENT_NAME.withTags("sql", sqlId);

    presetElements = IntStream.range(0, 1000).boxed()
        .collect(Collectors.toMap(i -> UUID.randomUUID().toString(), i -> i));
    presetElementKeys = new ArrayList<>(presetElements.keySet());

    StringBuilder sb = new StringBuilder(CREATE_TABLE_STRING);
    presetElements.forEach((s, o) -> {
      sb.append("\nINSERT INTO TEST VALUES('").append(s).append("', ").append(o).append(");");
    });

    String dbName = "JDBCInstrumentationTest." + UUID.randomUUID();
    datasource = JdbcConnectionPool.create("jdbc:h2:mem:" + dbName, "h2", "h2");
    ((JdbcConnectionPool) datasource).setMaxConnections(POOL_SIZE);
    datasource.setLoginTimeout(-1);
    Connection conn = datasource.getConnection();
    RunScript.execute(conn, new StringReader(sb.toString()));
    conn.close();
  }

  @Test
  public void testGetConnection() throws Exception {
    long expectedCount = getTimerCount(JdbcRuleHelper.GET_CONNECTION_NAME) + 1;

    Connection connection = datasource.getConnection();
    connection.close();

    assertEquals(expectedCount, getTimerCount(JdbcRuleHelper.GET_CONNECTION_NAME));
  }


  @Test
  public void testGetConnectionError() throws Exception {
    List<Connection> connections = new ArrayList<>(POOL_SIZE);
    for (int i = 0; i < POOL_SIZE; i++) {
      connections.add(datasource.getConnection());
    }
    long expectedCount = getTimerCount(JdbcRuleHelper.GET_CONNECTION_NAME);
    boolean gotTimeout = false;
    try {
      Connection connection = datasource.getConnection();
    } catch (SQLException e) {
      gotTimeout = true;
    }
    assertTrue(gotTimeout);
    assertEquals(expectedCount, getTimerCount(JdbcRuleHelper.GET_CONNECTION_NAME));

    for (Connection connection : connections) {
      connection.close();
    }

    expectedCount++;

    Connection connection = datasource.getConnection();
    assertEquals(expectedCount, getTimerCount(JdbcRuleHelper.GET_CONNECTION_NAME));
    connection.close();


  }

  @Test
  public void testPrepareStatement() throws Exception {
    long expectedCount = getTimerCount(JdbcRuleHelper.PREPARE_STATEMENT_NAME) + 1;

    Connection connection = datasource.getConnection();
    connection.prepareStatement(SELECT_QUERY);
    connection.close();

    assertEquals(expectedCount,
        getTimerCount(JdbcRuleHelper.PREPARE_STATEMENT_NAME));
  }


  @Test
  public void testPrepareStatementError() throws Exception {
    long expectedCount = getTimerCount(JdbcRuleHelper.PREPARE_STATEMENT_NAME);

    Connection connection = datasource.getConnection();
    boolean gotSqlException = false;
    try {
      connection.prepareStatement("DIE! DIE! DIE!");
    } catch (SQLException e) {
      gotSqlException = true;
    }
    assertTrue(gotSqlException);
    assertEquals(expectedCount,
        getTimerCount(JdbcRuleHelper.PREPARE_STATEMENT_NAME));

    expectedCount++;
    connection.prepareStatement(SELECT_QUERY);
    connection.close();

    assertEquals(expectedCount,
        getTimerCount(JdbcRuleHelper.PREPARE_STATEMENT_NAME));
  }

  @Test
  public void testPreparedStatementExecute() throws Exception {
    long expectedCount = getTimerCount(executeStatementName) + 1;

    Connection connection = datasource.getConnection();
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_QUERY);

    int rnd = ThreadLocalRandom.current().nextInt(0, presetElements.size());
    String key = presetElementKeys.get(rnd);
    Integer value = presetElements.get(key);

    preparedStatement.setString(1, key);

//    ResultSet resultSet = preparedStatement.executeQuery();
    preparedStatement.execute();
    ResultSet resultSet = preparedStatement.getResultSet();
    resultSet.next();
    assertEquals(value.intValue(), resultSet.getInt(2));
    connection.close();

    assertEquals(expectedCount,
        getTimerCount(executeStatementName));
  }


  @Test
  public void testPreparedStatementExecuteError() throws Exception {
    long expectedCount = getTimerCount(executeStatementName);

    Connection connection = datasource.getConnection();
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_QUERY);

    boolean gotSqlException = false;
    try {
      preparedStatement.execute();
    } catch (SQLException e) {
      gotSqlException = true;
    }
    assertTrue(gotSqlException);
    assertEquals(expectedCount,
        getTimerCount(executeStatementName));

    expectedCount++;
    int rnd = ThreadLocalRandom.current().nextInt(0, presetElements.size());
    String key = presetElementKeys.get(rnd);
    Integer value = presetElements.get(key);

    preparedStatement.setString(1, key);

//    ResultSet resultSet = preparedStatement.executeQuery();
    preparedStatement.execute();
    ResultSet resultSet = preparedStatement.getResultSet();
    resultSet.next();
    assertEquals(value.intValue(), resultSet.getInt(2));
    connection.close();

    assertEquals(expectedCount,
        getTimerCount(executeStatementName));
  }

  private long getTimerCount(TagEncodedMetricName name) {
    Timer timer = registry.getTimers().get(name.toString());
    return timer != null ? timer.getCount() : 0;
  }
}
