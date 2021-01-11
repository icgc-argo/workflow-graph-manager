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
