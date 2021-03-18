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

package org.icgc_argo.workflowgraphmanager.service;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.icgc_argo.workflowgraphmanager.config.constants.EsDefaults.ES_PAGE_DEFAULT_FROM;
import static org.icgc_argo.workflowgraphmanager.config.constants.EsDefaults.ES_PAGE_DEFAULT_SIZE;
import static org.icgc_argo.workflowgraphmanager.config.constants.SearchFields.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.val;
import org.elasticsearch.search.SearchHit;
import org.icgc_argo.workflowgraphmanager.graphql.model.*;
import org.icgc_argo.workflowgraphmanager.repository.GraphLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GraphLogService {
  private final GraphLogRepository graphLogRepository;

  @Autowired
  public GraphLogService(GraphLogRepository graphLogRepository) {
    this.graphLogRepository = graphLogRepository;
  }

  private static GraphLog hitToGraphLog(SearchHit hit) {
    val sourceMap = hit.getSourceAsMap();
    return GraphLog.parse(sourceMap);
  }

  public SearchResult<GraphLog> searchGraphLogs(QueryArgs queryArgs) {
    val filter = queryArgs.getFilter();
    val page = queryArgs.getPage();
    val sorts = queryArgs.getSorts();

    val response = graphLogRepository.getGraphLogs(filter, page, sorts);
    val responseSearchHits = response.getHits();

    val totalHits = responseSearchHits.getTotalHits().value;
    val from = page.getOrDefault("from", ES_PAGE_DEFAULT_FROM);
    val size = page.getOrDefault("size", ES_PAGE_DEFAULT_SIZE);

    val graphLogs =
        Arrays.stream(responseSearchHits.getHits())
            .map(GraphLogService::hitToGraphLog)
            .collect(toUnmodifiableList());
    val nextFrom = (totalHits - from) / size > 0;

    return new SearchResult<>(graphLogs, nextFrom, totalHits);
  }

  public AggregationResult aggregateGraphLogs(Map<String, Object> filter) {
    val response = graphLogRepository.getGraphLogs(filter, Map.of(), List.of());
    val responseSearchHits = response.getHits();
    val totalHits = responseSearchHits.getTotalHits().value;
    return new AggregationResult(totalHits);
  }

  public List<GraphLog> getGraphLogByPipelineId(String pipelineId) {
    return getGraphLogs(Map.of(PIPELINE, pipelineId), null);
  }

  public List<GraphLog> getGraphLogByNodeId(String nodeId) {
    return getGraphLogs(Map.of(NODE, nodeId), null);
  }

  public List<GraphLog> getGraphLogByQueue(Queue queue) {
    return getGraphLogs(
        Map.of(PIPELINE, queue.getPipeline(), NODE, queue.getNode(), QUEUE, queue.getQueue()));
  }

  public List<GraphLog> getGraphLogsByGraphMessageId(String graphMessageId) {
    return getGraphLogs(Map.of(GRAPH_MESSAGE_ID, graphMessageId));
  }

  public List<GraphLog> getGraphLogs(Map<String, Object> filter) {
    val response = graphLogRepository.getGraphLogs(filter, null);
    val hitStream = Arrays.stream(response.getHits().getHits());
    return hitStream.map(GraphLogService::hitToGraphLog).collect(toUnmodifiableList());
  }

  public List<GraphLog> getGraphLogs(Map<String, Object> filter, Map<String, Integer> page) {
    val response = graphLogRepository.getGraphLogs(filter, page);
    val hitStream = Arrays.stream(response.getHits().getHits());
    return hitStream.map(GraphLogService::hitToGraphLog).collect(toUnmodifiableList());
  }
}
