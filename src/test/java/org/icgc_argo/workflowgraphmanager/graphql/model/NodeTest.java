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

package org.icgc_argo.workflowgraphmanager.graphql.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import lombok.val;
import org.icgc_argo.workflowgraphmanager.repository.model.GraphExchangesQueue;
import org.icgc_argo.workflowgraphmanager.repository.model.GraphNode;
import org.icgc_argo.workflowgraphmanager.repository.model.GraphNodeConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
public class NodeTest {
  @Test
  public void parseTest() {
    val nodeId = "align-node";
    val pipelineId = "test-pipeline";
    val queueIds =
        List.of("start", "queued-align-node", "align-node-running", "align-node-complete");

    val graphNode =
        GraphNode.builder()
            .id(nodeId)
            .pipeline(pipelineId)
            .config(new GraphNodeConfig())
            .graphExchangesQueueList(
                queueIds.stream()
                    .map(GraphExchangesQueue::fromExchangeString)
                    .collect(Collectors.toList()))
            .build();

    val actualNode = Node.parse(graphNode);

    assertThat(actualNode)
        .extracting("id", "pipeline", "queues")
        .containsExactlyInAnyOrder(
            "align-node",
            "test-pipeline",
            queueIds.stream()
                .map(
                    queueId ->
                        Queue.builder()
                            .id(String.format("%s.%s.%s.%s", pipelineId, nodeId, queueId, queueId))
                            .exchange(queueId)
                            .queue(queueId)
                            .pipeline(pipelineId)
                            .node(nodeId)
                            .build())
                .collect(Collectors.toList()));
  }
}
