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

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
public class SonarTest {
  @Test
  public void testSinglePipelineAssembly() {
    // Test pipeline is correct
    //        assertThat(pipelines.keySet().size()).isEqualTo(1);
    //        assertThat(pipelines.containsKey("test-pipeline")).isTrue();
    //        assertThat(pipelines.get("test-pipeline").getGraphNodes()).hasSameElementsAs(nodes);
  }

  @Test
  public void testMultiPipelineAssembly() {
    //        // Test pipeline is correct
    //        assertThat(pipelines.keySet().size()).isEqualTo(3);
    //        assertThat(
    //                pipelines
    //                        .keySet()
    //                        .containsAll(List.of("test-pipeline", "test-pipeline-two",
    // "test-pipeline-three")))
    //                .isTrue();
    //        assertThat(pipelines.get("test-pipeline").getGraphNodes())
    //                .hasSameElementsAs(
    //                        nodes.stream()
    //                                .filter(node ->
    // node.getPipeline().equalsIgnoreCase("test-pipeline"))
    //                                .collect(Collectors.toList()));
    //        assertThat(pipelines.get("test-pipeline-two").getGraphNodes())
    //                .hasSameElementsAs(
    //                        nodes.stream()
    //                                .filter(node ->
    // node.getPipeline().equalsIgnoreCase("test-pipeline-two"))
    //                                .collect(Collectors.toList()));
    //        assertThat(pipelines.get("test-pipeline-three").getGraphNodes())
    //                .hasSameElementsAs(
    //                        nodes.stream()
    //                                .filter(node ->
    // node.getPipeline().equalsIgnoreCase("test-pipeline-three"))
    //                                .collect(Collectors.toList()));
  }
}
