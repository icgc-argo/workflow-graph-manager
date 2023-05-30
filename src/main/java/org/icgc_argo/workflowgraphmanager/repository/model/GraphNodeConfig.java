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

package org.icgc_argo.workflowgraphmanager.repository.model;

import java.util.List;
import lombok.*;
import org.icgc_argo.workflow_graph_lib.polyglot.enums.GraphFunctionLanguage;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
// TODO: this is the same as the NodeConfig class in graph-node, could go in lib?
public class GraphNodeConfig {
  private String pipelineId;
  private String nodeId;

  private GraphFunctionLanguage functionLanguage;
  private List<Filter> filters;
  private String gqlQueryString;
  private String activationFunction;

  // Declares new subscribers on fanout exchange(s)
  private List<GraphExchangesQueue> input;

  // Direct exchange internal to this Node
  private GraphExchangesQueue running;

  // Complete exchange with self-archiving complete queue
  private GraphExchangesQueue complete;

  private WorkflowProperties workflow;

  private WorkflowEngineParameters workflowEngineParams;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Filter {
    private String expression;
    private Boolean reject;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class WorkflowProperties {
    private String url;
    private String revision;
    private String schemaName;
    private String schemaNamespace;
    private String schemaVersion;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class WorkflowEngineParameters {
    private String workDir;
    private String launchDir;
    private String projectDir;
  }
}
