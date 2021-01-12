package org.icgc_argo.workflowgraphmanager.model.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import org.icgc_argo.workflowgraphmanager.model.GraphLog;
import org.icgc_argo.workflowgraphmanager.model.Node;
import org.icgc_argo.workflowgraphmanager.model.Pipeline;
import org.icgc_argo.workflowgraphmanager.model.Queue;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public abstract class Message {
  protected String id;

  protected Pipeline pipeline;

  protected Node node;

  protected Queue queue;

  protected List<GraphLog> logs;
}
