/*
 * Copyright (c) 2021 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc_argo.workflowgraphmanager.graphql;

import com.apollographql.federation.graphqljava._Entity;
import graphql.schema.DataFetcher;
import lombok.extern.slf4j.Slf4j;
import org.icgc_argo.workflow_graph_lib.utils.PatternMatch;
import org.icgc_argo.workflowgraphmanager.core.Sonar;
import org.icgc_argo.workflowgraphmanager.service.GraphLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Slf4j
@Component
public class EntityDataFetcher {
  public static final String PIPELINE_ENTITY = "Pipeline";
  public static final String NODE_ENTITY = "Node";
  public static final String QUEUE_ENTITY = "Queue";
  public static final String GRAPH_LOG_ENTITY = "GraphLog";

  private final Sonar sonar;
  private final GraphLogService graphLogService;

  @Autowired
  public EntityDataFetcher(Sonar sonar, GraphLogService graphLogService) {
    this.sonar = sonar;
    this.graphLogService = graphLogService;
  }

  public DataFetcher getDataFetcher() {
    return environment ->
        environment.<List<Map<String, Object>>>getArgument(_Entity.argumentName).stream()
            .map(
                values ->
                    PatternMatch.match(values.get("__typename"))
                        .on(
                            typename -> typename.equals(GRAPH_LOG_ENTITY),
                            () -> {
                              final Object graphMessageId = values.get("graphMessageId");
                              if (graphMessageId instanceof String) {
                                return graphLogService.getGraphLogByGraphMessageId(
                                    (String) graphMessageId);
                              } else {
                                return null;
                              }
                            })
                        .on(
                            typename -> typename.equals(PIPELINE_ENTITY),
                            () -> {
                              final Object pipelineId = values.get("id");
                              if (pipelineId instanceof String) {
                                return sonar.getPipelineById((String) pipelineId);
                              } else {
                                return null;
                              }
                            })
                        .on(
                            typename -> typename.equals(NODE_ENTITY),
                            () -> {
                              final Object nodeId = values.get("id");
                              if (nodeId instanceof String) {
                                return sonar.getNodeById((String) nodeId);
                              } else {
                                return null;
                              }
                            })
                        .on(
                            typename -> typename.equals(QUEUE_ENTITY),
                            () -> {
                              final Object queueId = values.get("id");
                              if (queueId instanceof String) {
                                return sonar.getQueueById((String) queueId);
                              } else {
                                return null;
                              }
                            })
                        .otherwise(() -> null))
            .collect(toList());
  }
}
