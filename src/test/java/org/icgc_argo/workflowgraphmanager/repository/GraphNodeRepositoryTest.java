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

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc_argo.workflowgraphmanager.utils.JacksonUtils.readValue;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.val;
import org.icgc_argo.workflow_graph_lib.utils.JacksonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

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

    // Test pipeline is correct
    assertThat(pipelines.keySet().size()).isEqualTo(1);
    assertThat(pipelines.containsKey("test-pipeline")).isTrue();
    assertThat(pipelines.get("test-pipeline").getGraphNodes().size()).isEqualTo(2);
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

  private void loadK8sWithBaseResourcesAnd(Stream<Pod> pods) {
    // load config map
    client.configMaps().create(readValue(this.getClass().getResourceAsStream("fixtures/align-node-config.json"), ConfigMap.class));

    // read decoy pods
    val decoyPodsJson =
        readValue(this.getClass().getResourceAsStream("fixtures/decoy-pods.json"), Map.class);

    // Merge pods with decoy stream and load mock K8s
    Stream.concat(pods, ((List<Map<String, Object>>) decoyPodsJson.get("items")).stream())
        .map(podJson -> JacksonUtils.convertValue(podJson, Pod.class))
        .forEach(pod -> client.pods().create(pod));
  }
}
