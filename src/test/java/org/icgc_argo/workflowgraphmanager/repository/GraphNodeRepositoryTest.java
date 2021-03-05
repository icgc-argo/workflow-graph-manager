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

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import lombok.val;
import org.icgc_argo.workflow_graph_lib.utils.JacksonUtils;
import org.icgc_argo.workflowgraphmanager.repository.model.GraphExchangesQueue;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc_argo.workflowgraphmanager.utils.JacksonUtils.readValue;

@ActiveProfiles("test")
@EnableKubernetesMockClient(crud = true)
public class GraphNodeRepositoryTest {

  static KubernetesClient client;

  private final GraphNodeRepository graphNodeRepository;

  public GraphNodeRepositoryTest() {
    this.graphNodeRepository = new GraphNodeRepository(client);
  }

  @Test
  public void singlePipelineTest() {
    val simplePipelineJson =
        readValue(this.getClass().getResourceAsStream("fixtures/single-pipeline.json"), Map.class);

    loadK8sWithBaseResourcesAnd(
        ((List<Map<String, Object>>) simplePipelineJson.get("items"))
            .stream().map(podJson -> JacksonUtils.convertValue(podJson, Pod.class)));

    val nodes = graphNodeRepository.getNodes().collect(Collectors.toList());
    val pipelines = graphNodeRepository.getPipelines();

    // Test nodes list is correct
    assertThat(nodes.size()).isEqualTo(2);
    assertThat(nodes.stream().anyMatch(node -> node.getId().equalsIgnoreCase("start"))).isTrue();
    assertThat(nodes.stream().anyMatch(node -> node.getId().equalsIgnoreCase("align-node")))
        .isTrue();

    // Test pipeline is correct
    assertThat(pipelines.keySet().size()).isEqualTo(1);
    assertThat(pipelines.containsKey("test-pipeline")).isTrue();
    assertThat(pipelines.get("test-pipeline").getGraphNodes()).hasSameElementsAs(nodes);
  }

  @Test
  public void multiPipelinePipelineTest() {
    val multiPipelineJson =
        readValue(this.getClass().getResourceAsStream("fixtures/multi-pipeline.json"), Map.class);

    loadK8sWithBaseResourcesAnd(
        ((List<Map<String, Object>>) multiPipelineJson.get("items"))
            .stream().map(podJson -> JacksonUtils.convertValue(podJson, Pod.class)));

    val nodes = graphNodeRepository.getNodes().collect(Collectors.toList());
    val pipelines = graphNodeRepository.getPipelines();

    // Test nodes list is correct
    assertThat(nodes.size()).isEqualTo(6);
    assertThat(nodes.stream().anyMatch(node -> node.getId().equalsIgnoreCase("start"))).isTrue();
    assertThat(nodes.stream().anyMatch(node -> node.getId().equalsIgnoreCase("align-node")))
        .isTrue();
    assertThat(nodes.stream().anyMatch(node -> node.getId().equalsIgnoreCase("start-two")))
        .isTrue();
    assertThat(nodes.stream().anyMatch(node -> node.getId().equalsIgnoreCase("align-node-two")))
        .isTrue();
    assertThat(nodes.stream().anyMatch(node -> node.getId().equalsIgnoreCase("start-three")))
        .isTrue();
    assertThat(nodes.stream().anyMatch(node -> node.getId().equalsIgnoreCase("align-node-three")))
        .isTrue();

    // Test pipeline is correct
    assertThat(pipelines.keySet().size()).isEqualTo(3);
    assertThat(
            pipelines
                .keySet()
                .containsAll(List.of("test-pipeline", "test-pipeline-two", "test-pipeline-three")))
        .isTrue();
    assertThat(pipelines.get("test-pipeline").getGraphNodes()).hasSameElementsAs(nodes.stream().filter(node -> node.getPipeline().equalsIgnoreCase("test-pipeline")).collect(Collectors.toList()));
    assertThat(pipelines.get("test-pipeline-two").getGraphNodes()).hasSameElementsAs(nodes.stream().filter(node -> node.getPipeline().equalsIgnoreCase("test-pipeline-two")).collect(Collectors.toList()));
    assertThat(pipelines.get("test-pipeline-three").getGraphNodes()).hasSameElementsAs(nodes.stream().filter(node -> node.getPipeline().equalsIgnoreCase("test-pipeline-three")).collect(Collectors.toList()));
  }

  @Test
  public void graphNodeConfigTest() {
    val simplePipelineJson =
        readValue(this.getClass().getResourceAsStream("fixtures/single-pipeline.json"), Map.class);

    loadK8sWithBaseResourcesAnd(
        ((List<Map<String, Object>>) simplePipelineJson.get("items"))
            .stream().map(podJson -> JacksonUtils.convertValue(podJson, Pod.class)));

    val pod = client.pods().withName("align-node-workflow-graph-node-86cf986995-c5gvk").get();
    val config = graphNodeRepository.getGraphNodeConfig(pod);

    assertThat(config.getPipelineId()).isEqualTo("test-pipeline");
    assertThat(config.getNodeId()).isEqualTo("align-node");
  }

  @Test
  public void graphIngestNodeConfigTest() {
    val simplePipelineJson =
        readValue(this.getClass().getResourceAsStream("fixtures/single-pipeline.json"), Map.class);

    loadK8sWithBaseResourcesAnd(
        ((List<Map<String, Object>>) simplePipelineJson.get("items"))
            .stream().map(podJson -> JacksonUtils.convertValue(podJson, Pod.class)));

    val pod = client.pods().withName("ingest-workflow-graph-ingest-769f477677-64cp8").get();
    val config = graphNodeRepository.getGraphIngestNodeConfig(pod);

    assertThat(config.getInboundKafkaTopic()).isEqualTo("wfg-test");
    assertThat(config.getOutboundRabbitExchangeQueue()).isEqualTo("start");
  }

  @Test
  public void graphNodeQueueTest() {
    val simplePipelineJson =
        readValue(this.getClass().getResourceAsStream("fixtures/single-pipeline.json"), Map.class);

    loadK8sWithBaseResourcesAnd(
        ((List<Map<String, Object>>) simplePipelineJson.get("items"))
            .stream().map(podJson -> JacksonUtils.convertValue(podJson, Pod.class)));

    val nodes = graphNodeRepository.getNodes().collect(Collectors.toList());

    val ingestPod =
        nodes.stream().filter(node -> node.getId().equalsIgnoreCase("start")).findFirst().get();
    val nodePod =
        nodes.stream()
            .filter(node -> node.getId().equalsIgnoreCase("align-node"))
            .findFirst()
            .get();

    assertThat(ingestPod.getGraphExchangesQueueList())
        .containsOnly(GraphExchangesQueue.fromExchangeString("start"));

    assertThat(nodePod.getGraphExchangesQueueList())
        .containsExactly(
            new GraphExchangesQueue("start", "align-node"),
            new GraphExchangesQueue("queued-align-node", "align-node"),
            new GraphExchangesQueue("align-node-running", "align-node-running"),
            new GraphExchangesQueue("align-node-complete", "align-node-complete"));
  }

  private void loadK8sWithBaseResourcesAnd(Stream<Pod> pods) {
    // clear all pods
    client.pods().delete();

    // load all configmaps
    List.of(
            "configmaps/align-node-config.json",
            "configmaps/align-node-two-config.json",
            "configmaps/align-node-three-config.json")
        .forEach(
            configMapName -> {
              // load config map
              client
                  .configMaps()
                  .create(
                      readValue(
                          this.getClass().getResourceAsStream(configMapName), ConfigMap.class));
            });

    // read decoy pods
    val decoyPodsJson =
        readValue(this.getClass().getResourceAsStream("fixtures/decoy-pods.json"), Map.class);

    // Merge pods with decoy stream and load mock K8s
    Stream.concat(pods, ((List<Map<String, Object>>) decoyPodsJson.get("items")).stream())
        .map(podJson -> JacksonUtils.convertValue(podJson, Pod.class))
        .forEach(pod -> client.pods().create(pod));
  }
}
