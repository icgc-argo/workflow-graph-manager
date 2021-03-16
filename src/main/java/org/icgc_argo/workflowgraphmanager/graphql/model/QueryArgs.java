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

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.icgc_argo.workflow_graph_lib.utils.JacksonUtils.convertValue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.val;

@Data
public class QueryArgs {
  private final Map<String, Object> filter;
  private final Map<String, Integer> page;
  private final List<Sort> sorts;

  public QueryArgs(DataFetchingEnvironment environment) {
    val args = environment.getArguments();

    val filterBuilder = ImmutableMap.<String, Object>builder();
    val pageBuilder = ImmutableMap.<String, Integer>builder();
    val sortsBuilder = ImmutableList.<Sort>builder();

    if (args != null) {
      if (args.get("filter") != null)
        filterBuilder.putAll((Map<String, Object>) args.get("filter"));
      if (args.get("page") != null) pageBuilder.putAll((Map<String, Integer>) args.get("page"));
      if (args.get("sorts") != null) {
        val rawSorts = (List<Object>) args.get("sorts");
        sortsBuilder.addAll(
            rawSorts.stream()
                .map(sort -> convertValue(sort, Sort.class))
                .collect(toUnmodifiableList()));
      }
    }

    filter = filterBuilder.build();
    page = pageBuilder.build();
    sorts = sortsBuilder.build();
  }
}
