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

import lombok.val;
import org.icgc_argo.workflowgraphmanager.repository.model.GraphExchangesQueue;
import org.icgc_argo.workflowgraphmanager.repository.model.GraphNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
public class QueueTest {

  @Test
  public void parseTest() {
    val expected =
        Queue.builder()
            .id(String.format("%s.%s.%s.%s", "test-pipeline", "test-node", "start", "start"))
            .exchange("start")
            .queue("start")
            .pipeline("test-pipeline")
            .node("test-node")
            .build();

    val graphExchangeQueue = GraphExchangesQueue.fromExchangeString("start");
    val graphNode =
        GraphNode.builder()
            .id("test-node")
            .pipeline("test-pipeline")
            .config(new HashMap<String, String>() {})
            .graphExchangesQueueList(List.of(graphExchangeQueue))
            .build();

    val actual = Queue.parse(graphExchangeQueue, graphNode);

    assertThat(actual).isEqualTo(expected);
  }
}
