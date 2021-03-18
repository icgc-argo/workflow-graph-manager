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
import org.icgc_argo.workflowgraphmanager.graphql.model.*;
import org.icgc_argo.workflowgraphmanager.service.GraphLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class GraphLogDataFetcher {

  private final GraphLogService graphLogService;

  @Autowired
  public GraphLogDataFetcher(GraphLogService graphLogService) {
    this.graphLogService = graphLogService;
  }

  @SuppressWarnings("unchecked")
  public DataFetcher<SearchResult<GraphLog>> getGraphLogsDataFetcher() {
    return environment -> graphLogService.searchGraphLogs(new QueryArgs(environment));
  }

  @SuppressWarnings("unchecked")
  public DataFetcher<AggregationResult> getAggregateGraphLogsDataFetcher() {
    return environment ->
        graphLogService.aggregateGraphLogs(new QueryArgs(environment).getFilter());
  }

  public DataFetcher<List<GraphLog>> getLogsForPipelineDataFetcher() {
    return environment ->
        graphLogService.getGraphLogByPipelineId(((Pipeline) environment.getSource()).getId());
  }

  public DataFetcher<List<GraphLog>> getLogsForNodeDataFetcher() {
    return environment ->
        graphLogService.getGraphLogByNodeId(((Node) environment.getSource()).getId());
  }

  public DataFetcher<List<GraphLog>> getLogsForQueueDataFetcher() {
    return environment -> graphLogService.getGraphLogByQueue(environment.getSource());
  }
}
