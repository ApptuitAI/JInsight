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

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.modules.common.RuleHelper;
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

  private static final OperationId GET_CONNECTION_OPERATION = new OperationId(
      GET_CONNECTION_NAME.toString());
  private static final OperationId PREPARE_STATEMENT_OPERATION = new OperationId(
      PREPARE_STATEMENT_NAME.toString());
  private static final OperationId EXECUTE_STATEMENT_OPERATION = new OperationId(
      EXECUTE_STATEMENT_NAME.toString());

  public JdbcRuleHelper(Rule rule) {
    super(rule);
  }

  public void onGetConnectionEntry(DataSource ds) {
    beginTimedOperation(GET_CONNECTION_OPERATION);
  }

  public void onGetConnectionExit(DataSource ds) {
    endTimedOperation(GET_CONNECTION_OPERATION, GET_CONNECTION_NAME);
  }

  public void onPrepareStatementEntry(Connection connection) {
    beginTimedOperation(PREPARE_STATEMENT_OPERATION);
  }

  public void onPrepareStatementExit(Connection connection) {
    //TODO add datasource name as a tag
    endTimedOperation(PREPARE_STATEMENT_OPERATION, PREPARE_STATEMENT_NAME);
  }

  public void onExecuteStatementEntry(PreparedStatement ps) {
    beginTimedOperation(EXECUTE_STATEMENT_OPERATION);
  }

  public void onExecuteStatementExit(PreparedStatement ps) {
    //TODO add datasource name & execution type (execute/executeUpdate/executeBath) as tags
    endTimedOperation(EXECUTE_STATEMENT_OPERATION, EXECUTE_STATEMENT_NAME);
  }
}
