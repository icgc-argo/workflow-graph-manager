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

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc_argo.workflow_graph_lib.utils.PatternMatch;
import org.icgc_argo.workflowgraphmanager.model.Node;
import org.icgc_argo.workflowgraphmanager.model.Pipeline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PipelineRepository {
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

  public PipelineRepository(@Autowired KubernetesClient kubernetesClient) {
    this.kubernetesClient = kubernetesClient;
  }

  public HashMap<String, Pipeline> getPipelines() {
    return buildPipelinesFromPodList(
        kubernetesClient.pods().withLabel(TYPE_LABEL_KEY, TYPE_LABEL_VAL).list().getItems());
  }

  private HashMap<String, Pipeline> buildPipelinesFromPodList(List<Pod> podList) {
    return podList.stream()
        .reduce(
            new HashMap<>(),
            (HashMap<String, Pipeline> pipelines, Pod pod) ->
                PatternMatch.<String, HashMap<String, Pipeline>>match(
                        pod.getMetadata().getLabels().get(APP_LABEL_KEY))
                    .on(
                        app ->
                            app.equalsIgnoreCase(APP_GRAPH_NODE)
                                || app.equalsIgnoreCase(APP_GRAPH_INGEST_NODE),
                        () -> {
                          val pipeline = getOrCreatePipeline(pod, pipelines);
                          val node =
                              pod.getMetadata()
                                      .getLabels()
                                      .get(APP_LABEL_KEY)
                                      .equalsIgnoreCase(APP_GRAPH_NODE)
                                  ? parsePodToNode(pod)
                                  : parsePodToIngestNode(pod);

                          // Add the new node to the list of nodes for the pipeline
                          pipeline.setNodes(
                              Stream.concat(Stream.of(node), pipeline.getNodes().stream())
                                  .collect(Collectors.toList()));

                          // Add/Replace the pipeline with the newly updated one
                          val pipelineId = getPipelineId(pod);

                          if (pipelines.containsKey(pipelineId)) {
                            pipelines.replace(pipelineId, pipeline);
                          } else {
                            pipelines.put(pipelineId, pipeline);
                          }

                          return pipelines;
                        })
                    .otherwise(
                        () -> {
                          log.warn(
                              "Unknown Workflow Graph App: {}",
                              pod.getMetadata().getLabels().get(APP_LABEL_KEY));
                          return pipelines;
                        }),
            (pipelinesA, pipelinesB) -> {
              throw new RuntimeException(
                  "Beware, here there be dragons ... in the form of reducer combinators somehow being called on a non-parallel stream reduce ...");
            });
  }

  private Pipeline getOrCreatePipeline(Pod pod, HashMap<String, Pipeline> pipelines) {
    return pipelines.getOrDefault(
        getPipelineId(pod),
        Pipeline.builder().id(getPipelineId(pod)).nodes(new ArrayList<>()).build());
  }

  private Node parsePodToNode(Pod pod) {
    return Node.builder().id(getNodeId(pod)).pipeline(getPipelineId(pod)).build();
  }

  private Node parsePodToIngestNode(Pod pod) {
    return Node.builder().id(getNodeId(pod)).pipeline(getPipelineId(pod)).build();
  }

  private String getPipelineId(Pod pod) {
    return pod.getMetadata().getLabels().get(PIPELINE_LABEL_KEY);
  }

  private String getNodeId(Pod pod) {
    return pod.getMetadata().getLabels().get(NODE_LABEL_KEY);
  }
}
