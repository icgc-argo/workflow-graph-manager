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
import static org.icgc_argo.workflowgraphmanager.TestUtils.loadK8sWithBaseResourcesAnd;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import java.util.List;
import java.util.stream.Collectors;
import lombok.val;
import org.icgc_argo.workflowgraphmanager.repository.model.GraphExchangesQueue;
import org.icgc_argo.workflowgraphmanager.repository.model.GraphNode;
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
    loadK8sWithBaseResourcesAnd(client, "fixtures/pods/single-pipeline.json");

    val nodes = graphNodeRepository.getNodes().collect(Collectors.toList());

    // Test nodes list is correct
    assertThat(nodes.size()).isEqualTo(2);
    assertThat(nodes.stream().anyMatch(node -> node.getId().equalsIgnoreCase("start"))).isTrue();
    assertThat(nodes.stream().anyMatch(node -> node.getId().equalsIgnoreCase("align-node")))
        .isTrue();
  }

  @Test
  public void multiPipelinePipelineTest() {
    loadK8sWithBaseResourcesAnd(client, "fixtures/pods/multi-pipeline.json");

    val nodes = graphNodeRepository.getNodes().collect(Collectors.toList());
    val expectedNodeIds =
        List.of(
            "start",
            "align-node",
            "start-two",
            "align-node-two",
            "start-three",
            "align-node-three");

    // Test nodes list is correct
    assertThat(nodes.size()).isEqualTo(6);
    assertThat(nodes.stream().map(GraphNode::getId))
        .containsExactlyInAnyOrderElementsOf(expectedNodeIds);
  }

  @Test
  public void graphNodeConfigTest() {
    loadK8sWithBaseResourcesAnd(client, "fixtures/pods/single-pipeline.json");

    val pod = client.pods().withName("align-node-workflow-graph-node-86cf986995-c5gvk").get();
    val config = graphNodeRepository.parseGraphNodeConfig(pod);

    assertThat(config.getPipelineId()).isEqualTo("test-pipeline");
    assertThat(config.getNodeId()).isEqualTo("align-node");
  }

  @Test
  public void graphIngestNodeConfigTest() {
    loadK8sWithBaseResourcesAnd(client, "fixtures/pods/single-pipeline.json");

    val pod = client.pods().withName("ingest-workflow-graph-ingest-769f477677-64cp8").get();
    val config = graphNodeRepository.parseGraphIngestNodeConfig(pod);

    assertThat(config.getInboundKafkaTopic()).isEqualTo("wfg-test");
    assertThat(config.getOutboundRabbitExchangeQueue()).isEqualTo("start");
  }

  @Test
  public void graphNodeQueueTest() {
    loadK8sWithBaseResourcesAnd(client, "fixtures/pods/single-pipeline.json");

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
        .containsExactlyInAnyOrder(
            new GraphExchangesQueue("start", "align-node-start"),
            new GraphExchangesQueue("queued-align-node", "queued-align-node"),
            new GraphExchangesQueue("align-node-running", "align-node-running"),
            new GraphExchangesQueue("align-node-complete", "align-node-complete"));
  }
}
