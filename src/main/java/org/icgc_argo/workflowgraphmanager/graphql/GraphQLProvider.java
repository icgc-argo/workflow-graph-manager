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

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

import com.apollographql.federation.graphqljava.Federation;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import graphql.GraphQL;
import graphql.execution.AsyncExecutionStrategy;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import java.io.IOException;
import java.net.URL;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.icgc_argo.workflow_graph_lib.utils.PatternMatch;
import org.icgc_argo.workflowgraphmanager.config.websecurity.AuthProperties;
import org.icgc_argo.workflowgraphmanager.graphql.model.GraphLog;
import org.icgc_argo.workflowgraphmanager.graphql.model.Node;
import org.icgc_argo.workflowgraphmanager.graphql.model.Pipeline;
import org.icgc_argo.workflowgraphmanager.graphql.model.Queue;
import org.icgc_argo.workflowgraphmanager.graphql.security.VerifyAuthQueryExecutionStrategyDecorator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GraphQLProvider {

  private final SonarDataFetcher sonarDataFetcher;
  private final GraphLogDataFetcher graphLogDataFetcher;
  private final EntityDataFetcher entityDataFetcher;
  private final AuthProperties authProperties;
  private GraphQL graphQL;
  private GraphQLSchema graphQLSchema;

  @Autowired
  public GraphQLProvider(
      SonarDataFetcher sonarDataFetcher,
      GraphLogDataFetcher graphLogDataFetcher,
      EntityDataFetcher entityDataFetcher,
      AuthProperties authProperties) {
    this.sonarDataFetcher = sonarDataFetcher;
    this.graphLogDataFetcher = graphLogDataFetcher;
    this.entityDataFetcher = entityDataFetcher;
    this.authProperties = authProperties;
  }

  @Bean
  @Profile("!secure")
  public GraphQL graphQL() {
    return graphQL;
  }

  @Bean
  @Profile("secure")
  public GraphQL secureGraphQL() {
    return graphQL.transform(this::toSecureGraphql);
  }

  private void toSecureGraphql(GraphQL.Builder graphQLBuilder) {
    // For more info on `Execution Strategies` see:
    // https://www.graphql-java.com/documentation/v15/execution/
    graphQLBuilder.queryExecutionStrategy(
        new VerifyAuthQueryExecutionStrategyDecorator(
            new AsyncExecutionStrategy(), queryScopesToCheck()));
  }

  @PostConstruct
  public void init() throws IOException {
    URL url = Resources.getResource("schema.graphql");
    String sdl = Resources.toString(url, Charsets.UTF_8);
    this.graphQLSchema = buildSchema(sdl);
    this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
  }

  private GraphQLSchema buildSchema(String sdl) {
    return Federation.transform(sdl, buildWiring())
        .fetchEntities(entityDataFetcher.getDataFetcher())
        .resolveEntityType(
            typeResolutionEnvironment ->
                PatternMatch.<Object, GraphQLObjectType>match(typeResolutionEnvironment.getObject())
                    .on(
                        src -> (src instanceof Pipeline),
                        () ->
                            typeResolutionEnvironment
                                .getSchema()
                                .getObjectType(EntityDataFetcher.PIPELINE_ENTITY))
                    .on(
                        src -> (src instanceof Node),
                        () ->
                            typeResolutionEnvironment
                                .getSchema()
                                .getObjectType(EntityDataFetcher.NODE_ENTITY))
                    .on(
                        src -> (src instanceof Queue),
                        () ->
                            typeResolutionEnvironment
                                .getSchema()
                                .getObjectType(EntityDataFetcher.QUEUE_ENTITY))
                    .on(
                        src -> (src instanceof GraphLog),
                        () ->
                            typeResolutionEnvironment
                                .getSchema()
                                .getObjectType(EntityDataFetcher.GRAPH_LOG_ENTITY))
                    .otherwise(() -> null))
        .build();
  }

  private RuntimeWiring buildWiring() {
    return RuntimeWiring.newRuntimeWiring()
        .scalar(ExtendedScalars.Json)
        .type(
            newTypeWiring("Query")
                .dataFetcher("pipelines", sonarDataFetcher.getPipelineDataFetcher()))
        .type(newTypeWiring("Query").dataFetcher("nodes", sonarDataFetcher.getNodeDataFetcher()))
        .type(newTypeWiring("Query").dataFetcher("queues", sonarDataFetcher.getQueueDataFetcher()))
        .type(
            newTypeWiring("Query")
                .dataFetcher("logs", graphLogDataFetcher.getGraphLogsDataFetcher()))
        .type(
            newTypeWiring("Query")
                .dataFetcher("logAggs", graphLogDataFetcher.getAggregateGraphLogsDataFetcher()))
        .type(
            newTypeWiring("Pipeline")
                .dataFetcher("logs", graphLogDataFetcher.getLogsForPipelineDataFetcher()))
        .type(
            newTypeWiring("Node")
                .dataFetcher("pipeline", sonarDataFetcher.getNestedPipelineDataFetcher())
                .dataFetcher("logs", graphLogDataFetcher.getLogsForNodeDataFetcher()))
        .type(
            newTypeWiring("Queue")
                .dataFetcher("pipeline", sonarDataFetcher.getNestedPipelineDataFetcher())
                .dataFetcher("node", sonarDataFetcher.getNestedNodeDataFetcher())
                .dataFetcher("logs", graphLogDataFetcher.getLogsForQueueDataFetcher()))
        .type(newTypeWiring("Message").typeResolver(new MessageTypeResolver()))
        .type(
            newTypeWiring("GraphLog")
                .dataFetcher("pipeline", sonarDataFetcher.getNestedPipelineDataFetcher())
                .dataFetcher("node", sonarDataFetcher.getNestedNodeDataFetcher())
                .dataFetcher("queue", sonarDataFetcher.getNestedQueueDataFetcher()))
        .build();
  }

  private ImmutableList<String> queryScopesToCheck() {
    return ImmutableList.copyOf(
        Iterables.concat(
            authProperties.getGraphqlScopes().getQueryOnly(),
            authProperties.getGraphqlScopes().getQueryAndMutation()));
  }
}
