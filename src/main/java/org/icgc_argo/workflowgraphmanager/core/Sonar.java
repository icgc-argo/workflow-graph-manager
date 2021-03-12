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

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc_argo.workflowgraphmanager.graphql.model.Node;
import org.icgc_argo.workflowgraphmanager.graphql.model.Pipeline;
import org.icgc_argo.workflowgraphmanager.graphql.model.Queue;
import org.icgc_argo.workflowgraphmanager.graphql.model.base.GraphEntity;
import org.icgc_argo.workflowgraphmanager.repository.GraphNodeRepository;
import org.icgc_argo.workflowgraphmanager.repository.model.GraphNode;
import org.icgc_argo.workflowgraphmanager.repository.model.GraphPipeline;
import org.icgc_argo.workflowgraphmanager.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Sonar Service is responsible for building and maintaining an in-memory store that represents
 * the current state of all pipelines deployed withing a single kubernetes namespace. After the
 * initial construction it will ping the various repositories for partial state updates in order to
 * maintain a near-realtime view of the current state of all aforementioned pipelines. Ref:
 * https://wiki.oicr.on.ca/pages/viewpage.action?pageId=154539008
 */
@Slf4j
@Configuration
public class Sonar {
  private final GraphNodeRepository graphNodeRepository;

  // State
  private ConcurrentHashMap<String, Pipeline> pipelines;
  private ConcurrentHashMap<String, Node> nodes;

  public Sonar(@Autowired GraphNodeRepository graphNodeRepository) {
    this.graphNodeRepository = graphNodeRepository;
    initStore();
  }

  private void initStore() {
    log.info("Sonar initializing entity state stores ...");
    pipelines = new ConcurrentHashMap<>();
    nodes = new ConcurrentHashMap<>();
    graphNodeRepository
        .getNodes()
        .forEach(graphNode -> nodes.put(graphNode.getId(), Node.parse(graphNode)));
    log.debug("Sonar nodes store initialized: {}", nodes);
    pipelines.putAll(assemblePipelinesFromNodes());
    log.debug("Sonar pipelines store initialized: {}", pipelines);
    log.info("Sonar state stores initialization complete!");
  }

  @Bean
  public Disposable doShallowUpdate() {
    return Flux.generate(
            (SynchronousSink<Stream<GraphNode<?>>> sink) -> {
              sink.next(graphNodeRepository.getNodes());
            })
        .delayElements(Duration.ofSeconds(10)) // todo: make configurable
        .doOnNext(this::shallowUpdate)
        .subscribe();
  }

  public Pipeline getPipelineById(String pipelineId) {
    return pipelines.get(pipelineId);
  }

  public List<Pipeline> getPipelines() {
    return new ArrayList<>(pipelines.values());
  }

  public Node getNodeById(String nodeId) {
    return nodes.get(nodeId);
  }

  public List<Node> getNodes() {
    return new ArrayList<>(nodes.values());
  }

  HashMap<String, Pipeline> assemblePipelinesFromNodes() {
    return nodes.values().stream()
        .reduce(
            new HashMap<>(),
            (HashMap<String, Pipeline> pipelines, Node node) -> {
              val pipeline = getOrCreatePipeline(node, pipelines);

              // Add the new node to the list of nodes for the pipeline
              pipeline.setNodes(
                  Stream.concat(Stream.of(node), pipeline.getNodes().stream())
                      .collect(Collectors.toList()));

              if (pipelines.containsKey(pipeline.getId())) {
                pipelines.replace(pipeline.getId(), pipeline);
              } else {
                pipelines.put(pipeline.getId(), pipeline);
              }

              return pipelines;
            },
            CommonUtils::handleReduceHashMapConflict);
  }

  /**
   * Populates the top levels of the state tree comprised of pipelines, nodes, and a list of queues
   * (without details), information which is gleamed via the Kubernetes API. A shallow update should
   * not erase existing deep data unless there is an actual difference between the new state and the
   * previous measured at the queue level. Pipeline/Node changes should be incorporated if possible
   * without wiping the deeper data (ex. config change in a node) TODO: rewrite this
   *
   * @param graphNodes - list of Nodes
   */
  private void shallowUpdate(Stream<GraphNode<?>> graphNodes) {
    log.info("Sonar starting shallowUpdate ...");
    log.debug("Sonar shallowUpdate received nodes update: {}", graphNodes);
    graphNodes.forEach(
        graphNode ->
            nodes.merge(
                graphNode.getId(),
                Node.parse(graphNode),
                (existing, update) ->
                    Node.builder()
                        .id(update.getId())
                        .pipeline(update.getPipeline())
                        .queues(shallowMergeQueues(existing.getQueues(), update.getQueues()))
                        .messages(filterForActiveQueues(existing.getMessages(), update))
                        .logs(filterForActiveQueues(existing.getLogs(), update))
                        .build()));
    log.debug("Sonar shallowUpdate of nodes complete, new nodes store: {}", nodes);
    rebuildPipelines();
    log.info("Sonar shallowUpdate complete!");
  }

  /**
   * TBD (not sure about this yet) Populates the deeper levels of the state tree, queues and below,
   * information which is gleamed via the RabbitMQ management API. A deep update will use as input
   * the complete state. New data WILL ALWAYS OVERWRITE existing data in a merge scenario.
   *
   * @param state - list of pipelines without details deeper than the name of the queues associated
   *     with a node
   */
  private void deepUpdate(HashMap<String, GraphPipeline> state) {}

  private List<Node> shallowMergeNodes(List<Node> existing, List<Node> update) {
    return update;
  }

  private List<Queue> shallowMergeQueues(List<Queue> existing, List<Queue> update) {
    return update;
  }

  /**
   * Filters a list of GraphEntity, returning only the entities that have a queueId which is present
   * in the node passed in as the second argument
   *
   * @param graphEntities - list of entities to filter
   * @param node - node containing queues which are used for the filter
   * @return a filtered list of entities that have an associated queue in the referenced node
   */
  private <T extends GraphEntity> List<T> filterForActiveQueues(List<T> graphEntities, Node node) {
    return graphEntities.stream()
        .filter(
            graphEntity ->
                node.getQueues().stream()
                    .map(Queue::getId)
                    .anyMatch(queueId -> queueId.equalsIgnoreCase(graphEntity.getQueue().getId())))
        .collect(Collectors.toList());
  }

  private Pipeline getOrCreatePipeline(Node node, HashMap<String, Pipeline> pipelines) {
    return pipelines.getOrDefault(node.getPipeline(), Pipeline.parse(node.getPipeline(), node));
  }

  private void rebuildPipelines() {
    pipelines.clear();
    pipelines.putAll(assemblePipelinesFromNodes());
  }
}
