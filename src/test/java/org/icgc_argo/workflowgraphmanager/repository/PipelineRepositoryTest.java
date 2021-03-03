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

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import lombok.val;
import org.icgc_argo.workflow_graph_lib.utils.JacksonUtils;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@EnableKubernetesMockClient(crud = true)
public class PipelineRepositoryTest {

  static KubernetesClient client;

  private final PipelineRepository pipelineRepository;

  public PipelineRepositoryTest() {
    this.pipelineRepository = new PipelineRepository(client);
  }

  @Test
  public void singleNodePipeline() {
    val simplePipelineJson =
        readValue(this.getClass().getResourceAsStream("fixtures/single-pipeline.json"), Map.class);

    loadK8sWithDecoysAnd(
        ((List<Map<String, Object>>) simplePipelineJson.get("items"))
            .stream().map(podJson -> JacksonUtils.convertValue(podJson, Pod.class)));

    val result = pipelineRepository.getPipelines();

    assertThat(result.keySet().size()).isEqualTo(1);
    assertThat(result.keySet().contains("test-pipeline")).isTrue();
    assertThat(result.get("test-pipeline").getNodes().size()).isEqualTo(2);
  }

  private void loadK8sWithDecoysAnd(Stream<Pod> pods) {
    val decoyPodsJson =
        readValue(this.getClass().getResourceAsStream("fixtures/decoy-pods.json"), Map.class);

    // Merge pods with decoy stream and load mock K8s
    Stream.concat(pods, ((List<Map<String, Object>>) decoyPodsJson.get("items")).stream())
        .map(podJson -> JacksonUtils.convertValue(podJson, Pod.class))
        .forEach(pod -> client.pods().create(pod));
  }
}
