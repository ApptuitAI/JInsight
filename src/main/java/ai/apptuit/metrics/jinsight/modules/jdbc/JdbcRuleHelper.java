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

import ai.apptuit.metrics.client.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import ai.apptuit.metrics.jinsight.modules.common.RuleHelper;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.sql.DataSource;
import org.jboss.byteman.rule.Rule;

/**
 * @author Rajiv Shivane
 */
public class JdbcRuleHelper extends RuleHelper {

  public static final TagEncodedMetricName ROOT_NAME = TagEncodedMetricName.decode("jdbc");

  public static final TagEncodedMetricName GET_CONNECTION_NAME = ROOT_NAME
      .submetric("ds.getConnection");
  public static final TagEncodedMetricName PREPARE_STATEMENT_NAME = ROOT_NAME
      .submetric("conn.prepareStatement");
  public static final TagEncodedMetricName EXECUTE_STATEMENT_NAME = ROOT_NAME
      .submetric("ps.execute");

  private static final Timer GET_CONNECTION_TIMER = createTimer(GET_CONNECTION_NAME);
  private static final Timer PREPARE_STATEMENT_TIMER = createTimer(PREPARE_STATEMENT_NAME);


  private static final OperationId GET_CONNECTION_OPERATION = new OperationId(
      GET_CONNECTION_NAME.toString());
  private static final OperationId PREPARE_STATEMENT_OPERATION = new OperationId(
      PREPARE_STATEMENT_NAME.toString());
  private static final OperationId EXECUTE_STATEMENT_OPERATION = new OperationId(
      EXECUTE_STATEMENT_NAME.toString());

  private static final String PREP_STMT_SQL_QUERY_STRING = "jdbc.ps.sql";
  private static final StringUniqueIdService uidService = new StringUniqueIdService();

  public JdbcRuleHelper(Rule rule) {
    super(rule);
  }

  private static Timer createTimer(TagEncodedMetricName name) {
    MetricRegistry metricRegistry = RegistryService.getMetricRegistry();
    return metricRegistry.timer(name.toString());
  }

  public void onGetConnectionEntry(DataSource ds) {
    beginTimedOperation(GET_CONNECTION_OPERATION);
  }

  public void onGetConnectionExit(DataSource ds, Connection connection) {
    endTimedOperation(GET_CONNECTION_OPERATION, GET_CONNECTION_TIMER);
  }

  public void onGetConnectionError(DataSource ds) {
    endTimedOperation(GET_CONNECTION_OPERATION, () -> null);
  }

  public void onPrepareStatementEntry(Connection connection) {
    beginTimedOperation(PREPARE_STATEMENT_OPERATION);
  }

  public void onPrepareStatementExit(Connection connection, String sql, PreparedStatement ps) {
    //TODO add datasource name as a tag
    endTimedOperation(PREPARE_STATEMENT_OPERATION, PREPARE_STATEMENT_TIMER);
    setObjectProperty(ps, PREP_STMT_SQL_QUERY_STRING, sql);
  }

  public void onPrepareStatementError(Connection connection) {
    endTimedOperation(PREPARE_STATEMENT_OPERATION, () -> null);
  }

  public void onExecuteStatementEntry(PreparedStatement ps) {
    beginTimedOperation(EXECUTE_STATEMENT_OPERATION);
  }

  public void onExecuteStatementExit(PreparedStatement ps) {
    //TODO add datasource name & execution type (execute/executeUpdate/executeBath) as tags
    endTimedOperation(EXECUTE_STATEMENT_OPERATION, () -> {
      String sql = getObjectProperty(ps, PREP_STMT_SQL_QUERY_STRING);
      String sqlId = uidService.getUniqueId(sql);
      String metricName = EXECUTE_STATEMENT_NAME.withTags("sql", sqlId).toString();
      return RegistryService.getMetricRegistry().timer(metricName);
    });
  }

  public void onExecuteStatementError(PreparedStatement ps) {
    endTimedOperation(EXECUTE_STATEMENT_OPERATION, () -> null);
  }

}
