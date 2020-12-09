package org.icgc_argo.workflowgraphmanager.graphql.type;

import lombok.Data;
import lombok.NonNull;

@Data
public class NodeGqlType {

  @NonNull String id;
  String sampleField;
}
