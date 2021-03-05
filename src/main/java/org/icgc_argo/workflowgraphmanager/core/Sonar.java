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

import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.icgc_argo.workflowgraphmanager.graphql.model.Pipeline;
import org.icgc_argo.workflowgraphmanager.repository.GraphNodeRepository;
import org.icgc_argo.workflowgraphmanager.repository.model.GraphPipeline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

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
  private ConcurrentHashMap<String, Pipeline> store;

  public Sonar(@Autowired GraphNodeRepository graphNodeRepository) {
    this.graphNodeRepository = graphNodeRepository;
  }

  @Bean
  public Disposable doShallowUpdate() {
    return Flux.generate(
            (SynchronousSink<HashMap<String, GraphPipeline>> sink) -> {
              sink.next(graphNodeRepository.getPipelines());
            })
        .delayElements(Duration.ofSeconds(10)) // todo: make configurable
        .doOnNext(this::shallowUpdate)
        .subscribe();
  }

  public Pipeline getPipelineById(String pipeline) {
    return store.get(pipeline);
  }

  /**
   * Populates the top levels of the state tree comprised of pipelines, nodes, and a list of queues
   * (without details), information which is gleamed via the Kubernetes API. A shallow update should
   * not erase existing deep data unless there is an actual difference between the new state and the
   * previous measured at the queue level. Pipeline/Node changes should be incorporated if possible
   * without wiping the deeper data (ex. config change in a node)
   *
   * @param state - list of pipelines without details deeper than the name of the queues associated
   *     with a node
   */
  private void shallowUpdate(HashMap<String, GraphPipeline> state) {
    log.info("Ping with new state: {}", state);
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
}
