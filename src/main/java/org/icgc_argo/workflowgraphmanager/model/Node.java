package org.icgc_argo.workflowgraphmanager.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.icgc_argo.workflowgraphmanager.utils.JacksonUtils;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Node {
  private String id;

  private Map<String, Object> config;

  private Boolean enabled;

  private Integer capacity;

//  private String workflow; // todo: Make Workflow an entity

  private Pipeline pipeline;

  private List<Queue> queues;

  private List<String> messages;

  private List<GraphLog> logs;

  @SneakyThrows
  public static Node parse(@NonNull Map<String, Object> sourceMap) {
    return JacksonUtils.parse(sourceMap, Node.class);
  }
}
