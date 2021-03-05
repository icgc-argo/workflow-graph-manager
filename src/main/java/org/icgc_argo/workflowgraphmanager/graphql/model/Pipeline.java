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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import org.icgc_argo.workflowgraphmanager.graphql.model.base.Message;
import org.icgc_argo.workflowgraphmanager.repository.model.GraphPipeline;
import org.icgc_argo.workflowgraphmanager.utils.JacksonUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Pipeline {

  @NonNull private String id;

  @NonNull private List<Node> nodes;

  private List<Queue> queues;

  private List<Message> messages;

  private List<GraphLog> logs;

  @SneakyThrows
  public static Pipeline parse(@NonNull Map<String, Object> sourceMap) {
    return JacksonUtils.parse(sourceMap, Pipeline.class);
  }

  public static Pipeline parse(@NonNull GraphPipeline graphPipeline) {
    val nodes =
        graphPipeline.getGraphNodes().stream()
            .map(
                graphNode ->
                    Node.builder()
                        .id(graphNode.getId())
                        .config(JacksonUtils.parse(graphNode.getConfig(), new TypeReference<>() {}))
                        .queues(
                            graphNode.getGraphExchangesQueueList().stream()
                                .map(Queue::parse)
                                .collect(Collectors.toList()))
                        .build())
            .collect(Collectors.toList());

    return Pipeline.builder().id(graphPipeline.getId()).nodes(nodes).build();
  }
}
