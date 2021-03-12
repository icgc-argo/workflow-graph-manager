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
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.icgc_argo.workflowgraphmanager.graphql.model.base.Message;
import org.icgc_argo.workflowgraphmanager.utils.JacksonUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Slf4j
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Pipeline {

  @NonNull private String id;

  @NonNull private List<Node> nodes;

  @NonNull private List<Queue> queues;

  @NonNull private List<Message> messages;

  @NonNull private List<GraphLog> logs;

  @SneakyThrows
  public static Pipeline parse(@NonNull Map<String, Object> sourceMap) {
    return JacksonUtils.parse(sourceMap, Pipeline.class);
  }

  public static Pipeline parse(@NonNull String pipelineId, @NonNull List<Node> nodes) {
    return Pipeline.builder()
        .id(pipelineId)
        .nodes(nodes)
        .queues(
            nodes.stream().flatMap(node -> node.getQueues().stream()).collect(Collectors.toList()))
        .messages(Collections.emptyList())
        .logs(Collections.emptyList())
        .build();
  }

  public static Pipeline parse(@NonNull String pipelineId, @NonNull Node node) {
    return Pipeline.builder()
        .id(pipelineId)
        .nodes(List.of(node))
        .queues(node.getQueues())
        .messages(Collections.emptyList())
        .logs(Collections.emptyList())
        .build();
  }
}
