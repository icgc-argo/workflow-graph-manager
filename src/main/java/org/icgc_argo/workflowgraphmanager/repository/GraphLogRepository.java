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

import static java.util.Collections.emptyList;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.search.sort.SortOrder.ASC;
import static org.icgc_argo.workflowgraphmanager.config.constants.SearchFields.*;
import static org.icgc_argo.workflowgraphmanager.utils.ElasticsearchQueryUtils.queryFromArgs;
import static org.icgc_argo.workflowgraphmanager.utils.ElasticsearchQueryUtils.sortsToEsSortBuilders;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.icgc_argo.workflowgraphmanager.config.ElasticsearchProperties;
import org.icgc_argo.workflowgraphmanager.model.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GraphLogRepository {
  private static final Map<String, Function<String, AbstractQueryBuilder<?>>> QUERY_RESOLVER =
      argumentPathMap();

  private static final Map<String, FieldSortBuilder> SORT_BUILDER_RESOLVER = sortPathMap();

  private final RestHighLevelClient client;
  private final String graphLogErrorWarningIndex;
  private final String graphLogInfoDebugIndex;

  @Autowired
  public GraphLogRepository(
      @Qualifier("ElasticSearchClient") @NonNull RestHighLevelClient client,
      @NonNull ElasticsearchProperties elasticSearchProperties) {
    this.client = client;
    this.graphLogErrorWarningIndex = elasticSearchProperties.getGraphLogErrorWarningIndex();
    this.graphLogInfoDebugIndex = elasticSearchProperties.getGraphLogInfoDebugIndex();
  }

  private static Map<String, Function<String, AbstractQueryBuilder<?>>> argumentPathMap() {
    return ImmutableMap.<String, Function<String, AbstractQueryBuilder<?>>>builder()
        .put(GRAPH_MESSAGE_ID, value -> new TermQueryBuilder("graphMessageId", value))
        .put(QUEUE, value -> new TermQueryBuilder("queue", value))
        .put(NODE, value -> new TermQueryBuilder("node", value))
        .put(PIPELINE, value -> new TermQueryBuilder("pipeline", value))
        .put(TIMESTAMP, value -> new TermQueryBuilder("timestamp", value))
        .build();
  }

  private static Map<String, FieldSortBuilder> sortPathMap() {
    return ImmutableMap.<String, FieldSortBuilder>builder()
        .put(GRAPH_MESSAGE_ID, SortBuilders.fieldSort("graphMessageId"))
        .put(QUEUE, SortBuilders.fieldSort("queue"))
        .put(NODE, SortBuilders.fieldSort("node"))
        .put(PIPELINE, SortBuilders.fieldSort("pipeline"))
        .put(TIMESTAMP, SortBuilders.fieldSort("timestamp"))
        .build();
  }

  public SearchResponse getGraphLogs(Map<String, Object> filter, Map<String, Integer> page) {
    return getGraphLogs(filter, page, emptyList());
  }

  public SearchResponse getGraphLogs(
      Map<String, Object> filter, Map<String, Integer> page, List<Sort> sorts) {
    final AbstractQueryBuilder<?> query =
        (filter == null || filter.size() == 0)
            ? matchAllQuery()
            : queryFromArgs(QUERY_RESOLVER, filter);

    val searchSourceBuilder = createSearchSourceBuilder(query, page, sorts);

    return execute(searchSourceBuilder);
  }

  public MultiSearchResponse getGraphLogs(
      List<Map<String, Object>> multipleFilters, Map<String, Integer> page) {
    List<SearchSourceBuilder> searchSourceBuilders =
        multipleFilters.stream()
            .filter(f -> f != null && f.size() != 0)
            .map(f -> createSearchSourceBuilder(queryFromArgs(QUERY_RESOLVER, f), page))
            .collect(Collectors.toList());

    if (searchSourceBuilders.isEmpty()) {
      searchSourceBuilders.add(createSearchSourceBuilder(matchAllQuery(), page));
    }

    return execute(searchSourceBuilders);
  }

  private SearchSourceBuilder createSearchSourceBuilder(
      AbstractQueryBuilder<?> query, Map<String, Integer> page) {
    return createSearchSourceBuilder(query, page, emptyList());
  }

  private SearchSourceBuilder createSearchSourceBuilder(
      AbstractQueryBuilder<?> query, Map<String, Integer> page, List<Sort> sorts) {
    val searchSourceBuilder = new SearchSourceBuilder();

    if (sorts.isEmpty()) {
      searchSourceBuilder.sort(SORT_BUILDER_RESOLVER.get(TIMESTAMP).order(ASC));
    } else {
      val sortBuilders = sortsToEsSortBuilders(SORT_BUILDER_RESOLVER, sorts);
      sortBuilders.forEach(searchSourceBuilder::sort);
    }

    searchSourceBuilder.query(query);

    if (page != null && page.size() != 0) {
      searchSourceBuilder.size(page.get("size"));
      searchSourceBuilder.from(page.get("from"));
    }

    return searchSourceBuilder;
  }

  @SneakyThrows
  private SearchResponse execute(@NonNull SearchSourceBuilder builder) {
    val searchRequest = new SearchRequest(graphLogErrorWarningIndex, graphLogInfoDebugIndex);
    searchRequest.source(builder);
    return client.search(searchRequest, RequestOptions.DEFAULT);
  }

  @SneakyThrows
  private MultiSearchResponse execute(@NonNull List<SearchSourceBuilder> builders) {
    MultiSearchRequest mSearchRequest = new MultiSearchRequest();
    builders.forEach(b -> mSearchRequest.add(new SearchRequest().source(b)));
    return client.msearch(mSearchRequest, RequestOptions.DEFAULT);
  }
}
