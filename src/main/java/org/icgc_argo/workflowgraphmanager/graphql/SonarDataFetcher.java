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

package org.icgc_argo.workflowgraphmanager.graphql;

import graphql.schema.DataFetcher;
import lombok.extern.slf4j.Slf4j;
import org.icgc_argo.workflowgraphmanager.core.Sonar;
import org.icgc_argo.workflowgraphmanager.graphql.model.*;
import org.icgc_argo.workflowgraphmanager.graphql.model.base.NodeProvider;
import org.icgc_argo.workflowgraphmanager.graphql.model.base.PipelineProvider;
import org.icgc_argo.workflowgraphmanager.graphql.model.base.QueueProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SonarDataFetcher {

  private final Sonar sonar;

  @Autowired
  public SonarDataFetcher(Sonar sonar) {
    this.sonar = sonar;
  }

  public DataFetcher<SearchResult<Pipeline>> getPipelineDataFetcher() {
    return environment -> sonar.searchPipelines(new QueryArgs(environment));
  }

  public DataFetcher<SearchResult<Node>> getNodeDataFetcher() {
    return environment -> sonar.searchNodes(new QueryArgs(environment));
  }

  public DataFetcher<SearchResult<Queue>> getQueueDataFetcher() {
    return environment -> sonar.searchQueues(new QueryArgs(environment));
  }

  public DataFetcher<Pipeline> getNestedPipelineDataFetcher() {
    return environment ->
        sonar.getPipelineById(((PipelineProvider) environment.getSource()).getPipeline());
  }

  public DataFetcher<Node> getNestedNodeDataFetcher() {
    return environment -> sonar.getNodeById(((NodeProvider) environment.getSource()).getNode());
  }

  public DataFetcher<Queue> getNestedQueueDataFetcher() {
    return environment -> sonar.getQueueById(((QueueProvider) environment.getSource()).getQueue());
  }
}
