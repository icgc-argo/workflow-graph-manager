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
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc_argo.workflow_graph_lib.utils.PatternMatch;
import org.icgc_argo.workflowgraphmanager.model.Node;
import org.icgc_argo.workflowgraphmanager.model.Pipeline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class PipelineRepository {
  /**
   * label for all pipeline pods as per
   * https://wiki.oicr.on.ca/display/icgcargotech/Kubernetes+Labelling
   */
  private final String TYPE_LABEL_KEY = "common.org.icgc.argo/type";

  private final String TYPE_LABEL_VAL = "workflow-graph";

  private final String APP_LABEL_KEY = "workflow-graph.org.icgc.argo/app";
  private final String APP_GRAPH_NODE = "workflow-graph-node";
  private final String APP_GRAPH_INGEST_NODE = "workflow-graph-ingest-node";

  private final String PIPELINE_LABEL_KEY = "workflow-graph.org.icgc.argo/pipeline-id";
  private final String NODE_LABEL_KEY = "workflow-graph.org.icgc.argo/node-id";

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
                        app -> app.equalsIgnoreCase(APP_GRAPH_NODE),
                        () -> {
                          // Get labels from pod meta
                          val pipelineId = pod.getMetadata().getLabels().get(PIPELINE_LABEL_KEY);
                          val nodeId = pod.getMetadata().getLabels().get(NODE_LABEL_KEY);

                          // Build new pipeline object if not already present in accumulator
                          val pipeline =
                              pipelines.getOrDefault(
                                  pipelineId, Pipeline.builder().id(pipelineId).nodes(new ArrayList<>()) .build());

                          // Build new node
                          val node = Node.builder().id(nodeId).build();

                          // Add the new node to the list of nodes for the pipeline
                          pipeline.setNodes(
                              Stream.concat(Stream.of(node), pipeline.getNodes().stream())
                                  .collect(Collectors.toList()));

                          // Replace the pipeline with the newly updated one
                          pipelines.replace(pipelineId, pipeline);

                          return pipelines;
                        })
                    .on(app -> app.equalsIgnoreCase(APP_GRAPH_INGEST_NODE), () -> pipelines)
                    .otherwise(() -> pipelines),
            (pipelinesA, pipelinesB) -> {
              throw new RuntimeException(
                  "Beware, here there be dragons ... in the form of reducer combinators somehow being called on a non-parallel stream reduce ...");
            });
  }

  private Node parsePodToNode(Pod pod, Pipeline pipeline) {
    return Node.builder()
        .id(pod.getMetadata().getLabels().get(NODE_LABEL_KEY))
        .pipeline(pipeline)
        .build();
  }

  //  public Pipeline getPipeline() {}
}
