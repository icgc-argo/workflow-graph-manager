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

package org.icgc_argo.workflowgraphmanager.repository;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc_argo.workflowgraphmanager.repository.model.GraphIngestNodeConfig;
import org.icgc_argo.workflowgraphmanager.repository.model.GraphNode;
import org.icgc_argo.workflowgraphmanager.repository.model.GraphNodeConfig;
import org.icgc_argo.workflowgraphmanager.repository.model.Pipeline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.icgc_argo.workflowgraphmanager.utils.JacksonUtils.jsonStringToNodeConfig;

@Slf4j
@Component
public class GraphNodeRepository {
  /**
   * label for all pipeline pods as per
   * https://wiki.oicr.on.ca/display/icgcargotech/Kubernetes+Labelling
   */
  static final String TYPE_LABEL_KEY = "common.org.icgc.argo/type";

  static final String TYPE_LABEL_VAL = "workflow-graph";

  static final String APP_LABEL_KEY = "workflow-graph.org.icgc.argo/app";
  static final String APP_GRAPH_NODE = "workflow-graph-node";
  static final String APP_GRAPH_INGEST_NODE = "workflow-graph-ingest-node";

  static final String PIPELINE_LABEL_KEY = "workflow-graph.org.icgc.argo/pipeline-id";
  static final String NODE_LABEL_KEY = "workflow-graph.org.icgc.argo/node-id";

  private final KubernetesClient kubernetesClient;

  public GraphNodeRepository(@Autowired KubernetesClient kubernetesClient) {
    this.kubernetesClient = kubernetesClient;
  }

  public Stream<GraphNode<?>> getNodes() {
    return kubernetesClient
        .pods()
        .withLabel(TYPE_LABEL_KEY, TYPE_LABEL_VAL)
        .list()
        .getItems()
        .stream()
        .map(
            pod -> {
              if (pod.getMetadata()
                  .getLabels()
                  .get(APP_LABEL_KEY)
                  .equalsIgnoreCase(APP_GRAPH_NODE)) {
                return parsePodToNode(pod);
              } else if (pod.getMetadata()
                  .getLabels()
                  .get(APP_LABEL_KEY)
                  .equalsIgnoreCase(APP_GRAPH_INGEST_NODE)) {
                return parsePodToIngestNode(pod);
              } else {
                log.warn("Unknown Workflow Graph Node Type, pod description: {}", pod);
                return null;
              }
            })
        .filter(Objects::nonNull);
  }

  public HashMap<String, Pipeline> getPipelines() {
    return getNodes()
        .reduce(
            new HashMap<>(),
            (HashMap<String, Pipeline> pipelines, GraphNode<?> graphNode) -> {
              val pipeline = getOrCreatePipeline(graphNode, pipelines);

              // Add the new node to the list of nodes for the pipeline
              pipeline.setGraphNodes(
                  Stream.concat(Stream.of(graphNode), pipeline.getGraphNodes().stream())
                      .collect(Collectors.toList()));

              if (pipelines.containsKey(pipeline.getId())) {
                pipelines.replace(pipeline.getId(), pipeline);
              } else {
                pipelines.put(pipeline.getId(), pipeline);
              }

              return pipelines;
            },
            this::handleReduceHashMapConflict);
  }

  GraphNodeConfig getGraphNodeConfig(Pod pod) {
    return pod.getSpec().getVolumes().stream()
        .filter(vol -> vol.getName().endsWith("-config"))
        .flatMap(
            vol ->
                kubernetesClient
                    .configMaps()
                    .withName(vol.getConfigMap().getName())
                    .get()
                    .getData()
                    .values()
                    .stream())
        .reduce(
            new GraphNodeConfig(),
            (config, configString) -> jsonStringToNodeConfig(configString),
            this::handleReduceHashMapConflict);
  }

  // TODO: look at magic string usage here
  GraphIngestNodeConfig getGraphIngestNodeConfig(Pod pod) {
    return pod.getSpec().getContainers().stream()
        .filter(container -> container.getName().equalsIgnoreCase("workflow-graph-ingest"))
        .flatMap(container -> container.getEnv().stream())
        .filter(
            keyVal ->
                keyVal
                    .getName()
                    .equalsIgnoreCase("SPRING_CLOUD_STREAM_BINDINGS_INBOUND_DESTINATION"))
        .map(EnvVar::getValue)
        .reduce(
            new GraphIngestNodeConfig(),
            (acc, curr) ->
                GraphIngestNodeConfig.builder()
                    .inboundKafkaTopic(curr)
                    .outboundRabbitExchangeQueue("start") // todo: especially too magical here
                    .build(),
            this::handleReduceHashMapConflict);
  }

  private GraphNode<GraphNodeConfig> parsePodToNode(Pod pod) {
    return GraphNode.<GraphNodeConfig>builder()
        .id(getNodeId(pod))
        .pipeline(getPipelineId(pod))
        .config(getGraphNodeConfig(pod))
        .build();
  }

  private GraphNode<GraphIngestNodeConfig> parsePodToIngestNode(Pod pod) {
    return GraphNode.<GraphIngestNodeConfig>builder()
        .id(getNodeId(pod))
        .pipeline(getPipelineId(pod))
        .config(getGraphIngestNodeConfig(pod))
        .build();
  }

  private String getPipelineId(Pod pod) {
    return pod.getMetadata().getLabels().get(PIPELINE_LABEL_KEY);
  }

  private String getNodeId(Pod pod) {
    return pod.getMetadata().getLabels().get(NODE_LABEL_KEY);
  }

  private Pipeline getOrCreatePipeline(
      GraphNode<?> graphNode, HashMap<String, Pipeline> pipelines) {
    return pipelines.getOrDefault(
        graphNode.getPipeline(),
        Pipeline.builder().id(graphNode.getPipeline()).graphNodes(new ArrayList<>()).build());
  }

  private <T> T handleReduceHashMapConflict(T a, T b) {
    throw new RuntimeException(
        "Beware, here there be dragons ... in the form of reducer combinators somehow being called on a non-parallel stream reduce ...");
  }
}
