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

package org.icgc_argo.workflowgraphmanager.graphql.resolver;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

import graphql.schema.DataFetcher;
import graphql.schema.idl.TypeRuntimeWiring;
import java.util.ArrayList;
import java.util.List;
import lombok.val;
import org.icgc_argo.workflowgraphmanager.graphql.type.NodeGqlType;
import org.springframework.stereotype.Component;

@Component
public class PipelineResolver implements GqlTypeProvider {

  public List<TypeRuntimeWiring.Builder> getTypes() {
    val types = new ArrayList<TypeRuntimeWiring.Builder>();
    types.add(newTypeWiring("Query").dataFetcher("pipeline", this.getPipelineById()));
    return types;
  }

  public DataFetcher<NodeGqlType> getPipelineById() {
    return env -> {
      val args = env.getArguments();
      String id = String.valueOf(args.get("id"));
      val output = new NodeGqlType(id);
      output.setSampleField("checkout these pipeline details");
      return output;
    };
  }
}
