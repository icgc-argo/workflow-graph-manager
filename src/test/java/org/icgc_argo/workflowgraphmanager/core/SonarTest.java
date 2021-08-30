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

package org.icgc_argo.workflowgraphmanager.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc_argo.workflowgraphmanager.TestUtils.loadK8sWithBaseResourcesAnd;
import static org.icgc_argo.workflowgraphmanager.TestUtils.loadResource;
import static org.icgc_argo.workflowgraphmanager.utils.JacksonUtils.readValue;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import java.util.List;
import java.util.stream.Collectors;
import lombok.val;
import org.icgc_argo.workflowgraphmanager.graphql.model.Node;
import org.icgc_argo.workflowgraphmanager.graphql.model.Pipeline;
import org.icgc_argo.workflowgraphmanager.graphql.model.Queue;
import org.icgc_argo.workflowgraphmanager.repository.GraphNodeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@EnableKubernetesMockClient(crud = true)
public class SonarTest {
  static KubernetesClient client;
  private final GraphNodeRepository graphNodeRepository;
  private final Sonar sonar;

  public SonarTest() {
    loadK8sWithBaseResourcesAnd(client, "fixtures/pods/multi-pipeline.json");
    this.graphNodeRepository = new GraphNodeRepository(client);
    this.sonar = new Sonar(graphNodeRepository, 10L);
  }

  @Test
  public void testPipelinesAssembly() {
    val pipelines = sonar.getPipelines();

    assertThat(pipelines.size()).isEqualTo(3);

    assertThat(
            pipelines.stream()
                .map(Pipeline::getId)
                .collect(Collectors.toList())
                .containsAll(List.of("test-pipeline", "test-pipeline-two", "test-pipeline-three")))
        .isTrue();

    val pipelineOne = sonar.getPipelineById("test-pipeline");
    assertThat(pipelineOne.getNodes())
        .hasSameElementsAs(
            sonar.getNodes().stream()
                .filter(node -> node.getPipeline().equalsIgnoreCase("test-pipeline"))
                .collect(Collectors.toList()));
    assertThat(pipelineOne.getQueues().stream().map(Queue::getId))
        .containsExactlyInAnyOrderElementsOf(
            List.of(
                "test-pipeline.start.start.start",
                "test-pipeline.align-node.start.align-node-start",
                "test-pipeline.align-node.queued-align-node.queued-align-node",
                "test-pipeline.align-node.align-node-running.align-node-running",
                "test-pipeline.align-node.align-node-complete.align-node-complete"));

    val pipelineTwo = sonar.getPipelineById("test-pipeline-two");
    assertThat(pipelineTwo.getNodes())
        .hasSameElementsAs(
            sonar.getNodes().stream()
                .filter(node -> node.getPipeline().equalsIgnoreCase("test-pipeline-two"))
                .collect(Collectors.toList()));
    assertThat(pipelineTwo.getQueues().stream().map(Queue::getId))
        .containsExactlyInAnyOrderElementsOf(
            List.of(
                "test-pipeline-two.start-two.start.start",
                "test-pipeline-two.align-node-two.start.align-node-two-start",
                "test-pipeline-two.align-node-two.queued-align-node-two.queued-align-node-two",
                "test-pipeline-two.align-node-two.align-node-two-running.align-node-two-running",
                "test-pipeline-two.align-node-two.align-node-two-complete.align-node-two-complete"));

    val pipelineThree = sonar.getPipelineById("test-pipeline-three");
    assertThat(pipelineThree.getNodes())
        .hasSameElementsAs(
            sonar.getNodes().stream()
                .filter(node -> node.getPipeline().equalsIgnoreCase("test-pipeline-three"))
                .collect(Collectors.toList()));
    assertThat(pipelineThree.getQueues().stream().map(Queue::getId))
        .containsExactlyInAnyOrderElementsOf(
            List.of(
                "test-pipeline-three.start-three.start.start",
                "test-pipeline-three.align-node-three.start.align-node-three-start",
                "test-pipeline-three.align-node-three.queued-align-node-three.queued-align-node-three",
                "test-pipeline-three.align-node-three.align-node-three-running.align-node-three-running",
                "test-pipeline-three.align-node-three.align-node-three-complete.align-node-three-complete"));
  }

  @Test
  public void testQueueExtraction() {
    val queues = sonar.getQueues();
    val expectedQueues =
        sonar.getNodes().stream()
            .flatMap(node -> node.getQueues().stream())
            .collect(Collectors.toList());
    assertThat(queues).containsExactlyInAnyOrderElementsOf(expectedQueues);
  }

  @Test
  public void testNewNodeAddition() {
    // load new pods
    client
        .configMaps()
        .create(
            readValue(
                loadResource("fixtures/configmaps/variant-caller-node-config.json"),
                ConfigMap.class));
    client
        .pods()
        .create(readValue(loadResource("fixtures/pods/variant-caller-pod.json"), Pod.class));

    // do update
    sonar.shallowUpdateOnNext(graphNodeRepository.getNodes());

    // Test that nodes have the update
    assertThat(sonar.getNodes().stream().map(Node::getId)).containsOnlyOnce("variant-caller-node");

    // Test Pipeline assembly is as expected
    val updatePipeline = sonar.getPipelineById("test-pipeline");
    assertThat(updatePipeline.getNodes().size()).isEqualTo(3);
    assertThat(
        updatePipeline.getNodes().stream()
            .anyMatch(node -> node.getId().equalsIgnoreCase("variant-caller-node")));
    assertThat(updatePipeline.getQueues().stream().map(Queue::getId))
        .containsExactlyInAnyOrderElementsOf(
            List.of(
                "test-pipeline.start.start.start",
                "test-pipeline.align-node.start.align-node-start",
                "test-pipeline.align-node.queued-align-node.queued-align-node",
                "test-pipeline.align-node.align-node-running.align-node-running",
                "test-pipeline.align-node.align-node-complete.align-node-complete",
                "test-pipeline.variant-caller-node.start.variant-caller-node-start",
                "test-pipeline.variant-caller-node.queued-variant-caller-node.queued-variant-caller-node",
                "test-pipeline.variant-caller-node.variant-caller-node-running.variant-caller-node-running",
                "test-pipeline.variant-caller-node.variant-caller-node-complete.variant-caller-node-complete"));

    // Test that Queues are updated
    assertThat(sonar.getQueues().stream().map(Queue::getId))
        .containsOnlyOnceElementsOf(
            List.of(
                "test-pipeline.variant-caller-node.start.variant-caller-node-start",
                "test-pipeline.variant-caller-node.queued-variant-caller-node.queued-variant-caller-node",
                "test-pipeline.variant-caller-node.variant-caller-node-running.variant-caller-node-running",
                "test-pipeline.variant-caller-node.variant-caller-node-complete.variant-caller-node-complete"));
  }
}
