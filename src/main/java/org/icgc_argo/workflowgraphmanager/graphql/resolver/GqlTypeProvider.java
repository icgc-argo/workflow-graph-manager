package org.icgc_argo.workflowgraphmanager.graphql.resolver;

import graphql.schema.idl.TypeRuntimeWiring;

import java.util.List;

public interface GqlTypeProvider {
    public List<TypeRuntimeWiring.Builder> getTypes();
}
