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
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.icgc_argo.workflowgraphmanager.graphql.model.base.Message;
import org.icgc_argo.workflowgraphmanager.graphql.model.base.NodeProvider;
import org.icgc_argo.workflowgraphmanager.graphql.model.base.PipelineProvider;
import org.icgc_argo.workflowgraphmanager.repository.model.GraphExchangesQueue;
import org.icgc_argo.workflowgraphmanager.repository.model.GraphNode;
import org.icgc_argo.workflowgraphmanager.utils.JacksonUtils;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Queue implements PipelineProvider, NodeProvider {
  @NonNull private String id;

  @NonNull private String exchange;

  @NonNull private String queue;

  @NonNull private String pipeline;

  @NonNull private String node;

  private List<Message> messages;

  @SneakyThrows
  public static Queue parse(@NonNull Map<String, Object> sourceMap) {
    return JacksonUtils.parse(sourceMap, Queue.class);
  }

  public static Queue parse(
      @NonNull GraphExchangesQueue graphExchangesQueue, @NonNull GraphNode<?> graphNode) {
    return Queue.builder()
        .id(
            String.format(
                "%s.%s.%s.%s",
                graphNode.getPipeline(),
                graphNode.getId(),
                graphExchangesQueue.getExchange(),
                graphExchangesQueue.getQueue()))
        .exchange(graphExchangesQueue.getExchange())
        .queue(graphExchangesQueue.getQueue())
        .pipeline(graphNode.getPipeline())
        .node(graphNode.getId())
        .build();
  }
}
